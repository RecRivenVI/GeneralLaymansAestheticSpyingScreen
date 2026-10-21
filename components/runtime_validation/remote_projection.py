"""Owned MCP driver for the native 1.21.1 remote-projection validation run."""

import json
import io
import os
import pathlib
import shutil
import uuid
import subprocess
import sys
import time
import tomllib

from PIL import Image

import mcp_alpha as m
import render_review as review

BASE = m.ROOT / "validations/integration-remote_projection"
CONFIG = tomllib.loads((BASE / "validation.toml").read_text(encoding="utf-8"))
TARGET = CONFIG["targets"][0]


def bind():
    m.TARGET = TARGET
    m.MC, m.LOADER = TARGET.rsplit("-", 1)
    m.CONFIG = CONFIG
    review.CONFIG = CONFIG
    m.snapshot = lambda: m.input_path(CONFIG["mcp"]["input"])


def launch(case):
    bind()
    m.quota()
    client = BASE / "instance" / TARGET / case["role"]
    output = BASE / "result" / case["name"]
    client.mkdir(parents=True, exist_ok=True)
    output.mkdir(parents=True, exist_ok=True)
    for pattern in [
        "*-request.txt",
        "*-arranged.txt",
        "animation-phase-*.json",
        "animation-resume-*.json",
    ]:
        for marker in output.glob(pattern):
            marker.write_text("", encoding="utf-8")
    m.write_json(output / "remote-views.json", [])
    world = (client / "saves" / case["world"]).resolve()
    assert world.is_relative_to((BASE / "instance").resolve())
    if world.exists():
        subprocess.run(
            [
                "pwsh",
                "-NoProfile",
                "-File",
                str(m.ROOT / "components/runtime_validation/recycle_validation.ps1"),
                "-ValidationWorld",
                str(world),
            ],
            check=True,
            creationflags=subprocess.CREATE_NO_WINDOW,
        )
    m.write_json(client / "glass-scene.json", case)
    m.write_json(client / "config/glass.json", {"legacyFadeAnimation": case["classic"]})
    (client / "options.txt").write_text(
        "lang:en_us\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\n"
        "tutorialStep:none\nsoundCategory_master:0.0\nsoundCategory_music:0.0\n"
        "renderDistance:8\nsimulationDistance:5\nchatVisibility:0\ngraphicsMode:"
        + str(case["graphics"])
        + "\n",
        encoding="utf-8",
        newline="\n",
    )
    environment = os.environ.copy()
    environment["MCP_RUNTIME_SCOPES"] = (
        "read,ui,input,control,capture,diagnostics,command"
    )
    environment["GLASS_VALIDATION_CASE"] = case["name"]
    role = "Client" if case["role"] == "client" else "ClientMultiplayer"
    log = (output / "native-launch.log").open("w", encoding="utf-8")
    launcher = subprocess.Popen(
        [
            str(m.ROOT / "gradlew.bat"),
            "--offline",
            "--no-configuration-cache",
            ":version:" + TARGET + ":runValidationIntegrationRemoteProjection" + role,
        ],
        cwd=m.ROOT,
        stdout=log,
        stderr=subprocess.STDOUT,
        env=environment,
        creationflags=subprocess.CREATE_NO_WINDOW,
    )
    log.close()
    endpoint = client / "minecraft-protocol/endpoint.json"
    deadline = time.monotonic() + 300
    while True:
        if endpoint.exists():
            record = json.loads(endpoint.read_text(encoding="utf-8"))
            process = m.ExistingProcess(record["pid"])
            if process.poll() is None:
                break
        assert launcher.poll() is None and time.monotonic() < deadline, (
            "Native launch failed"
        )
        time.sleep(0.5)
    runtime = m.Runtime(client, process)
    review.constrain(process)
    m.write_json(
        output / "process.json", {"pid": process.pid, "directory": str(client)}
    )
    review.ready(runtime, output)
    review.command(runtime, output, "gamemode spectator")
    review.command(runtime, output, "time set 6000")
    review.command(runtime, output, "weather clear")
    review.command(runtime, output, "tp @s .5 198.88 -5.5 0 0")
    return runtime, launcher, output


