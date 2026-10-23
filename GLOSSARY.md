# Quantum Quilt

An app that generates new pixel images from a small hand-drawn image using the Wave Function Collapse (overlapping model) algorithm.

## Images

**Sample**:
A small pixel image drawn or imported by the user, from which Patterns are learned.
_Avoid_: Input, source image, template

**Preset**:
A read-only Sample bundled with the app; duplicating it creates an editable Sample.
_Avoid_: Example, template, default sample

**Pattern**:
An N×N block of pixels taken from a Sample (including its rotations and reflections, depending on the Symmetry setting).
_Avoid_: Tile, piece

**Output**:
A pixel image generated from a Sample.
_Avoid_: Result, generation

## Generation

**Cell**:
One position in an Output.
_Avoid_: Tile, pixel (when meaning the solver position)

**Wave**:
The set of Patterns still possible for every Cell of an Output being generated.
_Avoid_: Grid, state

**Observation**:
Choosing a single Pattern for one Cell, removing all its other possibilities.
_Avoid_: Collapse (when meaning one Cell), pick

**Propagation**:
Removing Patterns from neighbouring Cells that are no longer compatible after an Observation.
_Avoid_: Ripple, spreading

**Contradiction**:
The state where some Cell has no Patterns left, so the current generation attempt cannot succeed.
_Avoid_: Zero wave, conflict

## Settings

**Settings**:
The full set of parameters for generating one Output from a Sample.
_Avoid_: Options, config, parameters

**Pattern Size**:
The side length N of the square Patterns taken from a Sample.
_Avoid_: N (in prose), tile size, kernel

**Symmetry**:
Which rotations and reflections of each Pattern are also added as Patterns.
_Avoid_: Rotation option, augmentation

**Periodic Sample**:
Treating the Sample as wrapping around at its edges when taking Patterns.
_Avoid_: Periodic input, wrap input

**Periodic Output**:
Generating an Output whose opposite edges fit together, so it tiles seamlessly.
_Avoid_: Tileable, wrap output
