"""Drive GLASS native widgets through the pinned MCP runtime, without DEBUG or host input."""

import datetime
import json
import pathlib
import shutil
import sys
import time
import tomllib

import mcp_alpha as m

BASE = m.ROOT / "validations/conformance-native_gui"
CONFIG = tomllib.loads((BASE / "validation.toml").read_text("utf-8"))
WORLD = CONFIG["fixture"]["world"]


def product():
    facts = m.properties(m.ROOT / "gradle.properties")
    name = f"{facts['mod_id']}-{m.TARGET}-{facts['mod_version']}.jar"
    release = m.ROOT / "build/release" / facts["mod_version"] / name
    candidate = m.ROOT / "versions" / m.TARGET / "build/libs" / name
    assert release.is_file() and m.digest(release) == m.digest(candidate)
    return release


def prepare(source, directory, human=False):
    assert not (source / "minecraft-protocol/endpoint.json").exists(), (
        "Save and close source first"
    )
    directory.mkdir(parents=True)
    shutil.copytree(source / "saves" / WORLD, directory / "saves" / WORLD)
    if (source / "config/glass.json").exists():
        (directory / "config").mkdir()
        shutil.copyfile(source / "config/glass.json", directory / "config/glass.json")
    mods = directory / "mods"
    mods.mkdir()
    for jar in [m.snapshot(), product()]:
        shutil.copyfile(jar, mods / jar.name)
    if human:
        jar = m.input_path("customskinloader_acceptance")
        shutil.copyfile(jar, mods / jar.name)
    preset = m.CONFIG["profiles"]["acceptance" if human else "automatic"]
    (directory / "options.txt").write_text(
        "lang:en_us\nonboardAccessibility:false\npauseOnLostFocus:false\nautoJump:false\n"
        "tutorialStep:none\nrenderDistance:6\nsimulationDistance:5\n"
        f"soundCategory_master:{preset['master-volume']}\nsoundCategory_music:0.0\n",
        encoding="utf-8",
        newline="\n",
    )


