"""Project-owned installed-loader MCP smoke and human acceptance launcher."""

import concurrent.futures
import datetime
import hashlib
import json
import os
import pathlib
import re
import shutil
import struct
import subprocess
import sys
import time
import tomllib
import urllib.error
import urllib.request
import uuid
import zipfile

from runtime_validation import ROOT, quota

BASE = ROOT / "validations/integration-mcp_alpha"
CONFIG = tomllib.loads((BASE / "validation.toml").read_text(encoding="utf-8"))
TARGET = CONFIG["targets"][0]
MC, LOADER = TARGET.rsplit("-", 1)
HOME = BASE / "instance/installation"
CACHE = BASE / "instance/downloads"
RESULT = BASE / "result"
GRADLE = pathlib.Path.home() / ".gradle/caches"


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(data, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def digest(path, algorithm="sha256"):
    return hashlib.new(algorithm, path.read_bytes()).hexdigest()


def properties(path):
    return dict(
        line.split("=", 1)
        for line in path.read_text(encoding="utf-8").splitlines()
        if "=" in line and not line.startswith("#")
    )


def input_path(name):
    inputs = tomllib.loads((ROOT / "inputs.toml").read_text(encoding="utf-8"))
    local = tomllib.loads((ROOT / "local.toml").read_text(encoding="utf-8"))
    path = pathlib.Path(local["inputs"][name])
    assert digest(path) in inputs[name]["sha256"], name
    return path


def snapshot():
    jar = input_path("mcp_alpha")
    manifest = json.loads((jar.parent / "alpha-manifest.json").read_text("utf-8"))
    assert manifest["packagedRuntimeVerified"] is True
    assert manifest["sourceFingerprint"] == CONFIG["mcp"]["source-fingerprint"]
    assert jar.parent.name == CONFIG["mcp"]["snapshot"]
    for artifact in manifest["artifacts"]:
        assert digest(jar.parent / artifact["fileName"]) == artifact["sha256"]
        if artifact["target"] == TARGET:
            assert artifact["fileName"] == jar.name
    guide = manifest["agentGuide"]
    assert digest(jar.parent / guide["fileName"]) == guide["sha256"]
    return jar


def fetch(url, path, expected=None, alternatives=()):
    path = pathlib.Path(path)
    for candidate in [path, *alternatives]:
        if candidate.is_file() and (
            expected is None or digest(candidate, "sha1") == expected
        ):
            if candidate != path:
                path.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(candidate, path)
            return path
    request = urllib.request.Request(
        url, headers={"User-Agent": "RecRivenVI-GLASS-validation/1.0"}
    )
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=120) as response:
                data = response.read()
            break
        except (OSError, urllib.error.URLError):
            if attempt == 2:
                raise
            time.sleep(attempt + 1)
    if expected:
        assert hashlib.sha1(data).hexdigest() == expected, url
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)
    return path


def get_json(url, path, expected=None, alternatives=()):
    return json.loads(fetch(url, path, expected, alternatives).read_text("utf-8"))


def java():
    facts = properties(ROOT / "versions" / TARGET / "target.properties")
    for candidate in (pathlib.Path.home() / ".gradle/jdks").glob("*/bin/java.exe"):
        release = (candidate.parents[1] / "release").read_text("utf-8")
        if re.search('JAVA_VERSION="' + facts["java_version"] + r"[.\"]", release):
            return candidate
    raise RuntimeError("Required game Java not found")


def allowed(rules):
    if rules is None:
        return True
    result = False
    for rule in rules:
        system = rule.get("os", {})
        if system.get("name", "windows") != "windows":
            continue
        if system.get("arch", "x86_64") not in ("x86_64", "amd64", "x64"):
            continue
        if any(rule.get("features", {}).values()):
            continue
        result = rule["action"] == "allow"
    return result


