"""Route A fixed scenes in project-owned installed-loader clients, operated through MCP."""

import ctypes
import datetime
import json
import pathlib
import shutil
import sys
import time
import tomllib
import mcp_alpha as m

BASE = m.ROOT / "validations/visual-render_review"
CONFIG = tomllib.loads((BASE / "validation.toml").read_text("utf-8"))
WORLD = CONFIG["fixture"]["world"]


def snapshot():
    return m.input_path(CONFIG["mcp"]["input"])


def product():
    facts = m.properties(m.ROOT / "gradle.properties")
    return (
        m.ROOT
        / "versions"
        / m.TARGET
        / "build/libs"
        / f"{facts['mod_id']}-{m.TARGET}-{facts['mod_version']}.jar"
    )


def probe_jar():
    return next((m.ROOT / "versions" / m.TARGET / "build/libs").glob("*-probe.jar"))


def constrain(process):
    api = ctypes.WinDLL("kernel32", use_last_error=True)
    api.OpenProcess.restype = ctypes.c_void_p
    api.OpenProcess.argtypes = [ctypes.c_ulong, ctypes.c_bool, ctypes.c_ulong]
    api.GetProcessAffinityMask.argtypes = [
        ctypes.c_void_p,
        ctypes.POINTER(ctypes.c_size_t),
        ctypes.POINTER(ctypes.c_size_t),
    ]
    api.SetProcessAffinityMask.argtypes = [ctypes.c_void_p, ctypes.c_size_t]
    api.SetPriorityClass.argtypes = [ctypes.c_void_p, ctypes.c_ulong]
    api.CloseHandle.argtypes = [ctypes.c_void_p]
    handle = api.OpenProcess(0x0200 | 0x0400, False, process.pid)
    assert handle
    try:
        available, system = ctypes.c_size_t(), ctypes.c_size_t()
        assert api.GetProcessAffinityMask(
            handle, ctypes.byref(available), ctypes.byref(system)
        )
        bits = [1 << i for i in range(64) if available.value & (1 << i)][:4]
        mask = sum(bits)
        assert len(bits) == 4 and api.SetProcessAffinityMask(handle, mask)
        assert api.SetPriorityClass(handle, 0x4000)
        return {"logicalCpus": 4, "affinityMask": mask, "priority": "BELOW_NORMAL"}
    finally:
        api.CloseHandle(handle)


def ready(runtime, output, *, human=False):
    runtime.acquire()
    deadline = time.monotonic() + 150
    while True:
        tree = runtime.call("/v0/ui/tree")
        player = runtime.call("/v0/player?category=USER")
        if tree.get("screenClass", "").endswith("BackupConfirmScreen") and not tree.get(
            "overlayIdentity"
        ):
            runtime.click({"translationKey": "selectWorld.backupJoinConfirmButton"})
        if (
            tree.get("screenClass", "").endswith("ConfirmScreen")
            and not tree.get("overlayIdentity")
            and any(
                node.get("translationKey") == "upgradeWorld.done"
                for node in tree.get("children", [])
            )
        ):
            runtime.click({"translationKey": "gui.yes"})
        if (
            player.get("available")
            and not tree.get("screenClass")
            and not tree.get("overlayIdentity")
        ):
            break
        assert runtime.process.poll() is None and time.monotonic() < deadline
        runtime.call("/v0/control/renew", {"ttlMs": 60000})
        time.sleep(0.5)
    before = player["yaw"]
    runtime.pipeline([{"type": "mouse.delta", "dx": 36, "dy": 0}])
    time.sleep(1)
    first = runtime.call("/v0/player?category=USER")["yaw"]
    workaround = abs(first - before) < 0.1
    if workaround:
        runtime.pipeline([{"type": "key.tap", "key": 256, "holdMs": 40}])
        runtime.click({"translationKey": "menu.returnToGame"})
        runtime.pipeline([{"type": "mouse.delta", "dx": 36, "dy": 0}])
        time.sleep(1)
        assert abs(runtime.call("/v0/player?category=USER")["yaw"] - first) > 0.1
    m.write_json(
        output / "first-entry-turn.json",
        {
            "snapshot": CONFIG["mcp"]["snapshot"],
            "beforeYaw": before,
            "firstYaw": first,
            "cf072Observed": workaround,
            "workaround": "native Esc and Return to Game"
            if workaround
            else "not needed",
        },
    )
    tree = runtime.call("/v0/ui/tree")
    profile = CONFIG["profiles"]["acceptance" if human else "automatic"]
    assert (tree["windowWidth"], tree["windowHeight"]) == (
        profile["width"],
        profile["height"],
    )


