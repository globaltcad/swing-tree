# Benchmark history

Runs the benchmarks in `src/test/java/benchmarks` at **every commit in a range** and compiles
the numbers into JSON and an HTML report, so that the effect of a series of commits on
rendering performance can be looked at rather than remembered.

```bash
# The default range is the commit that introduced the benchmarks .. HEAD
python3 tools/benchmark-history/bench_history.py run

# Or an explicit range
python3 tools/benchmark-history/bench_history.py run --from c4a6b746 --to HEAD --rounds 3
```

Results land in `build/benchmark-history/` (override with `--out`):

| Path | What it holds |
|---|---|
| `runs/NNN-<sha>.json` | one file per commit: every raw sample, per-metric medians, and a record of each benchmark run |
| `history.json` | all commits and all metrics in one file - this is the file to keep, diff, or feed to something else |
| `report.html` | a self-contained visualization; open it in a browser, no server and no network needed |
| `logs/` | the full stdout of every benchmark run, for when a number looks wrong |

Nothing about the working tree is touched: each commit is checked out into a detached git
worktree under the system temp directory. `bench_history.py clean` removes it.

## Commands

| Command | Purpose |
|---|---|
| `run` | measure a range, then compile and report |
| `list` | print the commits and the benchmark matrix a range expands to, without running anything |
| `compile` | rebuild `history.json` (and the report) from the per-commit files |
| `report` | rebuild `report.html` from `history.json` |
| `clean` | remove the measurement worktree |

Useful flags on `run`:

- `--rounds N` - how often the **whole range** is swept. Every commit is measured once per
  round and the median of the rounds is reported. Three is the default.
- `--only resize,noise` - measure a subset of the matrix (keys below; a benchmark family name
  such as `noise` selects both of its variants).
- `--java-home <path>` - the JDK to build and measure with. The benchmarks are meant to be run
  on **JDK 21**; measuring one commit on one JDK and the next on another compares two things
  at once.
- `-D benchmark.sweeps=10 -D benchmark.warmup=6` - forwarded to every benchmark JVM. Lower
  them for a quick dry run; the defaults are what a real measurement wants.
- `--force` - discard existing per-commit results instead of adding to them.

## The benchmark matrix

Five JVM runs per commit, each a separate process because the style render cache is global -
two benchmarks in one process would measure each other's leftovers.

| Key | Class | Measures |
|---|---|---|
| `resize` | `ViewResizeBenchmark -Dbenchmark.view=all` | layout and paint cost of a resize, for all seven example UIs |
| `noise-opaque` | `NoisePaintBenchmark` | a large noise gradient with opaque colours |
| `noise-translucent` | `NoisePaintBenchmark` | the same noise translucent, which denies it the opaque blit |
| `gradient-default` | `RoundedGradientBenchmark` | a rounded panel with a radial gradient, render cache on |
| `gradient-nocache` | `RoundedGradientBenchmark -Dbenchmark.cache=disabled` | the same panel with the cache off |

Metric ids are `benchmark/group/measure`, e.g. `resize/chat/drag` or `noise/opaque/resizing`.
All values are milliseconds and lower is better.

## How it measures, and what that costs

- **Repeated, interleaved rounds.** A desktop machine drifts: `RoundedGradientBenchmark`'s own
  javadoc warns that its absolute figure moves by half between one minute and the next. So the
  tool sweeps the range several times rather than taking three samples in a row at one commit,
  and it **alternates direction** each round (oldest-first, then newest-first). Sweeping one
  way only would bake the drift into the commit order itself - the last commits would always be
  the ones measured on the warmer machine. The report draws the spread between the fastest and
  slowest round as a band around each line: a step smaller than the band is not a result.
- **A real display is required.** The benchmarks open a window and paint into a `VolatileImage`,
  which is the accelerated path a real window's back buffer uses. There is nothing to measure
  headless. Keep off the machine while a sweep runs - a busy compositor inflates every paint
  number, and it will do so unevenly across the range.
- **The Groovy test sources are not compiled.** `bench-init.gradle` adds a `benchmarkRun` task
  that depends on `compileTestJava` only. The benchmarks and the example UIs are plain Java;
  compiling the Spock specs at every commit would dominate the whole sweep. The Gradle build
  cache is enabled for the same reason - the second round revisits commits the first already
  built.
- **Resumable.** A completed `(commit, round, benchmark)` is never re-run, so an interrupted
  sweep continues where it stopped, and `history.json` plus the report are rewritten after
  every round - a partial sweep is still readable.

## Reading the result

The comparison is only clean while the benchmark sources and the example UIs they build are
unchanged across the range - otherwise a moved number can mean the library got slower or the
example got bigger, and the two are indistinguishable. Check with:

```bash
git log --oneline --first-parent <from>..<to> -- src/test/java/benchmarks src/test/java/examples
```

An empty result means every difference in the report comes from `src/main`.