def install():
    snapshot()
    quota()
    RESULT.mkdir(parents=True, exist_ok=True)
    facts = properties(ROOT / "versions" / TARGET / "target.properties")
    manifest = get_json(
        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json",
        CACHE / "version_manifest_v2.json",
    )
    version = next(v for v in manifest["versions"] if v["id"] == MC)
    vanilla = get_json(
        version["url"],
        HOME / "versions" / MC / (MC + ".json"),
        version["sha1"],
        [GRADLE / "neoformruntime/artifacts" / f"minecraft_{MC}_version_manifest.json"],
    )
    artifact = vanilla["downloads"]["client"]
    client_jar = fetch(
        artifact["url"],
        HOME / "versions" / MC / (MC + ".jar"),
        artifact["sha1"],
        [GRADLE / "neoformruntime/artifacts" / f"minecraft_{MC}_client.jar"],
    )
    assert LOADER == "neoforge"
    loader_version = facts["loader_version"]
    profile_id = "neoforge-" + loader_version
    profile_path = HOME / "versions" / profile_id / (profile_id + ".json")
    if not (HOME / "installed_ok.json").exists():
        url = (
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/"
            + loader_version
            + "/neoforge-"
            + loader_version
            + "-installer.jar"
        )
        with urllib.request.urlopen(url + ".sha1", timeout=60) as response:
            checksum = response.read().decode().strip().split()[0]
        installer = fetch(
            url, CACHE / ("neoforge-" + loader_version + "-installer.jar"), checksum
        )
        write_json(HOME / "launcher_profiles.json", {"profiles": {}, "settings": {}})
        print("Installing official NeoForge " + loader_version, flush=True)
        with (RESULT / "installer.log").open("w", encoding="utf-8") as log:
            subprocess.run(
                [
                    str(java()),
                    "-Djava.awt.headless=true",
                    "-jar",
                    str(installer),
                    "--installClient",
                    str(HOME),
                ],
                cwd=HOME,
                stdout=log,
                stderr=subprocess.STDOUT,
                timeout=600,
                check=True,
            )
        write_json(HOME / "installed_ok.json", {"loaderVersion": loader_version})
    profile = json.loads(profile_path.read_text("utf-8"))
    libraries = {}
    for lib in vanilla["libraries"] + profile["libraries"]:
        if allowed(lib.get("rules")):
            parts = lib["name"].split(":")
            libraries[(parts[0], parts[1], *parts[3:])] = lib

    def library(lib):
        parts = lib["name"].split(":")
        relative = "/".join(
            [
                parts[0].replace(".", "/"),
                parts[1],
                parts[2],
                "-".join(parts[1:3] + parts[3:]) + ".jar",
            ]
        )
        item = lib.get("downloads", {}).get(
            "artifact",
            {
                "path": relative,
                "url": lib.get("url", "https://libraries.minecraft.net/") + relative,
            },
        )
        cached = list(
            (GRADLE / "modules-2/files-2.1" / parts[0] / parts[1] / parts[2]).glob(
                "*/" + pathlib.PurePosixPath(item["path"]).name
            )
        )
        path = fetch(
            item["url"], HOME / "libraries" / item["path"], item.get("sha1"), cached
        )
        return path

    with concurrent.futures.ThreadPoolExecutor(max_workers=12) as pool:
        classpath = sorted(pool.map(library, libraries.values()))
    natives = HOME / "natives"
    natives.mkdir(parents=True, exist_ok=True)
    for path in classpath:
        if "natives-windows" in path.name:
            with zipfile.ZipFile(path) as archive:
                for name in archive.namelist():
                    if name.lower().endswith(".dll"):
                        (natives / pathlib.PurePosixPath(name).name).write_bytes(
                            archive.read(name)
                        )
    index = vanilla["assetIndex"]
    assets = CACHE / "assets"
    asset_index = get_json(
        index["url"],
        assets / "indexes" / (index["id"] + ".json"),
        index["sha1"],
        [GRADLE / "neoformruntime/assets/indexes" / (index["id"] + ".json")],
    )

    def asset(sha):
        suffix = sha[:2] + "/" + sha
        return fetch(
            "https://resources.download.minecraft.net/" + suffix,
            assets / "objects" / suffix,
            sha,
            [
                GRADLE / "neoformruntime/assets/objects" / suffix,
                GRADLE / "fabric-loom/assets/objects" / suffix,
            ],
        )

    print("Preparing verified official assets", flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=24) as pool:
        list(pool.map(asset, {v["hash"] for v in asset_index["objects"].values()}))
    installed_client = profile_path.with_suffix(".jar")
    shutil.copyfile(client_jar, installed_client)
    classpath.append(installed_client)
    assert all(p.is_relative_to(HOME) for p in classpath)
    write_json(
        CACHE / "launch.json",
        {
            "vanilla": vanilla,
            "profile": profile,
            "classpath": [str(p) for p in classpath],
            "assets": str(assets),
            "index": index["id"],
            "loaderVersion": loader_version,
        },
    )
    quota()
    print("Official installed runtime prepared", flush=True)


