# AE2 Pattern Scaler

AE2 Pattern Scaler adds quick amount buttons to the Pattern Encoding Terminal. They appear beside the lower-left corner of the terminal while **Processing Pattern** mode is selected.

The buttons are arranged in paired rows:

```text
×2   ÷2
×4   ÷4
×8   ÷8
×16  ÷16
×32  ÷32
×64  ÷64
```

One click scales every filled input and output. Empty slots stay empty, and the items or fluids in each slot do not change.

## Compatibility

- Minecraft 1.21.1
- NeoForge 21.1.x
- Applied Energistics 2 19.2.x

This release was built with NeoForge 21.1.248 and AE2 19.2.17. Its metadata accepts AE2 versions from 19.2.0 up to, but not including, 19.3.0.

## Installation

Copy `ae2-pattern-scaler-1.0.0.jar` into the client instance's `mods` folder.

Do not install it on the server. The server only needs its usual AE2 installation.

## Amount rules

The mod keeps AE2's normal editor limits:

- 999,999 items per slot
- 999,999 buckets per fluid slot
- 1 mB fluid precision

Multiplication is checked for overflow before anything is sent.

Division must be exact for every filled slot. For example, `200 mB ÷ 8` becomes `25 mB`, but `25 mB ÷ 2` is rejected. If one slot would become fractional, zero, or too large, the whole operation is cancelled and a short message explains why.

## Building

Use JDK 21 and run:

```text
gradlew.bat clean check build
```

The JAR will be in `build/libs`. The tests cover multiplication, limits, overflow handling, exact fluid division, and rejected fractional results.
