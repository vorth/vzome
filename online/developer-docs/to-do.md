# Online vZome — to-do

Known work, deferred deliberately.  Each item records enough to pick it up cold.

---

## Ship the two missing `printable` shape VEFs

**Largest remaining cause of Java/JavaScript POV-Ray differences: 7 of the 19 designs that
still disagree.**

The web bundles 144 of the 147 part-shape VEFs under
`src/worker/legacy/resources/com/vzome/core/parts/`, loaded through esbuild's `dataurl`
loader.  Only two are genuinely absent:

    core/src/main/resources/com/vzome/core/parts/printable/apple.vef
    core/src/main/resources/com/vzome/core/parts/printable/turquoise.vef

(A third, `dodecagon3d/connector-old.vef`, is obsolete.  The four
`heptagon/antiprism/*.vef` that also appear missing are not — they were renamed to
`heptagonAntiprism/` on the web.)

This matters more than two files should, because `printable` is the **default** icosahedral
geometry:

```ts
const printableShapes = new ExportedVEFShapes( null, "printable", "printable",
                                               this.symmetry, icosadefaultShapes );
this.setDefaultGeometry( printableShapes );
```

That last constructor argument is a **fallback**.  When the web cannot find
`printable/turquoise.vef` it silently substitutes the default shape, so the export succeeds
and looks plausible while using different geometry from Java.  Confirmed concretely:
`Sicosahedral_apple_1__5_3_` has 34 triangles in the Java export and 24 here.

Affected designs include `greatRedGreenOrangeIcosa`, `turqouiseFun`,
`squashed-snubDodec2`, `turquoiseOrangeMaroonGreen`, `gosset11-63-117-yellow`,
`testAxialStretch`, and `PanelCentroids-and-PanelPerimeters`.

Fix: add the two files alongside the other 144 and register them in the neighbouring
`index.js`, then re-run the POV comparison to confirm the count drops.

---

## Automate the resource flow from desktop to online

Related to the item above, and the reason it happened.

`cicd/online.bash marshallResources` already copies **some** resources automatically —
`desktop/src/main/resources/*` and `core/.../exporters/*` into `serve/app/classic/resources`
at build time.  But the part-shape VEFs under `src/worker/legacy/resources/` are
**hand-maintained**: someone copied 144 files across once and has to remember to repeat it
whenever `core` gains or renames a shape.

That is how `printable/apple.vef` and `printable/turquoise.vef` came to be missing, and how
`heptagon/antiprism/` and `heptagonAntiprism/` came to disagree on layout.

Worth designing a step that derives the web copy from `core/src/main/resources`, so the two
cannot drift.  Watch out for the directory-naming difference, and for the fact that the web
copies are imported by generated `index.js` files, not discovered at runtime.

---

## Shape keys do not include the style name

`AbstractShapes` builds two kinds of content-derived shape key, and they are not symmetric:

```ts
// connector -- includes the shapes package
this.mConnectorGeometry.setShapeKey( this.mSymmetry.getName() + ":" + this.mPkgName + ":ball" );

// strut -- does NOT
lengthShape.setShapeKey( this.mSymmetry.getName() + ":" + orbit.getName() + ":"
                       + length.toString( AlgebraicField.DEFAULT_FORMAT ) );
```

So two different shapes packages (styles) produce **the same strut key** for the same orbit
and length, even though the geometry differs.  These keys are the web geometry cache and
instancing keys as well as the POV-Ray dedup keys, so a collision means unrelated shapes
share a cache entry.

Look into whether the strut key should carry `mPkgName` too.  Note this is a
cross-platform contract: any change must be made on both sides, and it changes the
`#declare S…` names in POV-Ray output, so the golden tree would need regenerating.

---

## Understand the `WARNING: No-op edit:` logs during `interpret()`

`interpreter.js` warns whenever a replayed edit reports `isNoOp()`:

```js
if ( edit .isNoOp() )
  // TODO: get stricter for more modern designs, where this should never happen
  console.log( 'WARNING: No-op edit:', edit.constructor.name );
```

Across one run of the 116 regression designs that fires **895 times**, in 9 kinds:

| count | edit |
|------:|------|
| 468 | `SymmetryCenterChange` |
| 212 | `SymmetryAxisChange` |
| 160 | `DeselectAll` |
| 16 | `StrutCreation` |
| 14 | `ValidateSelection` |
| 13 | `_CommandEdit` |
| 10 | `_JoinPoints` |
| 1 | `ShowPoint` |
| 1 | `ColorManifestations` |

The first three look benign — genuinely idempotent operations recorded by old UIs.  The
tail is more interesting: a no-op `StrutCreation` or `_JoinPoints` suggests an edit that was
*meant* to build something and did not.  Worth checking whether those correlate with the
designs that still differ from Java.

The existing TODO in the code is the right instinct: this should be an error for modern
designs and only tolerated for migrated ones.

---

## Get the regression suite running on the NAS

The POV-Ray comparison is the only real end-to-end check the online code has, and it is
currently a manual, ~25-minute local run.  Worth hosting somewhere it can run unattended
against both the Java and JavaScript paths.

See the "POV-Ray regression suite" memory, or `PORT-NOTES.md`, for the invocations.  Notes
for whoever sets this up:

- The Java side shards (6 workers, 300s timeout); the JavaScript runner does not, and takes
  roughly 25 minutes for 116 designs single-threaded.
- `--geometry-only` is required when comparing across platforms: camera and lighting cannot
  match headlessly, because `worldDirection` is computed from the live client camera.
- The golden tree is generated, not committed.  Decide whether the NAS regenerates it from
  Java each run (slower, always current) or caches it (faster, can go stale after a
  deliberate output change).
- 4 designs currently fail outright on the JavaScript side and would need an allow-list:
  two "Zomic module was not loaded", one `AffinePentagon`, one `SelectManifestation`.
