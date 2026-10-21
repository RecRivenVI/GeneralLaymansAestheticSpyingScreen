"""Probe-only measurements of unchanged GLASS A/B jars in owned installed clients."""

import csv
import io
import json
import os
import pathlib
import shutil
import socket
import subprocess
import sys
import time
import tomllib
import uuid

from PIL import Image

import remote_projection as r

BASE = r.m.ROOT / "validations/performance-remote_projection"
CONFIG = tomllib.loads((BASE / "validation.toml").read_text(encoding="utf-8"))


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def wait_file(path, condition=lambda value: True, timeout=180, process=None):
    deadline = time.monotonic() + timeout
    while True:
        if path.exists() and path.stat().st_size:
            data = read(path)
            if condition(data):
                return data
        assert time.monotonic() < deadline and (
            process is None or process.poll() is None
        ), path
        time.sleep(0.25)


def command(output, spec):
    r.m.write_json(output / "command.json", spec)
    return wait_file(output / "ack.json", lambda value: value == spec)


def launch(name, route="B", sodium=False, existing=False, sable=False):
    r.native_runtime()
    r.m.quota()
    directory = BASE / "instance" / name / "client"
    output = BASE / "result" / name
    assert not directory.exists(), "Preserve the preceding run before repeating"
    output.mkdir(parents=True, exist_ok=True)
    mods = directory / "mods"
    mods.mkdir(parents=True)
    product = (
        r.review.product() if route == "B" else r.m.input_path(CONFIG["products"]["a"])
    )
    jars = [product, r.review.probe_jar(), r.m.snapshot()]
    if sodium:
        jars.append(r.m.input_path("sodium"))
    if sable:
        jars.append(r.m.input_path("sable"))
    for jar in jars:
        shutil.copyfile(jar, mods / jar.name)
    world = "GLASS B sodium terminal" if sable else "GLASS Performance"
    if existing:
        source = (
            (r.BASE / "instance/1.21.1-neoforge/client-multiplayer/saves" / world)
            if sable
            else BASE / "instance/fixture" / world
        )
        shutil.copytree(source, directory / "saves" / world)
    r.m.write_json(directory / "glass-scene.json", {"world": world})
    (directory / "options.txt").write_text(
        "lang:en_us\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\n"
        "tutorialStep:none\nsoundCategory_master:0.0\nsoundCategory_music:0.0\n"
        "renderDistance:8\nsimulationDistance:5\nchatVisibility:0\ngraphicsMode:1\n"
        "enableVsync:false\nmaxFps:260\n",
        encoding="utf-8",
        newline="\n",
    )
    process = r.m.launch(
        "automatic",
        directory,
        world=world if existing else None,
        result_directory=output,
        extra_scopes=("command",),
        graphics_backend=None,
        player_uuid=uuid.UUID(
            read(r.BASE / "result/sodium-terminal/loaded.json")["remote-state"][
                "viewerUUID"
            ]
        ).hex
        if sable
        else None,
        jvm_args=[
            "-XX:ActiveProcessorCount=4",
            "-Dglass.performance=true",
            "-Dglass.performance.route=" + route,
            "-Dglass.overlay.fresh_world=" + str(not existing).lower(),
            "-Dglass.performance.existing=" + str(existing).lower(),
            "-Dglass.performance.sable=" + str(sable).lower(),
            "-Dvalidation.result=" + str(output),
        ],
    )
    wait_file(directory / "minecraft-protocol/endpoint.json", process=process)
    runtime = r.m.Runtime(directory, process)
    r.review.constrain(process)
    r.review.ready(runtime, output)
    wait_file(output / "ready.json", process=process)
    r.review.command(
        runtime,
        output,
        "tp @s .5 198.88 -5.5 0 0" if sable else "tp @s .5 78.88 -16 0 0",
    )
    stop = output / "monitor.stop"
    monitor_log = (output / "monitor-console.log").open("w", encoding="utf-8")
    monitor = subprocess.Popen(
        [
            "pwsh",
            "-NoProfile",
            "-File",
            str(r.m.ROOT / "components/runtime_validation/performance_monitor.ps1"),
            "-OutputPath",
            str(output / "host.jsonl"),
            "-StopPath",
            str(stop),
        ],
        stdout=monitor_log,
        stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW,
    )
    monitor_log.close()
    return runtime, process, output, monitor, stop