def command(runtime, output, text):
    runtime.call("/v0/control/renew", {"ttlMs": 60000})
    result = runtime.call("/v0/command/player", {"command": text, "route": "NATIVE"})
    with (output / "commands.jsonl").open("a", encoding="utf-8") as stream:
        stream.write(
            json.dumps({"command": text, "result": result}, ensure_ascii=False) + "\n"
        )
    assert result.get("accepted"), result
    time.sleep(0.35)


def capture_fade(runtime, output, scene, power, powered):
    before = max(
        (int(path.stem.split("-")[1]) for path in output.glob("phase-*.json")),
        default=0,
    )
    command(
        runtime,
        output,
        "setblock "
        + power
        + (" minecraft:redstone_block" if powered else " minecraft:stone"),
    )
    expected = before + 1
    phase_file = output / f"phase-{expected}.json"
    deadline = time.monotonic() + 40
    while not phase_file.exists():
        assert runtime.process.poll() is None and time.monotonic() < deadline
        time.sleep(0.2)
    phase = json.loads(phase_file.read_text("utf-8"))
    assert phase["scene"] == scene["name"] and phase["powered"] is powered
    assert phase["holdUntilMillis"] - time.time() * 1000 > 2000
    suffix = "opening" if powered else "closing"
    (output / (scene["name"] + "_" + suffix + ".png")).write_bytes(
        runtime.call("/v0/capture", binary=True)
    )
    while not (output / f"resume-{expected}.json").exists():
        assert runtime.process.poll() is None and time.monotonic() < deadline
        time.sleep(0.2)
    time.sleep(3)


def prepare(directory, *, classic=False, source=None, human=False):
    directory.mkdir(parents=True)
    mods = directory / "mods"
    mods.mkdir()
    for jar in [snapshot(), product(), probe_jar()]:
        shutil.copyfile(jar, mods / jar.name)
    source = source or m.ROOT / CONFIG["fixture"]["source"]
    shutil.copytree(source, directory / "saves" / WORLD)
    m.write_json(directory / "config/glass.json", {"legacyFadeAnimation": classic})
    profile = CONFIG["profiles"]["acceptance" if human else "automatic"]
    (directory / "options.txt").write_text(
        "lang:en_us\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\ntutorialStep:none\n"
        "renderDistance:8\nsimulationDistance:5\nclouds:fancy\nchatVisibility:2\nimprovedTransparency:false\n"
        f"soundCategory_master:{profile['master-volume']}\nsoundCategory_music:0.0\n",
        encoding="utf-8",
        newline="\n",
    )


def launch(directory, output, *, human=False):
    output.mkdir(parents=True)
    process = m.launch(
        "acceptance" if human else "reference",
        directory,
        world=WORLD,
        result_directory=output,
        extra_scopes=("command",),
        jvm_args=[
            "-XX:ActiveProcessorCount=4",
            "-Dglass.probe.models_only=true" if human else "-Dglass.render.review=true",
            "-Dvalidation.target=" + m.TARGET,
            "-Dvalidation.result=" + str(output),
        ],
    )
    constraints = constrain(process)
    deadline = time.monotonic() + 180
    while not (directory / "minecraft-protocol/endpoint.json").exists():
        assert process.poll() is None and time.monotonic() < deadline, (
            "Client startup failed; inspect console"
        )
        time.sleep(0.5)
    runtime = m.Runtime(directory, process)
    m.write_json(
        output / "runtime.json",
        {"runtime": runtime.endpoint, "constraints": constraints},
    )
    m.write_json(
        BASE / "result/current.json",
        {
            "directory": str(directory),
            "output": str(output),
            "runtime": runtime.endpoint,
        },
    )
    return runtime


def connect():
    current = json.loads((BASE / "result/current.json").read_text("utf-8"))
    runtime = m.Runtime(
        pathlib.Path(current["directory"]), m.ExistingProcess(current["runtime"]["pid"])
    )
    assert runtime.endpoint == current["runtime"]
    return runtime, pathlib.Path(current["output"])


def shutdown(runtime, output):
    runtime.call("/v0/input/release-all", {})
    tree = runtime.call("/v0/ui/tree")
    if not tree.get("screenClass", "").endswith("TitleScreen"):
        if not tree.get("screenClass"):
            runtime.pipeline([{"type": "key.tap", "key": 256, "holdMs": 40}])
        try:
            runtime.click({"translationKey": "menu.returnToMenu"})
        except RuntimeError as error:
            if "REQUEST_CANCELLED" not in str(error):
                raise
            m.write_json(
                output / "navigation.json",
                {
                    "outcome": "not_confirmed",
                    "response": str(error),
                    "followup": "observe title without replay",
                },
            )
    runtime.title()
    try:
        runtime.click({"translationKey": "menu.quit"})
    except RuntimeError as error:
        if "REQUEST_CANCELLED" not in str(error) and "RUNTIME_SHUTTING_DOWN" not in str(
            error
        ):
            raise
    assert runtime.process.wait(60) == 0 and not runtime.endpoint_path.exists()
    m.write_json(output / "exit.json", {"exitCode": 0, "endpointRemoved": True})