def prepare(role):
    jar = snapshot()
    directory = BASE / "instance" / TARGET / role
    if directory.exists():
        previous = json.loads((RESULT / (role + ".json")).read_text("utf-8"))
        assert previous["status"] == "PASS" and previous["cleanShutdown"] is True
        assert not (directory / "minecraft-protocol/endpoint.json").exists()
        stamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        archive = directory.with_name(role + "_attempt_" + stamp)
        assert directory.resolve().parent == (BASE / "instance" / TARGET).resolve()
        assert (
            archive.resolve().parent == directory.resolve().parent
            and not archive.exists()
        )
        shutil.move(str(directory), str(archive))
        saved = RESULT / ("attempt_" + role + "_" + stamp)
        saved.mkdir()
        for item in list(RESULT.iterdir()):
            if item.is_file() and (
                item.name.startswith(role)
                or item.name
                in [
                    "world-selection.json",
                    "entry-dialog.json",
                    "player-current.json",
                    "pause-menu.json",
                ]
            ):
                shutil.move(str(item), str(saved / item.name))
    mods = directory / "mods"
    mods.mkdir(parents=True)
    shutil.copyfile(jar, mods / jar.name)
    if role != "minimal":
        product = properties(ROOT / "gradle.properties")
        name = f"{product['mod_id']}-{TARGET}-{product['mod_version']}.jar"
        release = ROOT / "build/release" / product["mod_version"] / name
        verification = json.loads(
            (
                ROOT
                / "validations/migration-template_layout/result/build_verification.json"
            ).read_text("utf-8")
        )
        assert verification["status"] == "PASS"
        expected = next(
            item for item in verification["artifacts"] if item["name"] == name
        )
        assert digest(release) == expected["sha256"]
        shutil.copyfile(release, mods / name)
        if role == "reference":
            fixture = (
                ROOT
                / "validations/smoke-reference_projection/result/reference_world.zip"
            )
            destination = directory / "saves/GLASS MCP fixture"
            destination.mkdir(parents=True)
            with zipfile.ZipFile(fixture) as archive:
                for entry in archive.infolist():
                    assert (
                        (destination / entry.filename)
                        .resolve()
                        .is_relative_to(destination.resolve())
                    )
                archive.extractall(destination)
        else:
            source = BASE / "instance" / TARGET / "reference/saves/GLASS MCP fixture"
            destination = directory / "saves/GLASS MCP fixture"
            shutil.copytree(source, destination)
    if role == "acceptance":
        skin = input_path("customskinloader_acceptance")
        shutil.copyfile(skin, mods / skin.name)
    preset = CONFIG["profiles"]["acceptance" if role == "acceptance" else "automatic"]
    (directory / "options.txt").write_text(
        "lang:en_us\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\n"
        + f"soundCategory_master:{preset['master-volume']}\nsoundCategory_music:0.0\n"
        + "renderDistance:6\nsimulationDistance:5\n",
        encoding="utf-8",
        newline="\n",
    )
    return directory


