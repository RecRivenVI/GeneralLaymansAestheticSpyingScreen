"""Runtime profile gates and silent test-instance preparation."""

import json
import pathlib
import sys
import tomllib

ROOT = pathlib.Path(__file__).resolve().parents[2]
SOFT = 3 * 1024**3
HARD = 5 * 1024**3


def configuration():
    scenes = {}
    for path in sorted((ROOT / "validations").glob("*/validation.toml")):
        data = tomllib.loads(path.read_text(encoding="utf-8"))
        scene = data.get("scene")
        if scene:
            assert scene not in scenes, scene
            scenes[scene] = (path.parent, data)
            for role, settings in data.get("instances", {}).items():
                if role != "server":
                    assert (settings["width"], settings["height"]) == (1920, 1080)
                    assert settings["player-name"] == (
                        "RecRivenVI" if role == "client" else "CNN_MOYAN"
                    )
                if scene != "S2":
                    assert not settings.get("mods"), (scene, role)
    assert set(scenes) == {"S1", "S2", "M1", "M2"}
    return scenes


def quota():
    size = sum(
        p.stat().st_size for p in (ROOT / "validations").glob("*/**/*") if p.is_file()
    )
    local = ROOT / "local.toml"
    if local.exists():
        inputs = tomllib.loads(local.read_text(encoding="utf-8")).get("inputs", {})
        for directory in {
            pathlib.Path(value).parent
            for name, value in inputs.items()
            if name.startswith("mcp_")
        }:
            size += sum(p.stat().st_size for p in directory.iterdir() if p.is_file())
        if "customskinloader_acceptance" in inputs:
            path = pathlib.Path(inputs["customskinloader_acceptance"])
            if path.is_file():
                size += path.stat().st_size
    if size > HARD:
        raise RuntimeError(
            f"Validation data {size} exceeds hard limit {HARD}; recycle only after reviewing ownership"
        )
    print(
        json.dumps(
            {
                "validationBytes": size,
                "softLimit": SOFT,
                "hardLimit": HARD,
                "softExceeded": size > SOFT,
            }
        )
    )
    return size


def prepare(scene):
    parent, data = configuration()[scene]
    quota()
    for target in data["targets"]:
        for role in data.get("instances", {}):
            if role == "server":
                continue
            instance = parent / "instance" / target / role
            instance.mkdir(parents=True, exist_ok=True)
            options = instance / "options.txt"
            lines = (
                options.read_text(encoding="utf-8").splitlines()
                if options.exists()
                else []
            )
            lines = [
                line
                for line in lines
                if not line.startswith(
                    ("soundCategory_master:", "onboardAccessibility:")
                )
            ]
            lines.extend(["soundCategory_master:0.0", "onboardAccessibility:false"])
            options.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{scene}: silent client instances prepared")


def prepare_reference():
    import datetime
    import shutil

    quota()
    result = ROOT / "validations/smoke-reference_projection/result"
    previous = list(result.glob("*.png")) + list(result.glob("*.json"))
    if previous:
        archive = result / (
            "attempt_" + datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        )
        archive.mkdir()
        for path in previous:
            assert path.resolve().is_relative_to(result.resolve())
            shutil.move(str(path), str(archive / path.name))
    instance = (
        ROOT / "validations/smoke-reference_projection/instance/26.3-neoforge/client"
    )
    instance.mkdir(parents=True, exist_ok=True)
    options = instance / "options.txt"
    lines = options.read_text(encoding="utf-8").splitlines() if options.exists() else []
    lines = [
        line
        for line in lines
        if not line.startswith(
            ("soundCategory_master:", "onboardAccessibility:", "pauseOnLostFocus:")
        )
    ]
    lines.extend(
        [
            "soundCategory_master:0.0",
            "onboardAccessibility:false",
            "pauseOnLostFocus:false",
        ]
    )
    options.write_text("\n".join(lines) + "\n", encoding="utf-8")