def wait_ready(runtime, output):
    deadline = time.monotonic() + 45
    terminal, ready_since = None, None
    while True:
        runtime.call("/v0/control/renew", {"ttlMs": 60000})
        path = output / "remote-views.json"
        views = json.loads(path.read_text(encoding="utf-8")) if path.exists() else []
        if len(views) == 1 and views[0]["received"] == int(views[0]["expectedChunks"]):
            key = views[0]["terminal"]
            if key != terminal or ready_since is None:
                terminal, ready_since = key, time.monotonic()
            elif time.monotonic() - ready_since >= 2:
                return
        else:
            terminal, ready_since = None, None
        assert runtime.process.poll() is None and time.monotonic() < deadline, views
        time.sleep(0.5)


def wait_scene(runtime, *, blue=False):
    """Wait for the fixed source wall to be drawn, including asynchronous mesh compilation."""
    deadline = time.monotonic() + 30
    while True:
        runtime.call("/v0/control/renew", {"ttlMs": 60000})
        frame = Image.open(
            io.BytesIO(runtime.call("/v0/capture", binary=True))
        ).convert("RGB")
        pixels = list(frame.crop((795, 375, 1130, 710)).resize((32, 32)).getdata())
        count = sum(
            b > 4 and b > r * 1.8 and b > g * 1.8
            if blue
            else (r > 4 and r > g * 1.7 and r > b * 1.7)
            or (g > 4 and g > r * 1.3 and g > b * 1.5)
            for r, g, b in pixels
        )
        if count > len(pixels) * 0.6:
            return
        assert runtime.process.poll() is None and time.monotonic() < deadline, (
            "Fixture wall not drawn"
        )
        time.sleep(0.5)


def arrange(runtime, output, kind, action):
    (output / (kind + "-request.txt")).write_text(action, encoding="utf-8")
    deadline = time.monotonic() + 40
    ack = output / (kind + "-arranged.txt")
    while not ack.exists() or ack.read_text(encoding="utf-8") != action:
        assert runtime.process.poll() is None and time.monotonic() < deadline, action
        runtime.call("/v0/control/renew", {"ttlMs": 60000})
        time.sleep(0.3)


def capture(runtime, output, name):
    runtime.call("/v0/control/renew", {"ttlMs": 60000})
    (output / (name + ".png")).write_bytes(runtime.call("/v0/capture", binary=True))
    evidence = {}
    for key in ["remote-state", "remote-views", "sable-state", "view-state"]:
        path = output / (key + ".json")
        if path.exists():
            evidence[key] = json.loads(path.read_text(encoding="utf-8"))
    m.write_json(output / (name + ".json"), evidence)
    print(output.name, name, flush=True)


def release(runtime, output):
    review.command(runtime, output, "tp @s .5 198.88 -5.5 180 0")
    deadline = time.monotonic() + 10
    while True:
        state = json.loads((output / "remote-state.json").read_text(encoding="utf-8"))
        if all(
            state[key] == 0
            for key in ["sources", "nativeGlassTickets", "sableGlassTickets", "views"]
        ):
            break
        assert runtime.process.poll() is None and time.monotonic() < deadline, state
        time.sleep(0.3)
    assert state["mainConnectionWorldUnchanged"] and state["forcedChunks"] == 0, state
    m.write_json(output / "release.json", state)


def animations(runtime, output):
    for sequence, action in enumerate(["close", "open"], 1):
        (output / "animation-request.txt").write_text(action, encoding="utf-8")
        phase = output / ("animation-phase-" + str(sequence) + ".json")
        deadline = time.monotonic() + 45
        while not phase.exists() or not phase.read_text(encoding="utf-8").strip():
            assert runtime.process.poll() is None and time.monotonic() < deadline
            runtime.call("/v0/control/renew", {"ttlMs": 60000})
            time.sleep(0.3)
        time.sleep(5)
        capture(runtime, output, "animation-" + action)
        resume = output / ("animation-resume-" + str(sequence) + ".json")
        while not resume.exists() or not resume.read_text(encoding="utf-8").strip():
            assert runtime.process.poll() is None and time.monotonic() < deadline
            time.sleep(0.3)
        time.sleep(2)