class GuiDriver:
    def __init__(self, directory, process, output):
        deadline = time.monotonic() + 180
        while not (directory / "minecraft-protocol/endpoint.json").exists():
            assert process.poll() is None and time.monotonic() < deadline
            time.sleep(0.4)
        self.runtime = m.Runtime(directory, process)
        self.output = output
        self.last_pipeline = 0
        self.checks = []
        self.runtime.acquire()
        while True:
            tree = self.tree()
            player = self.runtime.call("/v0/player?category=USER")
            if tree.get("screenClass", "").endswith("BackupConfirmScreen"):
                self.click(key="selectWorld.backupJoinConfirmButton")
            if (
                player.get("available")
                and not tree.get("screenClass")
                and not tree.get("overlayIdentity")
            ):
                break
            assert process.poll() is None and time.monotonic() < deadline
            self.runtime.call("/v0/control/renew", {"ttlMs": 60000})
            time.sleep(0.4)
        self.key(256)
        self.click(key="menu.returnToGame")

    def tree(self):
        return self.runtime.call("/v0/ui/tree")

    def nodes(self):
        return list(m.walk(self.tree()))

    def find(self, *, role=None, prefix=None, key=None, class_end=None):
        nodes = [
            n
            for n in self.nodes()
            if (role is None or n.get("role") == role)
            and (prefix is None or n.get("label", "").startswith(prefix))
            and (key is None or n.get("translationKey") == key)
            and (class_end is None or n.get("class", "").endswith(class_end))
        ]
        assert len(nodes) == 1, (role, prefix, key, class_end, nodes)
        return nodes[0]

    def pipeline(self, steps):
        remaining = 0.6 - (time.monotonic() - self.last_pipeline)
        if remaining > 0:
            time.sleep(remaining)
        result = self.runtime.pipeline(steps)
        self.last_pipeline = time.monotonic()
        return result

    def key(self, key):
        return self.pipeline([{"type": "key.tap", "key": key, "holdMs": 40}])

    def action(self, node, action="click", **values):
        self.runtime.call("/v0/control/renew", {"ttlMs": 60000})
        result = self.runtime.call(
            "/v0/ui/action",
            {"selector": {"nodeId": node["nodeId"]}, "action": action, **values},
        )
        with (self.output / "actions.jsonl").open("a", encoding="utf-8") as stream:
            stream.write(json.dumps(result, ensure_ascii=False) + "\n")
        assert not result.get("directBusinessCallUsed") and not result.get(
            "directMutationUsed"
        ), result
        time.sleep(0.2)
        return result

    def click(self, **selector):
        return self.action(self.find(**selector))

    def capture(self, name):
        m.write_json(self.output / (name + ".json"), self.tree())
        (self.output / (name + ".png")).write_bytes(
            self.runtime.call("/v0/capture", binary=True)
        )

    def passed(self, name):
        self.checks.append(name)
        print("PASS " + name, flush=True)

    def game(self):
        for _ in range(5):
            if not self.tree().get("screenClass"):
                return
            self.key(256)
        raise AssertionError("Could not return to game")

    def command(self, text):
        result = self.runtime.call(
            "/v0/command/player", {"command": text, "route": "NATIVE"}
        )
        assert result["accepted"]
        with (self.output / "commands.jsonl").open("a", encoding="utf-8") as stream:
            stream.write(json.dumps({"command": text, "result": result}) + "\n")

    def open_block(self, pos):
        self.game()
        x, y, z = pos
        self.command(f"tp @s {x + 0.5} {float(max(65, y - 1))} {float(z + 3)} 180 10")
        deadline = time.monotonic() + 10
        while True:
            player = self.runtime.call("/v0/player?category=USER")
            if abs(player["x"] - x - 0.5) < 0.3 and abs(player["z"] - z - 3) < 0.3:
                break
            assert time.monotonic() < deadline, player
            time.sleep(0.1)
        self.key(57)
        self.use_block(pos)

    def use_block(self, pos):
        x, y, z = pos
        result = self.runtime.call(
            "/v0/wait/until",
            {
                "condition": {
                    "type": "block",
                    "category": "USER",
                    "x": x,
                    "y": y,
                    "z": z,
                    "available": True,
                },
                "timeoutMs": 15000,
            },
        )
        assert result["passed"]
        block = self.runtime.call(f"/v0/world/block?x={x}&y={y}&z={z}")
        self.pipeline(
            [{"type": "world.action", "action": "use", "targetRef": block["targetRef"]}]
        )
        deadline = time.monotonic() + 5
        while not self.tree().get("screenClass"):
            assert time.monotonic() < deadline
            time.sleep(0.1)

    def selected(self):
        return [
            n
            for n in self.nodes()
            if n.get("role") == "list_item" and n.get("selected")
        ]

    def row(self, text):
        for direction in (265, 264):
            for _ in range(40):
                nodes = self.nodes()
                found = [
                    n
                    for n in nodes
                    if n.get("role") == "list_item" and text in n.get("label", "")
                ]
                if len(found) == 1:
                    return found[0]
                if not self.find(role="list")["focused"]:
                    self.action(next(n for n in nodes if n.get("role") == "list_item"))
                self.key(direction)
                if not self.find(role="list")["focused"]:
                    break
        raise AssertionError("Visible native entry not found: " + text)

    def all_rows(self):
        rows = {}
        for _ in range(300):
            for node in self.nodes():
                if node.get("role") == "list_item":
                    rows[node["entryIndex"]] = node["label"]
            before = self.selected()
            self.key(264)
            after = self.selected()
            if not self.find(role="list")["focused"]:
                return [rows[index] for index in sorted(rows)]
            if before and after and before[0]["entryIndex"] == after[0]["entryIndex"]:
                return [rows[index] for index in sorted(rows)]
        raise AssertionError("Native list navigation did not reach its end")

    def config(self):
        self.game()
        self.key(256)
        self.click(key="fml.menu.mods")
        self.action(
            next(
                n
                for n in self.nodes()
                if n.get("role") == "list_item" and "G.L.A.S.S." in n.get("label", "")
            )
        )
        self.click(key="neoforge.screen.mods.button.config")
        assert self.tree()["screenClass"].endswith("GlassConfigScreen")

    def reopen_config(self):
        self.click(key="gui.done")
        self.click(key="neoforge.screen.mods.button.config")

    def config_boolean(self):
        for _ in range(30):
            found = [
                n for n in self.nodes() if n.get("class", "").endswith(".CycleButton")
            ]
            if found:
                assert len(found) == 1
                return found[0]
            self.key(258)
        raise AssertionError("Native boolean setting was not reachable through Tab")

    def release(self):
        r = self.runtime
        r.call("/v0/input/release-all", {})
        assert r.call(
            "/v0/wait/until",
            {"condition": {"type": "input.released"}, "timeoutMs": 5000},
        )["passed"]
        r.call("/v0/control/release", {})
        r.lease = None
        result = r.call("/v0/control/mode")
        assert result["mode"] == "READ"
        return result

    def close(self):
        self.game()
        self.runtime.call("/v0/input/release-all", {})
        self.key(256)
        self.click(key="menu.returnToMenu")
        assert self.runtime.call(
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
        )["passed"]
        try:
            self.click(key="menu.quit")
        except (RuntimeError, OSError):
            # Shutdown may close HTTP before the quit response; confirm the independent process result.
            pass
        assert self.runtime.process.wait(60) == 0
        assert not self.runtime.endpoint_path.exists()


