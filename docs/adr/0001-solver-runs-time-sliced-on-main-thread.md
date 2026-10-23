# Solver runs time-sliced on the main thread

On Flutter web, isolates and `compute` don't run on a separate thread, so a long generation would freeze the page. Instead of moving the solver into a Web Worker via JS interop, the solver exposes an incremental `step()` and the UI runs as many steps as fit in a small per-frame budget (~8 ms). This keeps the solver a plain Dart library with no platform plumbing, and the step-by-step animation of an Output falls out of the same loop.

## Considered Options

- **Web Worker via JS interop**: truly off the main thread, but adds build and messaging plumbing, and animating the Wave would require streaming state across the worker boundary every frame.

## Consequences

- The solver's per-step cost must stay small and bounded; long uninterruptible operations (e.g. a full Propagation over a huge Output) need to be splittable or will cause jank.
