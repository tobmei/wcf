# Coding Standards

Quantum Quilt is a Flutter web app. Domain vocabulary lives in `GLOSSARY.md`; architecture decisions in `docs/adr/`.

## Domain language

- Use the terms from `GLOSSARY.md` in identifiers, tests, UI copy and commits (Sample, Preset, Pattern, Output, Cell, Wave, Observation, Propagation, Contradiction, Settings, …). Never use the words listed under _Avoid_ (e.g. no `input`, `tile`, `ripple`, `zeroWave`).
- Don't contradict an ADR silently. If a change needs to deviate, say so in the commit and add or supersede an ADR.

## Style

- Follow the official Dart style guide and keep `flutter analyze` at zero issues (`flutter_lints` rules plus whatever `analysis_options.yaml` adds).
- `lowerCamelCase` for members and variables, `UpperCamelCase` for types, `snake_case` file names.
- Prefer `final` and immutable value types; no `dynamic`, avoid `as` casts and `!` null assertions where the type system can express the guarantee instead.
- Plain Flutter state (`ChangeNotifier` / `ValueNotifier`) and `go_router`; don't introduce other state-management or DI packages without an ADR.
- Small widgets composed together; keep business logic out of widget `build` methods.

## Architecture

- The solver (Pattern extraction, Wave, Observation, Propagation) is pure Dart with no Flutter imports, so it can be tested and benchmarked on the Dart VM.
- The solver advances in small, bounded steps; the UI drives it time-sliced on the main thread (ADR-0001). No step may do unbounded work.
- Persistence goes through repository interfaces; the UI never touches browser storage directly. Only browser-local implementations exist for now.
- Don't add packages for things a few lines of Dart can do.

## Testing

- Every new behaviour gets a test; write it first where practical (red, green, refactor).
- Solver tests check properties (one Pattern per Cell in a finished Output, every neighbouring pair compatible) rather than exact images, because generation is random.
- Unit-test pure logic (Pattern extraction, flood fill, undo/redo, PNG conversion); use widget tests for screen flows, not for pixel details.
- Test names describe the behaviour in domain language, e.g. `'Observation removes all other Patterns from the Cell'`.
