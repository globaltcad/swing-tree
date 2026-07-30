#!/usr/bin/env python3
"""
Runs the SwingTree benchmarks at every commit in a range and compiles the results.

The benchmarks under 'src/test/java/benchmarks' each print a small human readable table.
Running them by hand once per commit, copying the numbers somewhere and comparing them
afterwards is the tedious job this replaces: give it a start commit and an end commit and it
produces one JSON file per commit, one JSON file spanning the whole range, and an HTML report
that plots how each measured number moved from commit to commit.

    python3 tools/benchmark-history/bench_history.py run --from <sha> --to <sha>

Nothing about the checked out working tree is touched: every commit is measured in a detached
git worktree of its own, outside the repository.

Sub-commands
------------
  run       measure a commit range (and compile + report when done)
  compile   rebuild 'history.json' from the per-commit files already measured
  report    rebuild 'report.html' from 'history.json'
  list      print the commits a range expands to, and the benchmark matrix, without running
  clean     remove the measurement worktree

See 'README.md' next to this file for the reasoning behind the defaults.
"""

from __future__ import annotations

import argparse
import datetime as _datetime
import hashlib
import json
import os
import platform
import re
import shutil
import statistics
import subprocess
import sys
import tempfile
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
INIT_SCRIPT = HERE / "bench-init.gradle"

SCHEMA = "swingtree-benchmark-history/1"

# The commit that introduced the benchmarks; measuring anything before it is impossible,
# so it is the natural default start of a range.
DEFAULT_FROM = "c4a6b746"
DEFAULT_TO = "HEAD"
DEFAULT_OUT = "build/benchmark-history"


# ---------------------------------------------------------------------------------------
#  The benchmark matrix
# ---------------------------------------------------------------------------------------
#
#  One entry per JVM run. They are separate processes on purpose: the style render cache is
#  global and bounded, so two benchmarks sharing a process would measure each other's
#  leftovers rather than their own subject.
#
#  'parser' names the output format to read the numbers out of, and 'metric_prefix' is what
#  the metric ids of that run are rooted at. A metric id is 'benchmark/group/measure', e.g.
#  'resize/chat/drag' or 'noise/opaque/resizing'. All values are milliseconds, lower is better.

BENCHMARKS = [
    {
        "key": "resize",
        "main": "benchmarks.ViewResizeBenchmark",
        "props": {"benchmark.view": "all"},
        "parser": "resize_table",
        "metric_prefix": "resize",
        "description": "Layout and paint cost of a window resize, for all seven example UIs.",
    },
    {
        "key": "noise-opaque",
        "main": "benchmarks.NoisePaintBenchmark",
        "props": {"benchmark.noise": "opaque"},
        "parser": "two_line_paint",
        "metric_prefix": "noise/opaque",
        "description": "Paint cost of a large noise gradient with opaque colours.",
    },
    {
        "key": "noise-translucent",
        "main": "benchmarks.NoisePaintBenchmark",
        "props": {"benchmark.noise": "translucent"},
        "parser": "two_line_paint",
        "metric_prefix": "noise/translucent",
        "description": "Paint cost of a large noise gradient with translucent colours.",
    },
    {
        "key": "gradient-default",
        "main": "benchmarks.RoundedGradientBenchmark",
        "props": {"benchmark.cache": "default"},
        "parser": "two_line_paint",
        "metric_prefix": "gradient/cached",
        "description": "Paint cost of a rounded component with a gradient, render cache on.",
    },
    {
        "key": "gradient-nocache",
        "main": "benchmarks.RoundedGradientBenchmark",
        "props": {"benchmark.cache": "disabled"},
        "parser": "two_line_paint",
        "metric_prefix": "gradient/uncached",
        "description": "The same rounded gradient painted straight onto the surface, cache off.",
    },
]

BENCHMARKS_BY_KEY = {b["key"]: b for b in BENCHMARKS}