def open_gui(runtime, output, kind):
    command = (
        "tp @s 16.5 198.88 13.0 0 0" if kind == "terminal" else "tp @s .5 198.88 -3 0 0"
    )
    review.command(runtime, output, "gamemode creative")
    review.command(runtime, output, command)
    runtime.pipeline(
        [
            {"type": "mouse.button", "button": 1, "action": 1, "modifiers": 0},
            {"type": "delay", "durationMs": 40},
            {"type": "mouse.button", "button": 1, "action": 0, "modifiers": 0},
        ]
    )
    time.sleep(1)
    tree = runtime.call("/v0/ui/tree")
    m.write_json(output / ("gui-" + kind + ".json"), tree)
    expected = (
        "TerminalChannelScreen" if kind == "terminal" else "ProjectorChannelScreen"
    )
    assert tree.get("screenClass", "").endswith(expected), tree
    capture(runtime, output, "gui-" + kind)
    runtime.pipeline([{"type": "key.tap", "key": 256, "holdMs": 40}])
    review.command(runtime, output, "gamemode spectator")


def run_case(case):
    runtime, launcher, output = launch(case)
    wait_ready(runtime, output)
    wait_scene(runtime)
    capture(runtime, output, "loaded")
    state = json.loads((output / "remote-state.json").read_text(encoding="utf-8"))
    expected = 0 if case["sable"] == "none" else (2 if case["sable"] == "both" else 1)
    assert state["serverSableShips"] == expected, state
    assert ("sodium" in state["runtimeMods"]) == (
        case["role"] == "client-multiplayer"
    ), state
    if case["sable"] == "none":
        animations(runtime, output)
        review.command(runtime, output, "tp @s 40.5 198.88 -5.5 0 0")
        wait_ready(runtime, output)
        capture(runtime, output, "wireless")
        review.command(runtime, output, "tp @s 40.5 198.88 -5.5 65 0")
        time.sleep(2)
        capture(runtime, output, "wireless-root-outside-frustum")
        review.command(runtime, output, "tp @s .5 198.88 -5.5 0 0")
        wait_ready(runtime, output)
        arrange(runtime, output, "remote", "refs")
        wait_ready(runtime, output)
        for location in ["far", "nether"]:
            arrange(runtime, output, "remote", location)
            wait_ready(runtime, output)
            wait_scene(runtime)
            capture(runtime, output, location)
            arrange(runtime, output, "remote", "change")
            wait_scene(runtime, blue=True)
            capture(runtime, output, location + "-changed")
            if location == "far":
                arrange(runtime, output, "remote", "entity")
                time.sleep(1)
                capture(runtime, output, "far-entity-0")
                time.sleep(1)
                capture(runtime, output, "far-entity-1")
                arrange(runtime, output, "remote", "scale")
                time.sleep(0.5)
                capture(runtime, output, "far-entity-scaled")
                arrange(runtime, output, "remote", "death")
                capture(runtime, output, "far-entity-death")
                time.sleep(2)
                capture(runtime, output, "far-entity-removed")
        arrange(runtime, output, "remote", "private")
        time.sleep(2)
        capture(runtime, output, "private-denied")
        state = json.loads((output / "remote-state.json").read_text(encoding="utf-8"))
        assert state["sources"] == 0 and state["views"] == 0, state
        arrange(runtime, output, "remote", "public")
        wait_ready(runtime, output)
        capture(runtime, output, "public-restored")
    else:
        arrange(runtime, output, "sable", "clip")
        time.sleep(3)
        capture(runtime, output, "axis-clip")
        arrange(runtime, output, "sable", "tilt")
        time.sleep(3)
        capture(runtime, output, "tilted-scaled-pivot-clip")
        arrange(runtime, output, "sable", "motion")
        for index in range(3):
            time.sleep(2)
            capture(runtime, output, "motion-" + str(index))
        arrange(runtime, output, "sable", "reset")
        time.sleep(3)
        capture(runtime, output, "reset")
        open_gui(runtime, output, "terminal")
        open_gui(runtime, output, "projector")
    release(runtime, output)
    review.shutdown(runtime, output)
    assert launcher.wait(60) == 0
    m.write_json(
        output / "runtime-result.json",
        {"status": "PASS_RUNTIME", "visualReview": "pending", "case": case},
    )