def measure(runtime, output, label, networks, *, seconds=15, warmup=10, gpu=True):
    command(output, {"action": "set", "networks": networks})
    deadline = time.monotonic() + warmup
    while time.monotonic() < deadline:
        runtime.call("/v0/control/renew", {"ttlMs": 60000})
        time.sleep(min(2, max(0.1, deadline - time.monotonic())))
    (output / (label + "-scene.png")).write_bytes(
        runtime.call("/v0/capture", binary=True)
    )
    command(
        output, {"action": "measure", "label": label, "seconds": seconds, "gpu": gpu}
    )
    wait_file(
        output / "done.json",
        lambda value: value["label"] == label,
        timeout=seconds + 30,
    )
    summary = read(output / (label + ".json"))
    summary["networksRequested"] = networks
    r.m.write_json(
        output / (label + "-request.json"),
        {"networks": networks, "warmup": warmup, "seconds": seconds, "gpu": gpu},
    )
    print(
        json.dumps(
            {
                "window": label,
                "frames": summary["frames"],
                "resources": summary["resources"],
            }
        ),
        flush=True,
    )
    return summary


def close(runtime, process, output, monitor, stop, multiplayer=False):
    if multiplayer:
        runtime.call("/v0/input/release-all", {})
        if not runtime.call("/v0/ui/tree").get("screenClass"):
            runtime.pipeline([{"type": "key.tap", "key": 256, "holdMs": 40}])
        try:
            runtime.click({"translationKey": "menu.disconnect"})
        except RuntimeError as error:
            if "REQUEST_CANCELLED" not in str(error):
                raise
        tree = runtime.call("/v0/ui/tree")
        if tree.get("screenClass", "").endswith("JoinMultiplayerScreen"):
            try:
                runtime.click({"translationKey": "gui.back"})
            except RuntimeError as error:
                if "REQUEST_CANCELLED" not in str(error):
                    raise
        runtime.title()
        try:
            runtime.click({"translationKey": "menu.quit"})
        except RuntimeError as error:
            if not any(
                key in str(error)
                for key in ["REQUEST_CANCELLED", "RUNTIME_SHUTTING_DOWN"]
            ):
                raise
        assert process.wait(60) == 0 and not runtime.endpoint_path.exists()
        r.m.write_json(output / "exit.json", {"exitCode": 0, "endpointRemoved": True})
    else:
        r.review.shutdown(runtime, output)
    assert process.wait(60) == 0
    stop.write_text("stop", encoding="utf-8")
    assert monitor.wait(15) == 0


def pilot(name="pilot"):
    values = launch(name)
    runtime, process, output, monitor, stop = values
    for label, networks in [("off-0", 0), ("one-1", 1), ("one-2", 1), ("off-3", 0)]:
        measure(runtime, output, label, networks, seconds=6, warmup=10)
    measure(runtime, output, "off-no-gpu", 0, seconds=6, warmup=3, gpu=False)
    close(*values)
    fixture = BASE / "instance/fixture/GLASS Performance"
    assert not fixture.exists()
    shutil.copytree(runtime.directory / "saves/GLASS Performance", fixture)


def analyze():
    results = []
    for path in (BASE / "result").glob("*/*.csv"):
        rows = list(csv.DictReader(path.open(encoding="utf-8")))[1:]
        metrics = {}
        for key in rows[0]:
            values = sorted(int(row[key]) / 1e6 for row in rows if int(row[key]) >= 0)
            if values:
                metrics[key] = {
                    "p50": values[int((len(values) - 1) * 0.5)],
                    "p95": values[int((len(values) - 1) * 0.95)],
                    "p99": values[int((len(values) - 1) * 0.99)],
                    "mean": sum(values) / len(values),
                }
        results.append(
            {"window": str(path.relative_to(BASE)), "frames": len(rows), "ms": metrics}
        )
    r.m.write_json(BASE / "result/summary.json", results)
    print(json.dumps(results, ensure_ascii=False, indent=2), flush=True)