#  How the columns of the resize benchmark's table are named in the metric ids. The table
#  prints 'paint±' and 'paint=', which do not belong in a JSON key or a file name.
RESIZE_COLUMNS = ["layout", "paint_resize", "paint_static", "clip", "drag"]

#  Human readable labels, used by the report. Keyed by the trailing 'measure' of a metric id.
MEASURE_LABELS = {
    "layout": "re-layout at a fresh width",
    "paint_resize": "repaint after a size change (caches cold)",
    "paint_static": "repaint at an unchanged size (caches warm)",
    "clip": "repaint of a 300px clip (caret blink)",
    "drag": "one frame of a window drag (layout + repaint)",
    "resizing": "repaint while resizing",
    "static": "repaint at an unchanged size",
}


# ---------------------------------------------------------------------------------------
#  Small helpers
# ---------------------------------------------------------------------------------------

def log(message: str) -> None:
    stamp = _datetime.datetime.now().strftime("%H:%M:%S")
    print(f"[{stamp}] {message}", flush=True)


def run_git(repo: Path, *args: str, check: bool = True) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if check and result.returncode != 0:
        raise RuntimeError(
            f"git {' '.join(args)} failed ({result.returncode}):\n{result.stderr.strip()}"
        )
    return result.stdout


def repo_root(start: Path) -> Path:
    return Path(run_git(start, "rev-parse", "--show-toplevel").strip())


def resolve_commits(repo: Path, start: str, end: str) -> list[dict]:
    """
    The commits to measure, oldest first: the start commit itself, then every commit that
    first-parent history reaches from it up to the end commit.

    First-parent history only, because the merge commits on it are what the branch actually
    integrated - the individual commits inside a pull request are intermediate states that
    were never a version of the library anyone had.
    """
    start_sha = run_git(repo, "rev-parse", start + "^{commit}").strip()
    end_sha = run_git(repo, "rev-parse", end + "^{commit}").strip()
    ancestry = subprocess.run(
        ["git", "-C", str(repo), "merge-base", "--is-ancestor", start_sha, end_sha],
        capture_output=True,
    )
    if ancestry.returncode != 0:
        raise SystemExit(
            f"'{start}' ({start_sha[:8]}) is not an ancestor of '{end}' ({end_sha[:8]}); "
            "there is no commit range between them to measure."
        )
    shas = [start_sha]
    rest = run_git(repo, "rev-list", "--first-parent", "--reverse", f"{start_sha}..{end_sha}")
    shas.extend(line.strip() for line in rest.splitlines() if line.strip())

    commits = []
    for index, sha in enumerate(shas):
        fields = run_git(
            repo, "show", "-s", "--format=%H%x1f%h%x1f%s%x1f%aI%x1f%cI%x1f%an", sha
        ).strip("\n").split("\x1f")
        commits.append({
            "index": index,
            "sha": fields[0],
            "short": fields[1],
            "subject": fields[2],
            "author_date": fields[3],
            "commit_date": fields[4],
            "author": fields[5],
        })
    return commits


def environment_record(java_home: str | None) -> dict:
    java = "unknown"
    java_exe = str(Path(java_home) / "bin" / "java") if java_home else "java"
    try:
        out = subprocess.run(
            [java_exe, "-version"], capture_output=True, text=True, errors="replace"
        )
        java = (out.stderr or out.stdout).strip().splitlines()[0]
    except OSError:
        pass
    cpu = platform.processor()
    try:
        for line in Path("/proc/cpuinfo").read_text(errors="replace").splitlines():
            if line.startswith("model name"):
                cpu = line.split(":", 1)[1].strip()
                break
    except OSError:
        pass
    return {
        "host": platform.node(),
        "os": f"{platform.system()} {platform.release()}",
        "cpu": cpu,
        "cpu_count": os.cpu_count(),
        "java": java,
        "java_home": java_home or os.environ.get("JAVA_HOME", ""),
        "display": os.environ.get("DISPLAY", ""),
        "session_type": os.environ.get("XDG_SESSION_TYPE", ""),
    }


