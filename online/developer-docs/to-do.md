# Online vZome — to-do

Known work, deferred deliberately.  Each item records enough to pick it up cold.

---

## DONE — Resource flow from core to online is automated

*Both the "missing `printable` VEFs" and "automate the resource flow" items are resolved;
kept here as a record of what changed and why.*

`cicd/online.bash` gained `syncShapeResources()`, which runs before `marshallResources` in
both `initJs` and `productionBuild`.  It copies the shape `.vef` files from
`core/src/main/resources/com/vzome/core/{parts,math/symmetry}` into
`src/worker/legacy/resources/`, and **generates** the 28 `index.js` files that enumerate
them for esbuild.  That tree is now a gitignored build artifact; core is the single source
of truth.

Only `.vef` is copied.  The `.vZome` design sources and `.zomic` legacy exports beside them
in core are historic — never loaded by online — and stay in core, where the three formats
remain together as a lineage.

Consequences worth knowing:

- **Four whole packages were being dropped**, not two files: `printable`, `bigzome`,
  `vienne2` and `vienne3` had their `.vef` files present in the web tree but **no
  `index.js`**, so nothing bundled them.  Generating the indexes fixed all four at once.
- POV parity went **93/112 → 101/112** (83% → 90%).  The seven designs listed in the old
  item all match Java now; `Sicosahedral_turquoise_3__5_` is byte-identical across Java
  and JS in `greatRedGreenOrangeIcosa`.
- **The `heptagonAntiprism/` rename is gone.**  The generated tree uses core's real
  `heptagon/antiprism/` path; the top-level `index.js` still keys it `'heptagon/antiprism'`,
  which is what `ExportedVEFShapes` asks the `ResourceLoader` for.
- `moves.sh` moved to `oculus/moves.sh`.  It was never a sync tool — it is a dormant 2020
  Unity/Oculus asset converter (see the provenance header in the file itself).

Two output folders, easily confused:

| | `syncShapeResources` | `marshallResources` |
|---|---|---|
| output | `src/worker/legacy/resources/` | `serve/app/classic/resources/` |
| content | shape `.vef` + generated `index.js` | desktop resources + core `exporters/*` |
| consumed by | esbuild, bundled as data URLs | `fetch()` at runtime, over HTTP |
| enumerated by | the generated `index.js` files | generated `resourceIndex` in `revision.js` |

Shape resources are still **bundled**, not fetched.  Switching them to URL fetches was
considered and deliberately rejected: it would make `initialize()` block on 150 HTTP
round-trips, needs a service-worker cache route (the current routes match on
`request.destination`, which is `""` for these), and the hardcoded `/app/classic/resources/`
path is resolved against `window.location`, which is wrong for web components embedded on
third-party pages.

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

## Shared worker / lazy shape loading — still blocked

Picking this up cold: the goal is to stop every worker paying for every shape, so that
independent model loads can be parallelised across workers and shapes can be loaded once in
a **shared** worker.

**Half of this already shipped.**  Field applications are constructed lazily, per field, on
demand — `fieldRegistry` / `getFieldApp()` in `src/worker/legacy/core.js`, merged as
`a94e5492a` ("Load AlgebraicField/Symmetry apps lazily, per field, on demand").

**The shapes half did not.**  The attempt is preserved in commit `3a820f63a`, *"WIP Lazy
field initialization and shapes fetching"* — which is **not an ancestor of `main`**, so it
will not turn up in normal history browsing.  Its commit message is the best statement of
the problem and is worth reading in full (`git show 3a820f63a`).  The two blockers:

1. **Chicken-and-egg on `orbitSource`.**  The initial `orbitSource` is only known inside
   `documentFactory()`, and that is also where the first ball gets rendered.  So there is no
   point at which you know which shapes you need *before* you need them.

2. **No static definition of which *directions* a package supplies.**  `ExportedVEFShapes`
   falls back to the fallback shapes for any direction it cannot find, and nothing records
   which directions (including `connector`) are genuinely present as opposed to
   deliberately falling back.

### The resource-automation work did NOT unblock this

Worth stating plainly, because it looks like it should have.  The generated `index.js` files
do record which `.vef` files each package contains — but that is a **file** manifest, not an
**orbit** manifest.  Knowing that `printable` provides `apple.vef` and `turquoise.vef` does
not tell you which orbits of a given symmetry those serve, nor which orbits legitimately
fall back.  Blocker 2 is untouched, and blocker 1 was never related to resources at all.

The one thing that did change: because shapes are still **bundled** rather than fetched,
"which shapes exist" is still answered at build time, so 404-probing is not on the table.
That option was explicitly rejected (see the resource-flow item above).

### Where to pick it up

Session `4ebc30bd-143e-410f-a683-d6f6a55ab472` holds the context — it opens with exactly
this question, contains the lazy-fields design that shipped, and the orientation-bug
history that followed it (`ori-crash-fix`, the `orientationIndex out of range` work).
Prefer resuming there over starting cold.

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