def exercise(driver):
    d = driver
    d.open_block((48, 66, 24))
    original_name = d.find(role="text_field")["value"]
    original_visibility = d.find(class_end=".CycleButton")["translationKey"]
    d.key(258)
    assert not d.find(role="text_field")["focused"]
    assert d.find(class_end=".CycleButton")["translationKey"] == original_visibility
    d.action(
        d.find(role="text_field"),
        "type_text",
        text=original_name + " MCP",
        replace=True,
    )
    d.click(class_end=".CycleButton")
    changed_visibility = d.find(class_end=".CycleButton")["translationKey"]
    assert changed_visibility != original_visibility
    d.click(key="glass.gui.confirm")
    d.open_block((48, 66, 24))
    assert d.find(role="text_field")["value"] == original_name + " MCP"
    assert d.find(class_end=".CycleButton")["translationKey"] == changed_visibility
    d.capture("terminal_saved")
    d.action(d.find(role="text_field"), "type_text", text=original_name, replace=True)
    d.click(class_end=".CycleButton")
    d.click(key="glass.gui.confirm")
    d.passed("terminal text, visibility, Tab focus and save/reopen")

    d.open_block((77, 66, 69))
    original_channel = d.selected()[0]["label"]
    target = (
        original_name
        if not original_channel.startswith(original_name + ".")
        else "02 Prism Workshop"
    )
    d.action(d.row(target + "."))
    assert d.selected()[0]["label"].startswith(target + ".")
    d.key(258)
    assert not d.find(role="list")["focused"]
    before = d.selected()[0]["label"]
    d.key(264)
    assert d.selected()[0]["label"] == before
    d.click(key="glass.gui.confirm")
    d.open_block((77, 66, 69))
    assert d.selected()[0]["label"].startswith(target + ".")
    d.capture("channel_saved")
    d.action(d.row(original_channel.split(". ", 1)[0] + "."))
    d.key(257)
    assert not d.tree().get("screenClass")
    d.open_block((77, 66, 69))
    assert d.selected()[0]["label"] == original_channel
    d.passed(
        "named channel selection, focus isolation, keyboard activation and persistence"
    )

    d.open_block((90, 66, 54))
    d.click(key="glass.gui.manage_wireless")
    d.action(d.row("93, 65, 54"))
    d.click(key="glass.gui.move_down")
    assert d.selected()[0]["entryIndex"] == 1
    d.click(key="glass.gui.save")
    d.open_block((90, 66, 54))
    d.click(key="glass.gui.manage_wireless")
    assert d.row("93, 65, 54")["entryIndex"] == 1
    d.capture("wireless_order_saved")
    d.action(d.row("93, 65, 54"))
    d.click(key="glass.gui.move_up")
    d.action(d.row("93, 65, 54"))
    d.key(261)
    d.click(key="glass.gui.save")
    d.open_block((90, 66, 54))
    d.click(key="glass.gui.manage_wireless")
    remaining = d.all_rows()
    assert len(remaining) == 4 and not any("93, 65, 54" in label for label in remaining)
    d.capture("wireless_removed")
    d.open_block((93, 65, 54))
    d.action(d.row("90, 66, 54"))
    d.click(key="glass.gui.attach")
    d.open_block((90, 66, 54))
    d.click(key="glass.gui.manage_wireless")
    attached = d.all_rows()
    assert len(attached) == 5 and "93, 65, 54" in attached[-1]
    d.action(d.row("93, 65, 54"))
    while d.selected()[0]["entryIndex"] > 0:
        d.click(key="glass.gui.move_up")
    assert d.row("93, 65, 54")["entryIndex"] == 0
    d.capture("wireless_binding")
    d.click(key="glass.gui.save")
    d.passed("wireless native entries, reorder, remove, rebind, scroll and save/reopen")

    d.config()
    setting = d.find(role="slider", prefix="Propagation Delay:")
    value, label = setting["value"], setting["label"]
    d.action(setting, "set_value", value=0.5 if abs(value - 0.5) > 0.1 else 0.2)
    changed = d.find(role="slider", prefix="Propagation Delay:")["label"]
    assert changed != label
    d.reopen_config()
    assert d.find(role="slider", prefix="Propagation Delay:")["label"] == changed
    d.capture("config_slider_saved")
    d.action(
        d.find(role="slider", prefix="Propagation Delay:"), "set_value", value=value
    )
    d.reopen_config()
    assert d.find(role="slider", prefix="Propagation Delay:")["label"] == label
    option = d.config_boolean()
    boolean_label = option["label"]
    d.action(option)
    changed = d.config_boolean()["label"]
    assert changed != boolean_label
    d.reopen_config()
    assert d.config_boolean()["label"] == changed
    d.capture("config_boolean_saved")
    d.action(d.config_boolean())
    d.reopen_config()
    assert d.config_boolean()["label"] == boolean_label
    d.click(key="gui.done")
    d.game()
    d.passed(
        "native settings slider and boolean save/reopen, Tab scrolling and restoration"
    )
    d.command("tp @s 48.5 65.0 27.0 180 10")
    time.sleep(0.3)
    d.close()
    d.passed("saved world, released input and normal process exit")