def now_iso() -> str:
    return _datetime.datetime.now(_datetime.timezone.utc).isoformat(timespec="seconds")


# ---------------------------------------------------------------------------------------
#  Parsing benchmark output
# ---------------------------------------------------------------------------------------

_RESIZE_ROW = re.compile(
    r"^ {2}(\S+)\s+(\d+)x(\d+)((?:\s+(?:-|\d+(?:\.\d+)?)){5})\s*$"
)
_PAINT_RESIZING = re.compile(r"repaint while resizing\s+(\d+(?:\.\d+)?)\s*ms")
_PAINT_STATIC = re.compile(r"repaint at an unchanged size\s+(\d+(?:\.\d+)?)\s*ms")
_SURFACE_LOSS = re.compile(r"volatile surface was restored or replaced\s+(\d+)\s+times")


def parse_resize_table(text: str, prefix: str) -> tuple[dict, dict, list]:
    """
    Reads the comparison table the resize benchmark prints, one row per example UI:

          view              size    layout    paint±    paint=   clip300      drag
          chat         2600x1660      3.75     19.16      8.16      1.57     22.91

    Returns the measured values keyed by metric id, some per-view context, and any warnings.
    A column holding '-' is a sweep the run skipped, and is left out rather than recorded
    as a zero.
    """
    values: dict[str, float] = {}
    context: dict[str, dict] = {}
    warnings: list[str] = []
    in_table = False
    for line in text.splitlines():
        if re.match(r"^ {2}view\s+size\s+layout", line):
            in_table = True
            continue
        if not in_table:
            continue
        match = _RESIZE_ROW.match(line)
        if not match:
            if line.strip() == "":
                # A blank line only ends the table once at least one row was read; the
                # header is followed directly by the rows.
                if values:
                    in_table = False
            continue
        view = match.group(1)
        context[view] = {"width": int(match.group(2)), "height": int(match.group(3))}
        cells = match.group(4).split()
        for column, cell in zip(RESIZE_COLUMNS, cells):
            if cell == "-":
                continue
            values[f"{prefix}/{view}/{column}"] = float(cell)
    if not values:
        warnings.append("no result table found in the benchmark output")
    return values, context, warnings


def parse_two_line_paint(text: str, prefix: str) -> tuple[dict, dict, list]:
    """
    Reads the two line result the noise and rounded-gradient benchmarks print:

          repaint while resizing         31.402 ms
          repaint at an unchanged size    0.232 ms
    """
    values: dict[str, float] = {}
    warnings: list[str] = []
    resizing = _PAINT_RESIZING.search(text)
    static = _PAINT_STATIC.search(text)
    if resizing:
        values[f"{prefix}/resizing"] = float(resizing.group(1))
    if static:
        values[f"{prefix}/static"] = float(static.group(1))
    if not values:
        warnings.append("no result lines found in the benchmark output")
    losses = _SURFACE_LOSS.search(text)
    if losses:
        # The benchmark itself says a run that lost its surface was not measuring one steady
        # pipeline, so the sample is kept but flagged rather than silently averaged in.
        warnings.append(f"volatile surface lost {losses.group(1)} times during the run")
    return values, {}, warnings


PARSERS = {
    "resize_table": parse_resize_table,
    "two_line_paint": parse_two_line_paint,
}


# ---------------------------------------------------------------------------------------
#  The measurement worktree
# ---------------------------------------------------------------------------------------

def default_worktree(repo: Path) -> Path:
    """
    A worktree outside the repository, in a location stable for this repository, so that the
    Gradle build directory inside it survives between invocations. That matters: the second
    round of a sweep revisits every commit, and a warm build cache is the difference between
    rebuilding the library and not.
    """
    tag = hashlib.sha1(str(repo).encode()).hexdigest()[:10]
    return Path(tempfile.gettempdir()) / f"swing-tree-bench-history-{tag}" / "worktree"