def import_runtime(source):
    source = pathlib.Path(source).resolve()
    destination = (BASE / "instance/native_runtime").resolve()
    assert source.name == "runtime21" and source.parent.name == "GlassFeasibility"
    assert destination.is_relative_to((BASE / "instance").resolve())
    destination.mkdir(parents=True, exist_ok=True)
    for name in ["installation", "downloads"]:
        original = (source / name).resolve()
        target = (destination / name).resolve()
        assert original.is_relative_to(source) and target.is_relative_to(destination)
        assert original.exists() and not target.exists(), name
        shutil.move(original, target)
    plan = destination / "downloads/launch.json"

    def relocate(value):
        if isinstance(value, str):
            return value.replace(str(source), str(destination)).replace(
                source.as_posix(), destination.as_posix()
            )
        if isinstance(value, list):
            return [relocate(item) for item in value]
        if isinstance(value, dict):
            return {key: relocate(item) for key, item in value.items()}
        return value

    m.write_json(plan, relocate(json.loads(plan.read_text(encoding="utf-8"))))
    print("Native runtime imported", destination, flush=True)


def native_runtime():
    bind()
    root = BASE / "instance/native_runtime"
    m.HOME = root / "installation"
    m.CACHE = root / "downloads"
    m.RESULT = root / "result"
    assert (m.CACHE / "launch.json").exists(), (
        "Import the verified project-owned native runtime first"
    )


def install_showcase_mods(directory):
    mods = directory / "mods"
    mods.mkdir(parents=True, exist_ok=True)
    for jar in [
        m.snapshot(),
        review.product(),
        review.probe_jar(),
        *[
            m.input_path(name)
            for name in [
                "sable",
                "sodium",
                "customskinloader_acceptance",
                "zume_acceptance",
            ]
        ],
    ]:
        shutil.copyfile(jar, mods / jar.name)
    return mods


def prepare_acceptance():
    native_runtime()
    directory = BASE / "instance/acceptance/client"
    assert not directory.exists(), "Acceptance instance already prepared; preserve it"
    install_showcase_mods(directory)
    cases = json.loads((BASE / "task/cases.json").read_text(encoding="utf-8"))["cases"]
    selected = [
        case
        for case in cases
        if case["name"] in ["sodium-terminal", "sodium-projector", "sodium-both"]
    ]
    owners = set()
    for case in selected:
        source = BASE / "instance" / TARGET / case["role"] / "saves" / case["world"]
        shutil.copytree(source, directory / "saves" / case["world"])
        record = json.loads(
            (BASE / "result" / case["name"] / "loaded.json").read_text(encoding="utf-8")
        )
        owners.add(record["remote-state"]["viewerUUID"])
    assert len(owners) == 1, owners
    m.write_json(directory / "config/glass.json", {"legacyFadeAnimation": False})
    (directory / "options.txt").write_text(
        "lang:zh_cn\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\ntutorialStep:none\nsoundCategory_master:0.5\nsoundCategory_music:0.0\nrenderDistance:8\nsimulationDistance:5\ngraphicsMode:1\n",
        encoding="utf-8",
        newline="\n",
    )
    m.write_json(
        BASE / "result/acceptance-prepared.json",
        {
            "state": "PREPARED_NOT_STARTED",
            "directory": str(directory),
            "profile": CONFIG["profiles"]["acceptance"],
            "playerUUID": next(iter(owners)),
            "worlds": [case["world"] for case in selected],
            "demo": "F8 pauses/resumes native Sable motion; no fixture creation in acceptance",
            "launch": "python components/runtime_validation/remote_projection.py acceptance",
        },
    )
    print("Acceptance prepared, not started", directory, flush=True)