def launch(role, directory, *, world=None, result_directory=None, extra_scopes=()):
    data = json.loads((CACHE / "launch.json").read_text("utf-8"))
    profile, vanilla = data["profile"], data["vanilla"]
    preset = CONFIG["profiles"]["acceptance" if role == "acceptance" else "automatic"]
    values = {
        "auth_player_name": preset["player-name"],
        "version_name": profile["id"],
        "game_directory": str(directory),
        "assets_root": data["assets"],
        "assets_index_name": data["index"],
        "auth_uuid": uuid.uuid3(
            uuid.NAMESPACE_DNS, "OfflinePlayer:" + preset["player-name"]
        ).hex,
        "auth_access_token": "0",
        "clientid": "",
        "auth_xuid": "0",
        "user_type": "legacy",
        "version_type": "release",
        "natives_directory": str(HOME / "natives"),
        "launcher_name": "GLASS-owned-validation",
        "launcher_version": "1",
        "classpath": os.pathsep.join(data["classpath"]),
        "library_directory": str(HOME / "libraries"),
        "classpath_separator": os.pathsep,
    }

    def expand(value):
        return re.sub(r"\$\{([^}]+)\}", lambda m: values[m[1]], value)

    def arguments(items):
        out = []
        for item in items:
            if isinstance(item, str):
                out.append(expand(item))
            elif allowed(item.get("rules")):
                value = item["value"]
                out.extend(
                    expand(v) for v in (value if isinstance(value, list) else [value])
                )
        return out

    args = [str(java()), "-Xms512M", "-Xmx4G", "-Dminecraft.protocol.probe.port=0"]
    args += arguments(vanilla["arguments"]["jvm"] + profile["arguments"].get("jvm", []))
    args += [profile["mainClass"]]
    args += arguments(
        vanilla["arguments"]["game"] + profile["arguments"].get("game", [])
    )
    args += [
        "--width",
        str(preset["width"]),
        "--height",
        str(preset["height"]),
        "--graphicsBackend",
        "VULKAN",
    ]
    if world is not None or role == "acceptance":
        args += ["--quickPlaySingleplayer", world or "GLASS MCP fixture"]
    env = os.environ.copy()
    for name in list(env):
        if name.startswith("MCP_") or name == "MOD_CLASSES":
            env.pop(name)
    env["MCP_RUNTIME_SCOPES"] = ",".join(
        ["read", "ui", "input", "control", "capture", "diagnostics", *extra_scopes]
    )
    result_directory = result_directory or RESULT
    log = (result_directory / (role + "-console.log")).open("w", encoding="utf-8")
    process = subprocess.Popen(
        args,
        cwd=directory,
        stdout=log,
        stderr=subprocess.STDOUT,
        env=env,
        creationflags=subprocess.CREATE_NO_WINDOW,
    )
    log.close()
    write_json(
        result_directory / (role + "-process.json"),
        {
            "pid": process.pid,
            "directory": str(directory),
            "startedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "preset": preset,
        },
    )
    return process


class Runtime:
    def __init__(self, directory, process):
        self.directory, self.process, self.lease = directory, process, None
        self.endpoint_path = directory / "minecraft-protocol/endpoint.json"
        self.endpoint = json.loads(self.endpoint_path.read_text("utf-8"))
        record = self.endpoint
        assert record["schemaVersion"] == 1 and record["state"] == "RUNNING"
        assert (
            record["target"] == TARGET
            and pathlib.Path(record["gameDirectory"]).resolve() == directory.resolve()
        )
        assert record["pid"] == process.pid and process.poll() is None
        assert record["baseUrl"] == "http://127.0.0.1:" + str(record["port"])
        assert record["tokenFile"] == "token"
        identity = subprocess.check_output(
            [
                "powershell",
                "-NoProfile",
                "-Command",
                f"(Get-Process -Id {process.pid}).StartTime.ToUniversalTime().ToString('o')",
            ],
            text=True,
        ).strip()
        actual = datetime.datetime.fromisoformat(identity.replace("Z", "+00:00"))
        recorded = datetime.datetime.fromisoformat(
            record["processStartedAt"].replace("Z", "+00:00")
        )
        assert abs((actual - recorded).total_seconds()) < 1
        self.headers = {
            "Authorization": "Bearer "
            + (directory / "minecraft-protocol/token").read_text("utf-8").strip(),
            "X-MCP-Runtime-Instance": record["runtimeInstanceId"],
        }
        session = self.call("/v0/session")
        assert session["target"] == TARGET
        assert (
            session["runtimeConnection"]["runtimeInstanceId"]
            == record["runtimeInstanceId"]
        )

    def call(self, path, body=None, binary=False):
        record = json.loads(self.endpoint_path.read_text("utf-8"))
        assert record == self.endpoint and self.process.poll() is None
        headers = {**self.headers, "Content-Type": "application/json"}
        if self.lease:
            headers["X-MCP-Control-Lease"] = self.lease
        request = urllib.request.Request(
            self.endpoint["baseUrl"] + path,
            data=None if body is None else json.dumps(body).encode(),
            headers=headers,
        )
        try:
            with urllib.request.urlopen(request, timeout=65) as response:
                assert (
                    response.headers.get("X-MCP-Runtime-Instance")
                    == self.endpoint["runtimeInstanceId"]
                )
                data = response.read()
        except urllib.error.HTTPError as error:
            raise RuntimeError(
                f"MCP {path}: {error.code} {error.read().decode()}"
            ) from None
        return data if binary else json.loads(data)

    def acquire(self):
        state = self.call("/v0/control/mode")
        assert state["reconsentRequired"] is False, (
            "Human revoked control; do not reacquire automatically"
        )
        lease = self.call(
            "/v0/control/acquire",
            {"ttlMs": 60000, "expectedModeVersion": state["modeVersion"]},
        )
        self.lease = lease["leaseId"]

    def pipeline(self, steps):
        self.call("/v0/control/renew", {"ttlMs": 60000})
        operation = self.call("/v0/pipelines", {"steps": steps, "timeoutMs": 30000})
        identifier = operation.get("operationId", operation.get("id"))
        result = self.call(f"/v0/operations/{identifier}/wait", {"timeoutMs": 30000})
        assert result["state"] == "completed", result
        return result

    def click(self, selector):
        self.call("/v0/control/renew", {"ttlMs": 60000})
        return self.call("/v0/ui/action", {"selector": selector, "action": "click"})

    def title(self):
        result = self.call(
            "/v0/wait/until",
            {
                "condition": {
                    "type": "screen",
                    "category": "USER",
                    "classContains": "TitleScreen",
                    "ready": True,
                    "selector": {"translationKey": "menu.singleplayer"},
                },
                "timeoutMs": 60000,
            },
        )
        assert result["passed"], result
        tree = self.call("/v0/ui/tree")
        assert (tree["windowWidth"], tree["windowHeight"]) == (1920, 1080)
        return tree


