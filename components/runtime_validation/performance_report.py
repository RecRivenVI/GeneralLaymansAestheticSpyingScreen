"""Summarize frame boundaries, task counts and contemporaneous host samples."""

import csv
import json
import pathlib
import statistics

BASE = (
    pathlib.Path(__file__).resolve().parents[2]
    / "validations/performance-remote_projection"
)


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def stats(values):
    values = sorted(values)
    if not values:
        return None
    return {
        "p50": values[int((len(values) - 1) * 0.5)],
        "p95": values[int((len(values) - 1) * 0.95)],
        "p99": values[int((len(values) - 1) * 0.99)],
        "mean": statistics.fmean(values),
        "max": values[-1],
    }


def host_window(samples, start, end, pid):
    selected = [s for s in samples if start <= s["utcMillis"] <= end]
    own_gpu = []
    external_gpu = []
    dedicated = []
    working = []
    private = []
    clocks = []
    device = []
    peers = set()
    for sample in selected:
        own_gpu.append(
            sum(
                e["percent"]
                for e in sample["engines"]
                if e["pid"] == pid and e["name"].endswith("engtype_3D")
            )
        )
        external_gpu.append(
            sum(
                e["percent"]
                for e in sample["engines"]
                if e["pid"] != pid and e["name"].endswith("engtype_3D")
            )
        )
        for process in sample["processes"]:
            if process["pid"] == pid:
                working.append(process["workingSet"])
                private.append(process["privateBytes"])
            elif process.get("title"):
                peers.add((process["pid"], process["title"]))
        dedicated.extend(
            m["DedicatedUsage"]
            for m in sample["memory"]
            if m["Name"].startswith("pid_" + str(pid) + "_")
        )
        if sample.get("nvidia"):
            values = sample["nvidia"].split(",")
            device.append(float(values[0]))
            clocks.append(float(values[2]))
    return {
        "samples": len(selected),
        "own3DPercent": stats(own_gpu),
        "external3DPercent": stats(external_gpu),
        "dedicatedMiB": stats([v / 1024**2 for v in dedicated]),
        "workingSetMiB": stats([v / 1024**2 for v in working]),
        "privateMiB": stats([v / 1024**2 for v in private]),
        "gpuClockMHz": stats(clocks),
        "deviceBusyPercent": stats(device),
        "parallelClients": [{"pid": p, "title": t} for p, t in sorted(peers)],
        "interference": "present"
        if any(v >= 10 for v in external_gpu) or any(v >= 90 for v in device)
        else "background present; not a quiet-system proof",
    }