def packaged_smoke():
    native_runtime()
    directory = BASE / "instance/packaged/client"
    output = BASE / "result/packaged"
    assert not directory.exists(), "Preserve the previous packaged run before repeating"
    output.mkdir(parents=True, exist_ok=True)
    mods = install_showcase_mods(directory)
    world = "GLASS B sodium both"
    source = BASE / "instance" / TARGET / "client-multiplayer/saves" / world
    shutil.copytree(source, directory / "saves" / world)
    record = json.loads(
        (BASE / "result/sodium-both/loaded.json").read_text(encoding="utf-8")
    )
    owner = record["remote-state"]["viewerUUID"]
    (directory / "options.txt").write_text(
        "lang:zh_cn\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\ntutorialStep:none\nsoundCategory_master:0.0\nsoundCategory_music:0.0\nrenderDistance:8\nsimulationDistance:5\nchatVisibility:0\ngraphicsMode:1\n",
        encoding="utf-8",
        newline="\n",
    )
    process = m.launch(
        "automatic",
        directory,
        world=world,
        result_directory=output,
        player_uuid=uuid.UUID(owner).hex,
        extra_scopes=("command",),
        jvm_args=[
            "-XX:ActiveProcessorCount=4",
            "-Dglass.acceptance.demo=true",
            "-Dvalidation.result=" + str(output),
        ],
        graphics_backend=None,
    )
    endpoint = directory / "minecraft-protocol/endpoint.json"
    deadline = time.monotonic() + 180
    while not endpoint.exists():
        assert process.poll() is None and time.monotonic() < deadline, (
            "Packaged client failed to start"
        )
        time.sleep(0.5)
    runtime = m.Runtime(directory, process)
    review.constrain(process)
    review.ready(runtime, output)
    review.command(runtime, output, "tp @s .5 198.88 -5.5 0 0")
    time.sleep(12)
    capture(runtime, output, "moving-0")
    first = json.loads((output / "demo-state.json").read_text(encoding="utf-8"))
    time.sleep(2)
    capture(runtime, output, "moving-1")
    second = json.loads((output / "demo-state.json").read_text(encoding="utf-8"))
    assert len(second["ships"]) == 2 and first["ships"] != second["ships"], (
        first,
        second,
    )
    runtime.pipeline([{"type": "key.tap", "key": 297, "holdMs": 40}])
    time.sleep(1)
    assert not json.loads((output / "demo-state.json").read_text(encoding="utf-8"))[
        "moving"
    ]
    capture(runtime, output, "paused")
    runtime.pipeline([{"type": "key.tap", "key": 297, "holdMs": 40}])
    time.sleep(1)
    assert json.loads((output / "demo-state.json").read_text(encoding="utf-8"))[
        "moving"
    ]
    review.shutdown(runtime, output)
    m.write_json(
        output / "runtime-result.json",
        {
            "status": "PASS_RUNTIME",
            "visualReview": "pending",
            "mods": [jar.name for jar in mods.glob("*.jar")],
            "exitCode": process.poll(),
        },
    )


def acceptance():
    native_runtime()
    data = json.loads(
        (BASE / "result/acceptance-prepared.json").read_text(encoding="utf-8")
    )
    output = BASE / "result/acceptance-launch"
    output.mkdir(parents=True, exist_ok=True)
    process = m.launch(
        "acceptance",
        pathlib.Path(data["directory"]),
        world="GLASS B sodium both",
        result_directory=output,
        player_uuid=uuid.UUID(data["playerUUID"]).hex,
        jvm_args=["-Dglass.acceptance.demo=true", "-Dvalidation.result=" + str(output)],
        graphics_backend=None,
    )
    m.write_json(
        output / "handoff.json",
        {
            "pid": process.pid,
            "directory": data["directory"],
            "playerUUID": data["playerUUID"],
            "doNotClose": True,
        },
    )
    print("Acceptance launched", process.pid, flush=True)


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "import-runtime":
        import_runtime(sys.argv[2])
        return
    if len(sys.argv) > 1 and sys.argv[1] == "prepare-acceptance":
        prepare_acceptance()
        return
    if len(sys.argv) > 1 and sys.argv[1] == "acceptance":
        acceptance()
        return
    if len(sys.argv) > 1 and sys.argv[1] == "packaged-smoke":
        packaged_smoke()
        return
    cases = json.loads((BASE / "task/cases.json").read_text(encoding="utf-8"))["cases"]
    if len(sys.argv) > 1:
        cases = [case for case in cases if case["name"] in sys.argv[1:]]
    assert cases, "No matching scenario"
    for case in cases:
        run_case(case)


if __name__ == "__main__":
    main()