def capture(runtime, output, name, camera, *, settle=3):
    command(runtime, output, "tp @s " + " ".join(str(value) for value in camera))
    fixture = json.loads((output / "fixture.json").read_text("utf-8"))
    scene = next(
        (item["name"] for item in fixture["scenes"] if item["name"] in name), None
    )
    if "grazing_" in name and "thin_" not in name or "opaque_foreground" in name:
        scene = "full_north"
    if "thin_grazing" in name:
        scene = "full_up"
    if scene:
        deadline = time.monotonic() + 30
        last_renew = 0
        while True:
            path = output / "view-state.json"
            observed = json.loads(path.read_text("utf-8")) if path.exists() else None
            view = (
                next(
                    (value for value in observed["views"] if value["scene"] == scene),
                    None,
                )
                if observed
                else None
            )
            if (
                view
                and all(
                    view[key]
                    for key in [
                        "projectorLoaded",
                        "terminalLoaded",
                        "powered",
                        "networkPresent",
                        "active",
                    ]
                )
                and all(
                    abs(observed["player"][axis] - camera[axis]) < 0.1
                    for axis in range(3)
                )
            ):
                m.write_json(output / (name + "-state.json"), observed)
                break
            assert runtime.process.poll() is None and time.monotonic() < deadline, (
                f"{scene} did not become ready: {view}"
            )
            if time.monotonic() - last_renew > 10:
                runtime.call("/v0/control/renew", {"ttlMs": 60000})
                last_renew = time.monotonic()
            time.sleep(0.1)
    time.sleep(settle)
    (output / (name + ".png")).write_bytes(runtime.call("/v0/capture", binary=True))
    print(name, flush=True)


def transparency(runtime, enabled):
    options = runtime.directory / "options.txt"
    actual = "improvedTransparency:true" in options.read_text("utf-8")
    if actual != enabled:
        runtime.pipeline(
            [
                {"type": "key", "key": 292, "action": 1},
                {"type": "key.tap", "key": 88, "holdMs": 40},
                {"type": "key", "key": 292, "action": 0},
            ]
        )
        time.sleep(0.5)
    assert ("improvedTransparency:true" in options.read_text("utf-8")) == enabled


def lighting(runtime, output):
    blocks = [
        (1840, "glass:glass_projector[facing=north]"),
        (1842, "glass:glass_extension"),
        (1844, "minecraft:glass"),
        (1846, "glass:glass_terminal[facing=south]"),
        (1848, "minecraft:obsidian"),
        (1850, "glass:glass_wireless_extension"),
    ]
    for x, block in blocks:
        command(runtime, output, f"setblock {x} 80 0 {block}")
    camera = [1845.5, 79.5, -9, 0, 3]
    capture(runtime, output, "lighting_day", camera)
    command(runtime, output, "time set 18000")
    capture(runtime, output, "lighting_night", camera)
    command(runtime, output, "setblock 1843 80 -1 minecraft:torch")
    capture(runtime, output, "lighting_torch", camera)
    command(runtime, output, "time set 6000")
    command(runtime, output, "fill 1838 82 -2 1852 82 2 minecraft:stone")
    command(runtime, output, "setblock 1843 80 -1 minecraft:air")
    capture(runtime, output, "lighting_shadow", camera)
    command(runtime, output, "fill 1838 82 -2 1852 82 2 minecraft:air")


def stable_scenes(runtime, output, fixture, enabled):
    prefix = "oit_on_" if enabled else "oit_off_"
    faces = {
        "down": (0, -1, 0),
        "up": (0, 1, 0),
        "north": (0, 0, -1),
        "south": (0, 0, 1),
        "west": (-1, 0, 0),
        "east": (1, 0, 0),
    }
    for scene in fixture["scenes"]:
        capture(runtime, output, prefix + scene["name"], scene["camera"])
        if scene["name"].startswith("full_"):
            normal = faces[scene["name"][5:]]
            far = list(scene["camera"])
            for axis in range(3):
                far[axis] += 58 * normal[axis]
            capture(runtime, output, prefix + scene["name"] + "_64", far, settle=5)
    capture(runtime, output, prefix + "grazing_72", [1382.5, 78.88, -1.5, 71.565, 0])
    capture(runtime, output, prefix + "grazing_80", [1386.5, 78.88, -1.2633, 80, 0])
    command(runtime, output, "setblock 1376 80 -2 minecraft:gold_block")
    capture(runtime, output, prefix + "opaque_foreground", [1376.5, 78.88, -5.5, 0, 0])
    command(runtime, output, "setblock 1376 80 -2 minecraft:air")
    command(runtime, output, "setblock 1327 81 0 minecraft:blue_carpet")
    command(runtime, output, "setblock 1328 81 0 minecraft:snow[layers=1]")
    command(runtime, output, "setblock 1329 81 0 minecraft:torch")
    capture(runtime, output, prefix + "thin_grazing", [1328.5, 80.88, -8.5, 0, 10.62])
    for x in [1327, 1328, 1329]:
        command(runtime, output, f"setblock {x} 81 0 minecraft:air")


