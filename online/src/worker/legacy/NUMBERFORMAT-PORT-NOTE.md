# Port note: the `java.text.NumberFormat` stand-in

## What changed here

`core-java.js` contains a hand-written stand-in for `java.text.NumberFormat`.  Until now
`setMaximumFractionDigits()` / `setMinimumFractionDigits()` were **empty stubs** and `format()`
was just string concatenation:

```js
setMaximumFractionDigits(i) { }
format(x) { return '' + x; }
```

So every exporter that asks for a fixed number of digits silently got the full float instead.
POV-Ray asks for 8 (`FORMAT.setMaximumFractionDigits(8)`), and produced
`4.236067771911621` where Java writes `4.23606777`.  That made *every* exported file differ
from its Java counterpart, which defeats diff-based regression testing.

The class now implements the real semantics: round to at most `maximumFractionDigits`, pad to at
least `minimumFractionDigits`, and trim trailing zeros in between.

**This is not POV-Ray-specific.** 12 call sites in `core-java.js` construct a `NumberFormat`
(`DaeExporter`, `OffExporter`, `VRMLExporter`, `RealVector`, and others), so they were all
affected and are all fixed by this change.

## Why this note exists

On the `adopt-typescript` branch, `core-java.js` is **deleted** and this class moves to its own
file:

    online/src/worker/legacy/from-java/java/text/NumberFormat.ts

That file still carries the original empty stub.  Because the file this fix lives in disappears
in that migration, **the fix will not conflict -- it will simply be lost**, and the bug returns
silently.

## What to do when merging with the TypeScript migration

Port the implementation from `core-java.js` (search for "A minimal stand-in for
java.text.NumberFormat") into `from-java/java/text/NumberFormat.ts`, keeping the TypeScript
types.  The behavior to preserve:

- `setMaximumFractionDigits(i)` / `setMinimumFractionDigits(i)` actually store their argument,
  and keep min <= max
- default `maximumFractionDigits` is 3, matching `java.text.NumberFormat`
- `format(x)` rounds with `toFixed(maximumFractionDigits)`, then trims trailing zeros but never
  below `minimumFractionDigits`
- non-finite values fall back to plain string conversion
- grouping separators are never emitted (every caller writes machine-readable geometry)

To verify after porting, export one design through both Java and Javascript and compare:

    ./gradlew core:batchPov -PpovInput=<design> -PpovOutput=/tmp/java-pov
    cd online && node scripts/run-batch-pov.mjs <design> /tmp/js-pov

All the geometry -- `object`, `#declare S`, `#declare trans`, `#declare color` and `triangle`
lines -- must match exactly.  See also `online/scripts/batch-pov.mjs` for the one known and
deliberate difference (light directions, which need a camera the headless run does not have).