def walk(node):
    yield node
    for child in node.get("children", []):
        yield from walk(child)


class ExistingProcess:
    """Observe only the previously launched, endpoint-bound Windows process."""

    def __init__(self, pid):
        import ctypes
        from ctypes import wintypes

        self.pid = pid
        self.api = ctypes.WinDLL("kernel32", use_last_error=True)
        self.api.OpenProcess.restype = wintypes.HANDLE
        self.api.OpenProcess.argtypes = [wintypes.DWORD, wintypes.BOOL, wintypes.DWORD]
        self.api.GetExitCodeProcess.argtypes = [
            wintypes.HANDLE,
            ctypes.POINTER(wintypes.DWORD),
        ]
        self.handle = self.api.OpenProcess(0x1000 | 0x100000, False, pid)
        assert self.handle, "Owned process is unavailable"

    def poll(self):
        import ctypes
        from ctypes import wintypes

        value = wintypes.DWORD()
        assert self.api.GetExitCodeProcess(self.handle, ctypes.byref(value))
        return None if value.value == 259 else value.value

    def wait(self, timeout):
        deadline = time.monotonic() + timeout
        while self.poll() is None:
            assert time.monotonic() < deadline, "Owned process exit deadline"
            time.sleep(0.2)
        return self.poll()


def reference_journey(runtime, report):
    deadline = time.monotonic() + 90
    heartbeat = time.monotonic()
    while True:
        tree = runtime.call("/v0/ui/tree")
        if tree.get("screenClass", "").endswith("BackupConfirmScreen"):
            write_json(RESULT / "entry-dialog.json", tree)
            runtime.click({"translationKey": "selectWorld.backupJoinConfirmButton"})
            report["fixtureBackupConfirmed"] = True
        player = runtime.call("/v0/player?category=USER")
        write_json(RESULT / "player-current.json", player)
        if (
            player.get("available")
            and not tree.get("screenClass")
            and not tree.get("overlayIdentity")
        ):
            break
        assert time.monotonic() < deadline, "World entry deadline"
        if time.monotonic() - heartbeat > 15:
            runtime.call("/v0/control/renew", {"ttlMs": 60000})
            heartbeat = time.monotonic()
        time.sleep(0.5)
    runtime.pipeline([{"type": "key.tap", "key": 256, "holdMs": 40}])
    runtime.click({"translationKey": "menu.returnToGame"})
    report["playerBefore"] = player
    report["movement"] = runtime.pipeline(
        [
            {"type": "key.tap", "key": 87, "holdMs": 700},
            {"type": "mouse.delta", "dx": 36, "dy": -12},
        ]
    )
    deadline = time.monotonic() + 3
    while True:
        after = runtime.call("/v0/player?category=USER")
        if abs(after["yaw"] - player["yaw"]) > 0.1 or time.monotonic() > deadline:
            break
        time.sleep(0.1)
    report["playerAfter"] = after
    distance = sum((after[key] - player[key]) ** 2 for key in ("x", "z")) ** 0.5
    assert distance > 0.05, ("Player did not move", player, after)
    assert abs(after["yaw"] - player["yaw"]) > 0.1
    report["horizontalDistance"] = distance
    report["cameraConfirmed"] = True
    reference_capture_and_save(runtime, report)