def review_reference():
    import hashlib
    import shutil
    import struct

    result = ROOT / "validations/smoke-reference_projection/result"
    data = json.loads((result / "runtime.json").read_text(encoding="utf-8"))
    assert data["status"] == "PENDING_IMAGE_REVIEW", data
    assert data["packetRoundTrip"] and data["active"] and data["surfaces"] > 0, data
    assert (
        data["wirelessNodes"] == 3
        and data["wiredContexts"] > 0
        and data["wirelessContexts"] > 0
    ), data
    assert (
        data["width"] == 1920 and data["height"] == 1080 and data["masterVolume"] == 0
    ), data
    assert "vulkan" in data["backend"].lower(), data
    assert "imageError" not in data, data
    images = {}
    for name in ("wired_channel", "wired_main", "wireless_channel", "wireless_main"):
        image = (result / (name + ".png")).read_bytes()
        assert image[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(
            ">II", image[16:24]
        ) == (1920, 1080), name
        images[name] = {
            "sha256": hashlib.sha256(image).hexdigest(),
            "bytes": len(image),
        }
    for name in ("wiredChannel", "wirelessChannel", "wiredMain", "wirelessMain"):
        assert data[name + "RedPixels"] > 30 and data[name + "GreenPixels"] > 30, (
            name,
            data,
        )
    log = (
        ROOT
        / "validations/smoke-reference_projection/instance/26.3-neoforge/client/logs/latest.log"
    )
    text = log.read_text(encoding="utf-8")
    for failure in (
        "Failed to render GLASS",
        "Couldn't compile pipeline (glass:",
        "Mixin apply for mod glass failed",
    ):
        assert failure not in text, failure
    assert (
        "Starting integrated minecraft server" in text
        and "GLASS render backend: Vulkan" in text
    )
    shutil.copyfile(log, result / "client_latest.log")
    world = (
        ROOT
        / "validations/smoke-reference_projection/instance/26.3-neoforge/client/saves"
        / data["world"]
    )
    shutil.make_archive(str(result / "reference_world"), "zip", world)
    (result / "review.json").write_text(
        json.dumps(
            {
                "status": "PASS_BASIC_CAPTURE_AND_COMPOSITE",
                "world": data["world"],
                "images": images,
                "visualAcceptance": "PENDING_USER",
                "scope": "Reference launch, world entry, channel payload round trip, live framebuffers, wired and wireless color capture",
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )
    quota()
    print(
        "Runtime structure and image dimensions PASS; visual inspection remains required"
    )


def verify_migration():
    import hashlib
    import subprocess
    import xml.etree.ElementTree as xml

    result = ROOT / "validations/migration-template_layout/result"
    result.mkdir(parents=True, exist_ok=True)
    targets = sorted(
        path.parent.name for path in (ROOT / "versions").glob("*/target.properties")
    )
    commands = [
        ["spotlessApply"],
        ["check", "verifyRelease"] + [f":version:{target}:build" for target in targets],
    ]
    report = {"status": "RUNNING", "commands": [], "targets": {}, "artifacts": []}
    try:
        for index, tasks in enumerate(commands):
            command = [
                "cmd.exe",
                "/d",
                "/c",
                str(ROOT / "gradlew.bat"),
                *tasks,
                "--no-configuration-cache",
                "--console=plain",
            ]
            log = result / ("format.log" if index == 0 else "gates.log")
            print("Running " + " ".join(tasks), flush=True)
            with log.open("w", encoding="utf-8", newline="\n") as output:
                completed = subprocess.run(
                    command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT
                )
            report["commands"].append(
                {"tasks": tasks, "exitCode": completed.returncode, "log": log.name}
            )
            if completed.returncode != 0:
                raise RuntimeError(f"Migration gates failed; read {log}")
        for target in targets:
            suites = [
                xml.parse(path).getroot()
                for path in (
                    ROOT / "versions" / target / "build/test-results/test"
                ).glob("TEST-*.xml")
            ]
            counts = {
                key: sum(int(suite.attrib[key]) for suite in suites)
                for key in ("tests", "failures", "errors", "skipped")
            }
            assert (
                counts["tests"] > 0
                and counts["failures"] == 0
                and counts["errors"] == 0
                and counts["skipped"] == 0
            ), (target, counts)
            report["targets"][target] = counts
        facts = dict(
            line.split("=", 1)
            for line in (ROOT / "gradle.properties")
            .read_text(encoding="utf-8")
            .splitlines()
            if "=" in line and not line.startswith("#")
        )
        release = ROOT / "build/release" / facts["mod_version"]
        for path in sorted(release.glob("*.jar")):
            report["artifacts"].append(
                {
                    "name": path.name,
                    "bytes": path.stat().st_size,
                    "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                }
            )
        assert len(report["artifacts"]) == len(targets), report["artifacts"]
        report["status"] = "PASS"
    except Exception as error:
        report["status"] = "FAIL"
        report["error"] = str(error)
        raise
    finally:
        (result / "build_verification.json").write_text(
            json.dumps(report, indent=2) + "\n", encoding="utf-8"
        )
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    if sys.argv[1:] and sys.argv[1] == "verify-migration":
        verify_migration()
    elif sys.argv[1:] and sys.argv[1] == "prepare-reference":
        prepare_reference()
    elif sys.argv[1:] and sys.argv[1] == "review-reference":
        review_reference()
    elif sys.argv[1:] and sys.argv[1] == "prepare":
        prepare(sys.argv[2])
    else:
        configuration()
        quota()
        print("Four independent runtime profiles validated")