def analyze():
    output = []
    for directory in (BASE / "result").iterdir():
        if (
            not directory.is_dir()
            or not (directory / "automatic-process.json").exists()
        ):
            continue
        pid = read(directory / "automatic-process.json")["pid"]
        samples = (
            [
                json.loads(line)
                for line in (directory / "host.jsonl")
                .read_text(encoding="utf-8-sig")
                .splitlines()
                if line
            ]
            if (directory / "host.jsonl").exists()
            else []
        )
        for path in directory.glob("*.csv"):
            meta = read(path.with_suffix(".json"))
            rows = list(csv.DictReader(path.open(encoding="utf-8")))[1:]
            metrics = {
                key: stats([int(row[key]) / 1e6 for row in rows if int(row[key]) >= 0])
                for key in rows[0]
            }
            duration = (meta["endedAtMillis"] - meta["startedAtMillis"]) / 1000
            cap = metrics["capture_wall_ns"]["mean"]
            draw = metrics["draw_wall_ns"]["mean"]
            record = {
                "case": directory.name,
                "window": path.stem,
                "frames": len(rows),
                "durationSeconds": duration,
                "ms": metrics,
                "captureShareOfBoundedRenderWall": cap / (cap + draw)
                if cap + draw
                else None,
                "nativeCompilesPerSecond": meta["nativeCompileCount"] / duration,
                "nativeSourceCompiles": meta.get("nativeSourceCompileCount"),
                "nativeCompileWorkerWallMsPerSecond": meta["nativeCompileNanos"]
                / 1e6
                / duration,
                "sodiumCompilesPerSecond": meta.get("sodiumCompileCount", 0) / duration,
                "sodiumSourceCompiles": meta.get("sodiumSourceCompileCount"),
                "sodiumCompileWorkerWallMsPerSecond": meta.get("sodiumCompileNanos", 0)
                / 1e6
                / duration,
                "heapUsedMiBAtEnd": meta["heapUsed"] / 1024**2,
                "nonHeapUsedMiBAtEnd": meta["nonHeapUsed"] / 1024**2,
                "buffers": meta["buffers"],
                "resources": meta["resources"],
                "host": host_window(
                    samples, meta["startedAtMillis"], meta["endedAtMillis"], pid
                ),
            }
            pending = {}
            waits = []
            for event in meta.get("timeline", []):
                when = event["utcMillis"]
                if not meta["startedAtMillis"] <= when <= meta["endedAtMillis"]:
                    continue
                shape = event["details"]["hash"]
                if event["kind"] == "source-shape":
                    pending[shape] = when
                elif shape in pending:
                    waits.append(
                        {
                            "sourceShapeAtMillis": pending[shape],
                            "snapshotReadyAtMillis": when,
                            "waitMillis": when - pending.pop(shape),
                        }
                    )
            record["sourceSnapshotWaits"] = waits
            output.append(record)
    (BASE / "result/measurements.json").write_text(
        json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    server = BASE / "result/network-final"
    if (server / "exit.json").exists():
        host = [
            json.loads(line)
            for line in (BASE / "result/network-final-client/host.jsonl")
            .read_text(encoding="utf-8-sig")
            .splitlines()
            if line
        ]
        pid = read(server / "server-ready.json")["pid"]
        windows = []
        for path in sorted(server.glob("*viewers*.json")):
            meta = read(path)
            seconds = (meta["endedAtMillis"] - meta["startedAtMillis"]) / 1000
            before = {e["uuid"]: e for e in meta["entitiesBefore"]}
            advances = [
                e["ticks"] - before[e["uuid"]]["ticks"]
                for e in meta["entitiesAfter"]
                if e["loaded"] and before[e["uuid"]]["loaded"]
            ]
            windows.append(
                {
                    "window": path.stem,
                    "mspt": stats([v / 1e6 for v in meta["ticks"]]),
                    "tickThreadMs": stats([v / 1e6 for v in meta["tickCpuNanos"]]),
                    "actualTcpPayloadBytesPerSecond": {
                        k: v / seconds for k, v in meta["wireSent"].items()
                    },
                    "encodedGlassPayloadBytesPerSecond": {
                        k: v / seconds for k, v in meta["encodedGlassPayload"].items()
                    },
                    "liveEntityTickAdvances": stats(advances),
                    "sources": meta["sources"],
                    "fullChunks": meta["fullChunks"],
                    "host": host_window(
                        host, meta["startedAtMillis"], meta["endedAtMillis"], pid
                    ),
                }
            )
        (BASE / "result/network-measurements.json").write_text(
            json.dumps(windows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
    print(
        json.dumps(
            [
                {
                    "case": r["case"],
                    "window": r["window"],
                    "p50": round(r["ms"]["frame_interval_ns"]["p50"], 2),
                    "p95": round(r["ms"]["frame_interval_ns"]["p95"], 2),
                    "p99": round(r["ms"]["frame_interval_ns"]["p99"], 2),
                    "captureMs": round(r["ms"]["capture_wall_ns"]["mean"], 3),
                    "captureShare": round(r["captureShareOfBoundedRenderWall"] or 0, 3),
                    "sourceCompiles": r["nativeSourceCompiles"]
                    or r["sodiumSourceCompiles"],
                    "interference": r["host"]["interference"],
                }
                for r in output
                if not r["case"].startswith("pilot")
            ],
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    analyze()