def reference_capture_and_save(runtime, report):
    image = runtime.call("/v0/capture", binary=True)
    assert image[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", image[16:24]) == (
        1920,
        1080,
    )
    (RESULT / "reference.png").write_bytes(image)
    report["screenshotSha256"] = hashlib.sha256(image).hexdigest()
    runtime.call("/v0/input/release-all", {})
    report["inputReleased"] = runtime.call(
        "/v0/wait/until", {"condition": {"type": "input.released"}, "timeoutMs": 5000}
    )
    assert report["inputReleased"]["passed"]
    runtime.pipeline([{"type": "key.tap", "key": 256, "holdMs": 40}])
    write_json(RESULT / "pause-menu.json", runtime.call("/v0/ui/tree"))
    runtime.click({"translationKey": "menu.returnToMenu"})
    runtime.title()
    report["savedAndReturnedToTitle"] = True
    runtime.call("/v0/control/release", {})
    runtime.lease = None
    assert runtime.call("/v0/control/mode")["mode"] == "READ"
    runtime.acquire()


def run(role):
    quota()
    if role == "reference":
        assert (
            json.loads((RESULT / "minimal.json").read_text("utf-8"))["status"] == "PASS"
        )
    if role == "acceptance":
        assert (
            json.loads((RESULT / "reference.json").read_text("utf-8"))["status"]
            == "PASS"
        )
    directory = prepare(role)
    process = launch(role, directory)
    if role == "acceptance":
        hand_off_acceptance(directory, process)
        return
    report = {
        "status": "RUNNING",
        "target": TARGET,
        "snapshot": CONFIG["mcp"]["snapshot"],
        "mcpSha256": digest(snapshot()),
        "runtime": "official_installed_loader",
        "role": role,
    }
    runtime = None
    try:
        deadline = time.monotonic() + 180
        endpoint = directory / "minecraft-protocol/endpoint.json"
        while not endpoint.exists():
            assert process.poll() is None, "Client exited; inspect own console log"
            assert time.monotonic() < deadline, "MCP endpoint deadline"
            time.sleep(0.3)
        runtime = Runtime(directory, process)
        report["endpoint"] = runtime.endpoint
        report["capabilities"] = runtime.call("/v0/capabilities")
        report["read"] = runtime.call("/v0/player?category=USER")
        report["title"] = runtime.title()
        report["readPassed"] = True
        write_json(RESULT / (role + ".json"), report)
        print(role + ": endpoint, identity, READ and title PASS", flush=True)
        runtime.acquire()
        if role == "reference":
            runtime.click({"translationKey": "menu.singleplayer"})
            tree = runtime.call("/v0/ui/tree")
            write_json(RESULT / "world-selection.json", tree)
            entries = [
                n
                for n in walk(tree)
                if n.get("role") in ("world_entry", "world_list_entry")
            ]
            if not entries:
                entries = [
                    n
                    for n in walk(tree)
                    if "GLASS" in str(n.get("label", ""))
                    and "click" in n.get("actions", [])
                ]
            assert len(entries) == 1, (
                "Inspect world-selection.json for the native world entry"
            )
            runtime.click({"nodeId": entries[0]["nodeId"]})
            runtime.click({"translationKey": "selectWorld.select"})
            reference_journey(runtime, report)
        try:
            runtime.click({"translationKey": "menu.quit"})
        except (RuntimeError, OSError) as error:
            report["quitTransportResult"] = str(error)
        code = process.wait(timeout=60)
        assert code == 0 and not endpoint.exists()
        text = (directory / "logs/latest.log").read_text("utf-8")
        assert "Stopping!" in text and not (directory / "crash-reports").exists()
        assert (
            "Failed to render GLASS" not in text
            and "Couldn't compile pipeline (glass:" not in text
        )
        report["exitCode"], report["cleanShutdown"], report["status"] = (
            code,
            True,
            "PASS",
        )
        shutil.copyfile(directory / "logs/latest.log", RESULT / (role + "-latest.log"))
        print(role + ": PASS / normal shutdown", flush=True)
    except BaseException as error:
        report["status"], report["error"] = "FAIL", str(error)
        raise
    finally:
        write_json(RESULT / (role + ".json"), report)
        if process.poll() is None:
            print(
                "Owned client retained for diagnosis: " + str(process.pid), flush=True
            )
        quota()