def ensure_worktree(repo: Path, worktree: Path) -> None:
    if (worktree / ".git").exists():
        return
    worktree.parent.mkdir(parents=True, exist_ok=True)
    if worktree.exists() and any(worktree.iterdir()):
        raise SystemExit(f"{worktree} exists and is not a git worktree; remove it or pass --worktree")
    run_git(repo, "worktree", "prune")
    log(f"creating measurement worktree at {worktree}")
    run_git(repo, "worktree", "add", "--detach", str(worktree), "HEAD")


def checkout(repo: Path, worktree: Path, sha: str) -> None:
    current = run_git(worktree, "rev-parse", "HEAD").strip()
    if current == sha:
        return
    # 'checkout --detach' would refuse if the previous run left something behind, and there
    # is nothing in this worktree worth preserving between commits:
    run_git(worktree, "checkout", "--detach", "--force", sha)
    run_git(worktree, "clean", "-fdq", "-e", "build", "-e", ".gradle")


def remove_worktree(repo: Path, worktree: Path) -> None:
    if (worktree / ".git").exists():
        log(f"removing worktree {worktree}")
        run_git(repo, "worktree", "remove", "--force", str(worktree), check=False)
    if worktree.exists():
        shutil.rmtree(worktree, ignore_errors=True)
    run_git(repo, "worktree", "prune", check=False)


# ---------------------------------------------------------------------------------------
#  Running one benchmark
# ---------------------------------------------------------------------------------------

def gradle_command(worktree: Path, benchmark: dict, extra_props: dict) -> list[str]:
    command = [
        str(worktree / "gradlew"),
        "--quiet",
        "--console=plain",
        "--build-cache",
        "--init-script", str(INIT_SCRIPT),
        "benchmarkRun",
        f"-PbenchMain={benchmark['main']}",
    ]
    for key, value in {**benchmark["props"], **extra_props}.items():
        command.append(f"-D{key}={value}")
    return command


def run_benchmark(
    worktree: Path, benchmark: dict, extra_props: dict, log_path: Path,
    timeout: int, java_home: str | None,
) -> dict:
    env = dict(os.environ)
    if java_home:
        env["JAVA_HOME"] = java_home
        env["PATH"] = str(Path(java_home) / "bin") + os.pathsep + env.get("PATH", "")
    command = gradle_command(worktree, benchmark, extra_props)
    started = time.time()
    load_before = os.getloadavg()[0] if hasattr(os, "getloadavg") else None
    try:
        completed = subprocess.run(
            command, cwd=str(worktree), env=env, capture_output=True,
            text=True, encoding="utf-8", errors="replace", timeout=timeout,
        )
        output = completed.stdout + "\n" + completed.stderr
        exit_code = completed.returncode
        timed_out = False
    except subprocess.TimeoutExpired as expired:
        output = (expired.stdout or "") + "\n" + (expired.stderr or "")
        if isinstance(output, bytes):
            output = output.decode("utf-8", errors="replace")
        exit_code = -1
        timed_out = True
    duration = time.time() - started

    log_path.parent.mkdir(parents=True, exist_ok=True)
    log_path.write_text(
        "$ " + " ".join(command) + "\n\n" + output, encoding="utf-8"
    )

    warnings: list[str] = []
    values: dict[str, float] = {}
    context: dict = {}
    if timed_out:
        warnings.append(f"timed out after {timeout}s")
    elif exit_code != 0:
        warnings.append(f"gradle exited with {exit_code}")
    else:
        values, context, warnings = PARSERS[benchmark["parser"]](output, benchmark["metric_prefix"])

    return {
        "values": values,
        "context": context,
        "warnings": warnings,
        "ok": bool(values) and not timed_out and exit_code == 0,
        "exit_code": exit_code,
        "duration_s": round(duration, 1),
        "load_before": load_before,
        "started": _datetime.datetime.fromtimestamp(started).isoformat(timespec="seconds"),
        "log": log_path.name,
    }