def group(name, route, sodium, networks):
    if (BASE / "result" / name / "exit.json").exists():
        assert read(BASE / "result" / name / "exit.json")["exitCode"] == 0
        for i, count in enumerate(networks):
            assert (
                read(BASE / "result" / name / f"{i:02d}-n{count}.json")["resources"][
                    "contexts"
                ]
                == count
            )
        return
    values = launch(name, route=route, sodium=sodium, existing=True)
    runtime, process, output, monitor, stop = values
    for i, count in enumerate(networks):
        result = measure(runtime, output, f"{i:02d}-n{count}", count, warmup=12)
        assert result["resources"]["contexts"] == count, result["resources"]
        assert result["resources"]["targets"] == count, result["resources"]
    close(*values)


def full_client():
    if not (BASE / "result/abba-a-first/exit.json").exists():
        group("abba-a-first", "A", False, [0, 1, 2, 4])
    group("matrix-b-vanilla", "B", False, [0, 1, 2, 4, 4, 2, 1, 0])
    group("abba-a-last", "A", False, [4, 2, 1, 0])
    group("matrix-b-sodium", "B", True, [0, 1, 2, 4, 4, 2, 1, 0])
    analyze()


def network(name="network-final"):
    if (BASE / "result" / name / "exit.json").exists():
        assert read(BASE / "result" / name / "exit.json")["exitCode"] == 0
        assert read(BASE / "result" / (name + "-client") / "exit.json")["exitCode"] == 0
        return
    r.native_runtime()
    server_dir = BASE / "instance/1.21.1-neoforge/server"
    server_dir.mkdir(parents=True, exist_ok=True)
    server_out = BASE / "result" / name
    server_out.mkdir(parents=True, exist_ok=True)
    assert not (server_out / "server-ready.json").exists(), (
        "Preserve the previous server run"
    )
    with socket.socket() as available:
        available.bind(("127.0.0.1", 0))
        port = available.getsockname()[1]
    (server_dir / "server.properties").write_text(
        f"server-ip=127.0.0.1\nserver-port={port}\nlevel-name=GLASS Performance {name}\nonline-mode=false\nwhite-list=false\n"
        "level-type=minecraft:flat\nlevel-seed=20261020\ngenerate-structures=false\n"
        'generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\n'
        "gamemode=creative\nspawn-protection=0\nview-distance=8\nsimulation-distance=5\n"
        "allow-flight=true\nmax-players=4\n",
        encoding="utf-8",
        newline="\n",
    )
    log = (server_out / "native-server.log").open("w", encoding="utf-8")
    environment = os.environ.copy()
    environment["GLASS_PERFORMANCE_SERVER_CASE"] = name
    server = subprocess.Popen(
        [
            str(r.m.ROOT / "gradlew.bat"),
            "--offline",
            "--no-configuration-cache",
            ":version:1.21.1-neoforge:runValidationPerformanceRemoteProjectionServer",
        ],
        cwd=r.m.ROOT,
        stdout=log,
        env=environment,
        stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW,
    )
    log.close()
    identity = wait_file(server_out / "server-ready.json", timeout=240, process=server)
    r.review.constrain(r.m.ExistingProcess(identity["pid"]))
    directory = BASE / "instance" / (name + "-client") / "client"
    output = BASE / "result" / (name + "-client")
    assert not directory.exists()
    mods = directory / "mods"
    mods.mkdir(parents=True)
    output.mkdir(parents=True, exist_ok=True)
    for jar in [r.review.product(), r.review.probe_jar(), r.m.snapshot()]:
        shutil.copyfile(jar, mods / jar.name)
    (directory / "options.txt").write_text(
        "lang:en_us\nonboardAccessibility:false\npauseOnLostFocus:false\nsoundCategory_master:0.0\nsoundCategory_music:0.0\nrenderDistance:8\nsimulationDistance:5\nchatVisibility:0\ngraphicsMode:1\n",
        encoding="utf-8",
        newline="\n",
    )
    process = r.m.launch(
        "automatic",
        directory,
        result_directory=output,
        multiplayer="127.0.0.1:" + str(identity["port"]),
        extra_scopes=("command",),
        graphics_backend=None,
        jvm_args=[
            "-XX:ActiveProcessorCount=4",
            "-Dglass.performance=true",
            "-Dglass.performance.existing=true",
            "-Dvalidation.result=" + str(output),
        ],
    )
    wait_file(directory / "minecraft-protocol/endpoint.json", process=process)
    runtime = r.m.Runtime(directory, process)
    r.review.constrain(process)
    r.review.ready(runtime, output)
    wait_file(server_out / "fixture-ready.json", process=server)
    r.review.command(runtime, output, "tp @s .5 78.88 -15.5 0 0")
    stop = output / "monitor.stop"
    monitor_log = (output / "monitor-console.log").open("w", encoding="utf-8")
    monitor = subprocess.Popen(
        [
            "pwsh",
            "-NoProfile",
            "-File",
            str(r.m.ROOT / "components/runtime_validation/performance_monitor.ps1"),
            "-OutputPath",
            str(output / "host.jsonl"),
            "-StopPath",
            str(stop),
        ],
        stdout=monitor_log,
        stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW,
    )
    monitor_log.close()
    for i, (count, second) in enumerate(
        [(0, False), (1, False), (1, True), (1, True), (1, False), (0, False)]
    ):
        command(server_out, {"action": "set", "networks": count, "second": second})
        for _ in range(7):
            runtime.call("/v0/control/renew", {"ttlMs": 60000})
            time.sleep(2)
        label = f"{i:02d}-viewers{0 if count == 0 else 2 if second else 1}"
        command(server_out, {"action": "measure", "label": label, "seconds": 15})
        wait_file(
            server_out / "done.json",
            lambda value: value["label"] == label,
            timeout=45,
            process=server,
        )
        report = read(server_out / (label + ".json"))
        assert report["sources"] == (1 if count else 0), report
        assert report["fullChunks"] == (81 if count else 0), report
        print(
            json.dumps(
                {
                    "networkWindow": label,
                    "ticks": len(report["ticks"]),
                    "wireSent": report["wireSent"],
                    "payload": report["encodedGlassPayload"],
                }
            ),
            flush=True,
        )
    close(runtime, process, output, monitor, stop, multiplayer=True)
    r.m.write_json(server_out / "command.json", {"action": "stop"})
    assert server.wait(120) == 0
    r.m.write_json(server_out / "exit.json", {"exitCode": 0})