def hand_off_acceptance(directory, process):
    deadline = time.monotonic() + 180
    endpoint = directory / "minecraft-protocol/endpoint.json"
    while not endpoint.exists():
        assert process.poll() is None, "Acceptance client exited"
        assert time.monotonic() < deadline, "Acceptance endpoint deadline"
        time.sleep(0.3)
    runtime = Runtime(directory, process)
    while True:
        tree = runtime.call("/v0/ui/tree")
        if tree.get("screenClass", "").endswith("BackupConfirmScreen"):
            if runtime.lease is None:
                runtime.acquire()
            runtime.click({"translationKey": "selectWorld.backupJoinConfirmButton"})
        player = runtime.call("/v0/player?category=USER")
        if (
            player.get("available")
            and not tree.get("screenClass")
            and not tree.get("overlayIdentity")
        ):
            break
        assert process.poll() is None and time.monotonic() < deadline
        if runtime.lease:
            runtime.call("/v0/control/renew", {"ttlMs": 60000})
        time.sleep(0.5)
    assert (tree["windowWidth"], tree["windowHeight"]) == (2560, 1440)
    if runtime.lease is None:
        runtime.acquire()
    runtime.pipeline([{"type": "key.tap", "key": 256, "holdMs": 40}])
    runtime.click({"translationKey": "menu.returnToGame"})
    runtime.pipeline([{"type": "key.tap", "key": 83, "holdMs": 700}])
    runtime.call("/v0/input/release-all", {})
    released = runtime.call(
        "/v0/wait/until", {"condition": {"type": "input.released"}, "timeoutMs": 5000}
    )
    assert released["passed"]
    runtime.call("/v0/control/release", {})
    runtime.lease = None
    mode = runtime.call("/v0/control/mode")
    assert mode["mode"] == "READ" and mode["takeoverActive"] is False
    options = properties_options(directory / "options.txt")
    assert float(options["soundCategory_master"]) == 0.5
    assert float(options["soundCategory_music"]) == 0
    image = runtime.call("/v0/capture", binary=True)
    assert struct.unpack(">II", image[16:24]) == (2560, 1440)
    (RESULT / "acceptance.png").write_bytes(image)
    write_json(
        RESULT / "acceptance.json",
        {
            "status": "READY_FOR_USER",
            "visualAcceptance": "PENDING_USER",
            "pid": process.pid,
            "endpoint": runtime.endpoint,
            "controlMode": mode,
            "inputReleased": released,
            "preset": CONFIG["profiles"]["acceptance"],
            "musicVolume": 0,
            "mods": [
                {"name": p.name, "sha256": digest(p)}
                for p in sorted((directory / "mods").glob("*.jar"))
            ],
            "zume": "SKIPPED_NO_EXACT_26.3_NEOFORGE_MODRINTH_VERSION",
        },
    )
    quota()
    print(
        "Acceptance ready for user; READ mode / no held input / PID "
        + str(process.pid),
        flush=True,
    )


def properties_options(path):
    return dict(
        line.split(":", 1)
        for line in path.read_text("utf-8").splitlines()
        if ":" in line
    )


def verify():
    report = {"status": "RUNNING", "commands": []}
    try:
        for name, tasks in [
            ("format", ["spotlessApply"]),
            ("repository", ["check", "verifyRelease"]),
        ]:
            with (RESULT / (name + "-gates.log")).open("w", encoding="utf-8") as log:
                completed = subprocess.run(
                    [
                        "cmd.exe",
                        "/d",
                        "/c",
                        str(ROOT / "gradlew.bat"),
                        *tasks,
                        "--no-configuration-cache",
                        "--console=plain",
                    ],
                    cwd=ROOT,
                    stdout=log,
                    stderr=subprocess.STDOUT,
                )
            report["commands"].append(
                {"tasks": tasks, "exitCode": completed.returncode}
            )
            assert completed.returncode == 0, "Read " + name + "-gates.log"
        report["status"] = "PASS"
    except BaseException as error:
        report.update(status="FAIL", error=str(error))
        raise
    finally:
        write_json(RESULT / "repository_gates.json", report)
    print("MCP integration: spotlessApply / check / verifyRelease PASS", flush=True)


if __name__ == "__main__":
    {
        "install": install,
        "minimal": lambda: run("minimal"),
        "reference": lambda: run("reference"),
        "acceptance": lambda: run("acceptance"),
        "verify": verify,
    }[sys.argv[1]]()