def prepare_build(worktree: Path, timeout: int, java_home: str | None) -> tuple[bool, str]:
    """
    Compiles the commit before anything is timed, so that a build failure is reported as a
    build failure, and so that no benchmark run pays for the compilation of its own commit.
    """
    env = dict(os.environ)
    if java_home:
        env["JAVA_HOME"] = java_home
        env["PATH"] = str(Path(java_home) / "bin") + os.pathsep + env.get("PATH", "")
    command = [
        str(worktree / "gradlew"), "--quiet", "--console=plain", "--build-cache",
        "--init-script", str(INIT_SCRIPT), "compileTestJava", "processTestResources",
    ]
    try:
        completed = subprocess.run(
            command, cwd=str(worktree), env=env, capture_output=True,
            text=True, encoding="utf-8", errors="replace", timeout=timeout,
        )
    except subprocess.TimeoutExpired:
        return False, f"build timed out after {timeout}s"
    if completed.returncode != 0:
        return False, (completed.stdout + "\n" + completed.stderr).strip()[-4000:]
    return True, ""


# ---------------------------------------------------------------------------------------
#  Per-commit result files
# ---------------------------------------------------------------------------------------

def commit_file(out: Path, commit: dict) -> Path:
    return out / "runs" / f"{commit['index']:03d}-{commit['short']}.json"


def load_commit_result(path: Path) -> dict | None:
    if not path.exists():
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (ValueError, OSError):
        return None


def new_commit_result(commit: dict, environment: dict) -> dict:
    return {
        "schema": SCHEMA,
        "commit": commit,
        "environment": environment,
        "samples": {},   # metric id -> list of measured values, one per completed run
        "context": {},   # e.g. the geometry each example UI was measured at
        "runs": [],      # one record per (round, benchmark) attempt, in the order they ran
    }


def record_run(result: dict, benchmark_key: str, round_index: int, outcome: dict) -> None:
    for metric, value in outcome["values"].items():
        result["samples"].setdefault(metric, []).append(value)
    for key, value in outcome.get("context", {}).items():
        result["context"][key] = value
    result["runs"].append({
        "round": round_index,
        "benchmark": benchmark_key,
        "ok": outcome["ok"],
        "exit_code": outcome["exit_code"],
        "duration_s": outcome["duration_s"],
        "load_before": outcome["load_before"],
        "started": outcome["started"],
        "warnings": outcome["warnings"],
        "log": outcome["log"],
    })


def already_done(result: dict, benchmark_key: str, round_index: int) -> bool:
    return any(
        run["round"] == round_index and run["benchmark"] == benchmark_key and run["ok"]
        for run in result.get("runs", [])
    )


def summarize(samples: list[float]) -> dict:
    ordered = sorted(samples)
    median = statistics.median(ordered)
    return {
        "median": round(median, 4),
        "min": round(ordered[0], 4),
        "max": round(ordered[-1], 4),
        "n": len(ordered),
        # The spread as a fraction of the median, which is the honest way to read a chart of
        # these: a commit-to-commit step smaller than this is not distinguishable from noise.
        "spread": round((ordered[-1] - ordered[0]) / median, 4) if median else 0.0,
    }