def visual_ready(runtime, output, label, *, sable=False):
    began = time.monotonic()
    deadline = began + 90
    while True:
        runtime.call("/v0/control/renew", {"ttlMs": 60000})
        png = runtime.call("/v0/capture", binary=True)
        frame = Image.open(io.BytesIO(png)).convert("RGB")
        roi = (800, 380, 1120, 700) if sable else (1330, 440, 1460, 550)
        pixels = list(frame.crop(roi).resize((32, 32)).getdata())
        count = sum(
            (r > 4 and r > b * 1.5) or (g > 4 and g > r * 1.5 and g > b * 0.7)
            for r, g, b in pixels
        )
        if count / len(pixels) > (0.3 if sable else 0.55):
            (output / (label + ".png")).write_bytes(png)
            return {
                "seconds": time.monotonic() - began,
                "criterion": "colored marker fraction",
                "fraction": count / len(pixels),
                "screenshotPolling": True,
            }
        if time.monotonic() >= deadline:
            (output / (label + "-timeout.png")).write_bytes(png)
            raise AssertionError("Projected marker did not appear")
        time.sleep(0.5)


def record_window(runtime, output, label, seconds=15):
    command(
        output, {"action": "measure", "label": label, "seconds": seconds, "gpu": True}
    )
    wait_file(
        output / "done.json",
        lambda value: value["label"] == label,
        timeout=seconds + 30,
    )
    result = read(output / (label + ".json"))
    print(
        json.dumps(
            {
                "window": label,
                "frames": result["frames"],
                "resources": result["resources"],
            }
        ),
        flush=True,
    )
    return result