def run(human=False):
    m.quota()
    stamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    output = BASE / "result" / stamp
    output.mkdir(parents=True)
    directory = BASE / "instance" / stamp / ("acceptance" if human else "client")
    if human:
        previous = json.loads((BASE / "result/latest.json").read_text("utf-8"))
        assert previous["status"] == "PASS" and previous["productSha256"] == m.digest(
            product()
        )
        source = pathlib.Path(previous["directory"])
    else:
        source = m.ROOT / CONFIG["fixture"]["source"]
    prepare(source, directory, human)
    initial_config = json.loads((directory / "config/glass.json").read_text("utf-8"))
    process = m.launch(
        "acceptance" if human else "reference",
        directory,
        world=WORLD,
        result_directory=output,
        extra_scopes=() if human else ("command",),
    )
    report = {
        "status": "RUNNING",
        "directory": str(directory),
        "output": str(output),
        "productSha256": m.digest(product()),
        "mcpSha256": m.digest(m.snapshot()),
        "pid": process.pid,
        "human": human,
    }
    m.write_json(output / "report.json", report)
    d = GuiDriver(directory, process, output)
    try:
        if human:
            tree = d.tree()
            assert (tree["windowWidth"], tree["windowHeight"]) == (2560, 1440)
            d.use_block((48, 66, 24))
            assert d.tree()["screenClass"].endswith("TerminalChannelScreen")
            position = d.find(key="glass.gui.position")
            confirm = d.find(key="glass.gui.confirm")
            assert position["y"] + position["height"] <= confirm["y"]
            d.capture("acceptance_terminal")
            report["control"] = d.release()
            report["status"] = "READY_FOR_USER"
        else:
            assert (d.tree()["windowWidth"], d.tree()["windowHeight"]) == (1920, 1080)
            exercise(d)
            assert (
                json.loads((directory / "config/glass.json").read_text("utf-8"))
                == initial_config
            )
            d.passed("configuration persisted and restored on disk")
            report["status"] = "PASS"
    except BaseException as error:
        report["status"] = "FAIL"
        report["error"] = repr(error)
        try:
            d.capture("failure")
            d.close()
        except (AssertionError, RuntimeError, OSError):
            pass
        raise
    finally:
        report["checks"] = d.checks
        report["runtime"] = d.runtime.endpoint
        report["validationBytes"] = m.quota()
        m.write_json(output / "report.json", report)
        if report["status"] in ("PASS", "READY_FOR_USER"):
            m.write_json(
                BASE / "result" / ("acceptance.json" if human else "latest.json"),
                report,
            )
    print(json.dumps(report, ensure_ascii=False), flush=True)


if __name__ == "__main__":
    assert CONFIG["targets"] == [m.TARGET]
    run(human=len(sys.argv) > 1 and sys.argv[1] == "acceptance")