def write_commit_result(path: Path, result: dict) -> None:
    result["stats"] = {
        metric: summarize(values) for metric, values in sorted(result["samples"].items())
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")


# ---------------------------------------------------------------------------------------
#  Compiling the cross-commit file
# ---------------------------------------------------------------------------------------

def metric_descriptor(metric_id: str) -> dict:
    parts = metric_id.split("/")
    benchmark, group, measure = parts[0], "/".join(parts[1:-1]), parts[-1]
    return {
        "id": metric_id,
        "benchmark": benchmark,
        "group": group,
        "measure": measure,
        "label": MEASURE_LABELS.get(measure, measure),
        "unit": "ms",
        "lower_is_better": True,
    }


def compile_history(out: Path, meta: dict | None = None) -> dict:
    runs_dir = out / "runs"
    if not runs_dir.is_dir():
        raise SystemExit(f"no per-commit results under {runs_dir}; run the 'run' command first")

    results = []
    for path in sorted(runs_dir.glob("*.json")):
        loaded = load_commit_result(path)
        if loaded and loaded.get("commit"):
            results.append(loaded)
    results.sort(key=lambda r: r["commit"]["index"])
    if not results:
        raise SystemExit(f"no readable per-commit results under {runs_dir}")

    metric_ids: list[str] = []
    for result in results:
        for metric in result.get("stats", {}):
            if metric not in metric_ids:
                metric_ids.append(metric)
    metric_ids.sort()

    commits = []
    for result in results:
        commits.append({
            **result["commit"],
            "stats": result.get("stats", {}),
            "samples": result.get("samples", {}),
            "context": result.get("context", {}),
            "warnings": sorted({
                w for run in result.get("runs", []) for w in run.get("warnings", [])
            }),
            "failed_runs": [
                {"round": r["round"], "benchmark": r["benchmark"], "warnings": r["warnings"]}
                for r in result.get("runs", []) if not r["ok"]
            ],
        })

    history = {
        "schema": SCHEMA,
        "generated": now_iso(),
        "meta": meta or {},
        "environment": results[0].get("environment", {}),
        "benchmarks": [
            {"key": b["key"], "main": b["main"], "props": b["props"],
             "description": b["description"]}
            for b in BENCHMARKS
        ],
        "metrics": [metric_descriptor(m) for m in metric_ids],
        "commits": commits,
    }

    path = out / "history.json"
    path.write_text(json.dumps(history, indent=2) + "\n", encoding="utf-8")
    log(f"compiled {len(commits)} commits and {len(metric_ids)} metrics into {path}")
    return history


# ---------------------------------------------------------------------------------------
#  Commands
# ---------------------------------------------------------------------------------------

def command_list(args) -> int:
    repo = repo_root(Path.cwd())
    commits = resolve_commits(repo, args.start, args.end)
    selected = select_benchmarks(args.only)
    print(f"{len(commits)} commits, {len(selected)} benchmark runs each, "
          f"{args.rounds} round(s) -> {len(commits) * len(selected) * args.rounds} JVM runs\n")
    for commit in commits:
        print(f"  {commit['index']:3d}  {commit['short']}  {commit['commit_date'][:10]}  "
              f"{commit['subject'][:78]}")
    print()
    for benchmark in selected:
        props = " ".join(f"-D{k}={v}" for k, v in benchmark["props"].items())
        print(f"  {benchmark['key']:<20} {benchmark['main']} {props}")
    return 0


def select_benchmarks(only: str | None) -> list[dict]:
    if not only:
        return list(BENCHMARKS)
    wanted = [part.strip() for part in only.split(",") if part.strip()]
    selected = []
    for name in wanted:
        matches = [b for b in BENCHMARKS if b["key"] == name or b["key"].startswith(name + "-")]
        if not matches:
            raise SystemExit(
                f"unknown benchmark '{name}'; known keys: {', '.join(b['key'] for b in BENCHMARKS)}"
            )
        for match in matches:
            if match not in selected:
                selected.append(match)
    return selected


def command_run(args) -> int:
    repo = repo_root(Path.cwd())
    out = Path(args.out)
    if not out.is_absolute():
        out = repo / out
    out.mkdir(parents=True, exist_ok=True)

    commits = resolve_commits(repo, args.start, args.end)
    selected = select_benchmarks(args.only)
    worktree = Path(args.worktree) if args.worktree else default_worktree(repo)
    environment = environment_record(args.java_home)

    total = len(commits) * len(selected) * args.rounds
    log(f"{len(commits)} commits x {len(selected)} benchmarks x {args.rounds} rounds "
        f"= {total} benchmark runs")
    log(f"results -> {out}")
    log(f"java    -> {environment['java']}")

    if args.force:
        for path in (out / "runs").glob("*.json"):
            path.unlink()

    ensure_worktree(repo, worktree)

    meta = {
        "from": commits[0]["sha"],
        "to": commits[-1]["sha"],
        "from_short": commits[0]["short"],
        "to_short": commits[-1]["short"],
        "rounds": args.rounds,
        "benchmarks": [b["key"] for b in selected],
        "started": now_iso(),
        "tool": "tools/benchmark-history/bench_history.py",
    }

    done = 0
    failures = 0
    started_at = time.time()
    for round_index in range(args.rounds):
        # Alternating direction rather than the same order every round. The machine drifts
        # over a sweep that takes the better part of an hour, and sweeping one way only would
        # bake that drift into the commit order itself - the last commits would always be
        # measured on a warmer machine than the first.
        ordered = commits if round_index % 2 == 0 else list(reversed(commits))
        log(f"=== round {round_index + 1}/{args.rounds} "
            f"({'oldest first' if round_index % 2 == 0 else 'newest first'}) ===")
        for commit in ordered:
            path = commit_file(out, commit)
            result = load_commit_result(path) or new_commit_result(commit, environment)
            result["commit"] = commit
            pending = [b for b in selected if not already_done(result, b["key"], round_index)]
            if not pending:
                done += len(selected)
                continue

            checkout(repo, worktree, commit["sha"])
            ok, error = prepare_build(worktree, args.build_timeout, args.java_home)
            if not ok:
                failures += len(pending)
                done += len(selected)
                log(f"  {commit['short']}: BUILD FAILED, skipping - {error.splitlines()[-1][:160]}"
                    if error else f"  {commit['short']}: BUILD FAILED, skipping")
                result.setdefault("build_failures", []).append(
                    {"round": round_index, "error": error[-2000:]}
                )
                write_commit_result(path, result)
                continue

            for benchmark in pending:
                log_path = out / "logs" / f"{commit['index']:03d}-{commit['short']}" \
                                          f"-r{round_index}-{benchmark['key']}.log"
                outcome = run_benchmark(
                    worktree, benchmark, args.extra_props, log_path,
                    args.timeout, args.java_home,
                )
                record_run(result, benchmark["key"], round_index, outcome)
                done += 1
                if not outcome["ok"]:
                    failures += 1
                elapsed = time.time() - started_at
                eta = (elapsed / done) * (total - done) if done else 0
                status = "ok" if outcome["ok"] else "FAILED"
                note = f" ({'; '.join(outcome['warnings'])})" if outcome["warnings"] else ""
                log(f"  [{done}/{total}] {commit['short']} {benchmark['key']:<18} "
                    f"{status} {outcome['duration_s']:>5.1f}s{note}  eta {eta / 60:.0f}m")
                write_commit_result(path, result)

        # Compiling after every round keeps a partial sweep usable: an interrupted run still
        # leaves a readable history file behind.
        meta["rounds_completed"] = round_index + 1
        history = compile_history(out, meta)
        write_report(history, out / "report.html")

    log(f"done in {(time.time() - started_at) / 60:.0f} min, {failures} failed run(s)")
    # The worktree is deliberately left behind: its Gradle build directory is what makes a
    # follow-up sweep of the same range cheap. 'clean' removes it.
    log(f"worktree kept at {worktree}")
    return 1 if failures and args.strict else 0


def command_compile(args) -> int:
    repo = repo_root(Path.cwd())
    out = Path(args.out)
    if not out.is_absolute():
        out = repo / out
    history = compile_history(out)
    write_report(history, out / "report.html")
    return 0


def command_report(args) -> int:
    repo = repo_root(Path.cwd())
    out = Path(args.out)
    if not out.is_absolute():
        out = repo / out
    path = out / "history.json"
    if not path.exists():
        raise SystemExit(f"{path} does not exist; run the 'compile' command first")
    history = json.loads(path.read_text(encoding="utf-8"))
    write_report(history, out / "report.html")
    return 0


def command_clean(args) -> int:
    repo = repo_root(Path.cwd())
    worktree = Path(args.worktree) if args.worktree else default_worktree(repo)
    remove_worktree(repo, worktree)
    return 0


def write_report(history: dict, path: Path) -> None:
    from bench_report import render_report  # local import: the runner works without it
    path.write_text(render_report(history), encoding="utf-8")
    log(f"report -> {path}")


# ---------------------------------------------------------------------------------------

def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="bench_history.py",
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    subparsers = parser.add_subparsers(dest="command", required=True)

    def add_common(subparser):
        subparser.add_argument("--out", default=DEFAULT_OUT,
                               help=f"where results are written (default: {DEFAULT_OUT})")

    def add_range(subparser):
        subparser.add_argument("--from", dest="start", default=DEFAULT_FROM,
                               help=f"first commit to measure, inclusive (default: {DEFAULT_FROM}, "
                                    "the commit that introduced the benchmarks)")
        subparser.add_argument("--to", dest="end", default=DEFAULT_TO,
                               help=f"last commit to measure, inclusive (default: {DEFAULT_TO})")
        subparser.add_argument("--only", default=None,
                               help="comma separated benchmark keys to run instead of all: "
                                    + ", ".join(b["key"] for b in BENCHMARKS))
        subparser.add_argument("--rounds", type=int, default=3,
                               help="how often to sweep the whole range; every commit is measured "
                                    "once per round and the median is reported (default: 3)")

    run_parser = subparsers.add_parser("run", help="measure a commit range")
    add_common(run_parser)
    add_range(run_parser)
    run_parser.add_argument("--worktree", default=None,
                            help="where to check the commits out (default: a stable temp directory)")
    run_parser.add_argument("--java-home", default=None,
                            help="JDK to build and measure with; the benchmarks are meant to be "
                                 "run on JDK 21")
    run_parser.add_argument("--timeout", type=int, default=1200,
                            help="seconds a single benchmark run may take (default: 1200)")
    run_parser.add_argument("--build-timeout", type=int, default=1800,
                            help="seconds a single commit's build may take (default: 1800)")
    run_parser.add_argument("--force", action="store_true",
                            help="discard existing per-commit results and measure from scratch")
    run_parser.add_argument("--strict", action="store_true",
                            help="exit non-zero if any benchmark run failed")
    run_parser.add_argument("-D", dest="extra_props", action="append", default=[],
                            metavar="benchmark.x=y",
                            help="extra system property for every benchmark JVM, repeatable")
    run_parser.set_defaults(func=command_run)

    list_parser = subparsers.add_parser("list", help="show the commits and matrix a range expands to")
    add_common(list_parser)
    add_range(list_parser)
    list_parser.set_defaults(func=command_list)

    compile_parser = subparsers.add_parser("compile", help="rebuild history.json and report.html")
    add_common(compile_parser)
    compile_parser.set_defaults(func=command_compile)

    report_parser = subparsers.add_parser("report", help="rebuild report.html from history.json")
    add_common(report_parser)
    report_parser.set_defaults(func=command_report)

    clean_parser = subparsers.add_parser("clean", help="remove the measurement worktree")
    clean_parser.add_argument("--worktree", default=None)
    clean_parser.set_defaults(func=command_clean)

    return parser


def main(argv: list[str]) -> int:
    sys.path.insert(0, str(HERE))
    args = build_parser().parse_args(argv)
    if hasattr(args, "extra_props"):
        parsed = {}
        for entry in args.extra_props:
            if "=" not in entry:
                raise SystemExit(f"-D expects 'key=value', got '{entry}'")
            key, value = entry.split("=", 1)
            parsed[key] = value
        args.extra_props = parsed
    return args.func(args)


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