def animations(runtime, output, fixture, enabled):
    original = list(fixture["scenes"])
    for scene in original:
        if scene["name"] not in ["full_north", "wireless"]:
            continue
        label = ("oit_on_" if enabled else "oit_off_") + scene["name"]
        capture(runtime, output, label + "_stable", scene["camera"])
        power = " ".join(str(value) for value in scene["power"])
        for powered in [False, True]:
            # The probe records scene identity separately from the screenshot's transparency label.
            capture_fade(runtime, output, scene, power, powered)
            suffix = "_opening.png" if powered else "_closing.png"
            (output / (scene["name"] + suffix)).replace(output / (label + suffix))


def verify():
    stamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    runs = []
    m.quota()
    for classic in [False, True]:
        name = stamp + ("_classic" if classic else "_default")
        directory = BASE / "instance" / name / "client"
        output = BASE / "result" / name
        prepare(directory, classic=classic)
        runtime = launch(directory, output)
        ready(runtime, output)
        deadline = time.monotonic() + 120
        while not (output / "fixture.json").exists():
            assert runtime.process.poll() is None and time.monotonic() < deadline
            runtime.call("/v0/control/renew", {"ttlMs": 60000})
            time.sleep(0.5)
        fixture = json.loads((output / "fixture.json").read_text("utf-8"))
        assert fixture["physicalFullCubeChecks"] > 20
        command(runtime, output, "gamemode spectator")
        command(runtime, output, "time set 6000")
        command(runtime, output, "weather clear")
        for enabled in [False, True]:
            transparency(runtime, enabled)
            if not classic:
                stable_scenes(runtime, output, fixture, enabled)
            animations(runtime, output, fixture, enabled)
        if not classic:
            lighting(runtime, output)
        shutdown(runtime, output)
        runs.append(
            {
                "mode": "classic" if classic else "default",
                "directory": str(directory),
                "output": str(output),
                "captures": sorted(p.name for p in output.glob("*.png")),
            }
        )
        m.write_json(
            BASE / "result/candidate.json",
            {"status": "CAPTURED_PENDING_REVIEW", "runs": runs, "reference": m.TARGET},
        )
    print(
        "Candidate captured. Inspect originals and record differences before accepting.",
        flush=True,
    )


def close():
    runtime, output = connect()
    runtime.acquire()
    shutdown(runtime, output)


def acceptance():
    candidate = json.loads((BASE / "result/candidate.json").read_text("utf-8"))
    assert candidate["status"] == "REVIEWED_REFERENCE"
    source = (
        pathlib.Path(
            next(
                run["directory"]
                for run in candidate["runs"]
                if run["mode"] == "default"
            )
        )
        / "saves"
        / WORLD
    )
    stamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    directory = BASE / "instance" / (stamp + "_acceptance") / "client"
    output = BASE / "result" / (stamp + "_acceptance")
    prepare(directory, source=source, human=True)
    skin = m.input_path("customskinloader_acceptance")
    shutil.copyfile(skin, directory / "mods" / skin.name)
    runtime = launch(directory, output, human=True)
    ready(runtime, output, human=True)
    command(runtime, output, "gamemode creative")
    command(runtime, output, "tp @s 1376.5 78.88 -5.5 0 0")
    time.sleep(5)
    runtime.call("/v0/input/release-all", {})
    runtime.call("/v0/control/release", {})
    runtime.lease = None
    mode = runtime.call("/v0/control/mode")
    assert mode["mode"] == "READ" and not mode["takeoverActive"]
    (output / "acceptance.png").write_bytes(runtime.call("/v0/capture", binary=True))
    m.write_json(
        BASE / "result/acceptance.json",
        {
            "status": "READY_FOR_USER",
            "visualAcceptance": "PENDING_USER",
            "directory": str(directory),
            "output": str(output),
            "runtime": runtime.endpoint,
            "preset": CONFIG["profiles"]["acceptance"],
            "musicVolume": 0,
            "controlMode": mode,
            "keepRunning": True,
            "customSkinLoader": skin.name,
            "zume": "SKIPPED_NO_EXACT_26.3_NEOFORGE_MODRINTH_VERSION",
        },
    )
    print(
        "User acceptance running; input released; keep PID " + str(runtime.process.pid),
        flush=True,
    )


if __name__ == "__main__":
    {"verify": verify, "close": close, "acceptance": acceptance}[sys.argv[1]]()