def remote_cases():
    if not (BASE / "result/remote-transitions/exit.json").exists():
        remote_static(launch("remote-transitions", sodium=True, existing=True))
    if not (BASE / "result/sable-motion/exit.json").exists():
        sable_case()
    if not (BASE / "result/remote-relocation/exit.json").exists():
        relocation_cases()
    analyze()


def remote_static(values, *, resume_far=False):
    runtime, process, output, monitor, stop = values
    for place in ["far", "nether"]:
        command(output, {"action": "set", "networks": 0, "place": place})
        time.sleep(8)
        requested = time.time()
        command(output, {"action": "set", "networks": 1})
        latency = visual_ready(runtime, output, place + "-first")
        latency["fromRequestSeconds"] = time.time() - requested
        result = (
            read(output / "far-steady.json")
            if resume_far and place == "far"
            else record_window(runtime, output, place + "-steady")
        )
        assert result["resources"]["fullChunks"] == 81, result["resources"]
        assert any(
            ("8192" if place == "far" else "the_nether") in terminal
            for terminal in result["resources"]["sourceTerminals"]
        ), result["resources"]
        r.m.write_json(
            output / (place + "-latency.json"),
            {"first": latency},
        )
    close(*values)


def sable_case():
    values = launch("sable-motion", sodium=True, existing=True, sable=True)
    runtime, process, output, monitor, stop = values
    first = visual_ready(runtime, output, "first", sable=True)
    record_window(runtime, output, "stationary")
    command(
        output, {"action": "measure", "label": "moving", "seconds": 35, "gpu": True}
    )
    command(output, {"action": "motion", "enabled": True})
    wait_file(
        output / "done.json", lambda value: value["label"] == "moving", timeout=65
    )
    command(output, {"action": "motion", "enabled": False})
    (output / "after-motion.png").write_bytes(runtime.call("/v0/capture", binary=True))
    moved = read(output / "moving.json")
    assert moved["resources"]["fullChunks"] == 82, moved["resources"]
    r.m.write_json(
        output / "latency.json",
        {
            "first": first,
            "movement": "native Sable translation at 0.8 blocks per second plus three-axis rotation and nonuniform scaling; source shape/ready events in moving.json",
        },
    )
    close(*values)
    analyze()


def relocation_cases():
    values = launch("remote-relocation", sodium=True, existing=True)
    runtime, process, output, monitor, stop = values
    for place in ["far", "nether"]:
        command(output, {"action": "set", "networks": 0, "place": place})
        time.sleep(8)
        command(output, {"action": "set", "networks": 1})
        visual_ready(runtime, output, place + "-before")
        label = place + "-cross"
        command(
            output, {"action": "measure", "label": label, "seconds": 15, "gpu": True}
        )
        requested = time.time()
        command(output, {"action": "set", "networks": 1, "place": place + "_shift"})
        setup = time.time() - requested
        wait_file(
            output / "done.json", lambda value: value["label"] == label, timeout=40
        )
        (output / (place + "-settled.png")).write_bytes(
            runtime.call("/v0/capture", binary=True)
        )
        r.m.write_json(
            output / (place + "-latency.json"),
            {
                "setupSeconds": setup,
                "observationUpperBoundSeconds": time.time() - requested,
                "visualFirstAppearanceTimed": False,
                "method": "32-block static terminal relocation; snapshot hold interval is obtained from source-shape/snapshot-ready in the measured window, not image hue",
            },
        )
    close(*values)


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "analyze":
        analyze()
    elif len(sys.argv) > 1 and sys.argv[1] == "all":
        if not (BASE / "instance/fixture/GLASS Performance").exists():
            pilot("pilot-fixed")
        full_client()
        remote_cases()
        network()
        import performance_report

        performance_report.analyze()
    elif len(sys.argv) > 1 and sys.argv[1] == "client":
        full_client()
    elif len(sys.argv) > 1 and sys.argv[1] == "network":
        network()
    elif len(sys.argv) > 1 and sys.argv[1] == "remote":
        remote_cases()
    else:
        pilot(sys.argv[2] if len(sys.argv) > 2 else "pilot")
