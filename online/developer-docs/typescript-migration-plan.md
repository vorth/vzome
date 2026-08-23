# TypeScript migration

**Phase 1 (legacy transpiled code): COMPLETE.**  **Phase 2 (the rest of `online/`): proposed, not started.**

---

## Phase 1 — the legacy code (done)

The generated TypeScript under `online/src/worker/legacy/from-java/` is now the source of truth
for online vZome, edited directly.  JSweet, the monolithic bundles, and the Java build for
`online/` have all been removed.

This file previously held a *plan*.  It is kept as a record of what was actually done,
because the outcome differs from the plan in ways worth knowing.

### What the plan got wrong

The plan proposed a two-phase approach: first make the code compile as-is with
`"module": "None"` plus `outFile`, then migrate to ES modules incrementally.  Phase 1 was
implemented and verified, then **deliberately discarded**: it produces a green `tsc` but no
bundling benefit at all, since `outFile` just reproduces the monolith and every entry point
still pays for the whole 2.9MB.

Three other assumptions turned out to be false:

- *"The j4ts types aren't declared anywhere in `.d.ts` form"* — they were.  `j4ts.d.ts`
  already existed in the `vorth/j4ts` fork; only the runtime `bundle.js` had ever been
  copied into this repo.  Vendoring the existing file, with one edit making
  `Iterable.forEach` / `Iterator.forEachRemaining` optional, fixed 11 of the 27 errors on
  its own.  No hand-written declarations were needed.
- *"If you add even one `import` or `export`, TypeScript re-interprets the file as a
  module, breaking all the cross-namespace references"* — true in isolation, but not an
  obstacle: converting **every** file at once, with a codemod that rewrote each
  cross-namespace reference into an import, avoids the mixed state entirely.
- The plan assumed nested namespaces were the thing preventing compilation.  They were
  not.  Both the monolith and the per-class tree compiled to exactly the same 27 errors
  under `"module": "none"`; those 27 were ordinary type errors, the same ones JSweet had
  always reported.  Namespaces blocked *bundling*, not *compilation*.

### What was actually done

1. Vendored `j4ts.d.ts` alongside the runtime bundle, plus a `bundle.d.ts` re-exporting
   the ambient namespaces as module bindings, and a small `shims.d.ts` for three types
   j4ts lacks entirely.  Errors: 27 → 11.
2. Ran a one-time codemod, `online/scripts/ts-to-esm.mjs`, converting all 433 per-class
   files from namespaces to ES modules with explicit imports.  Errors: 11 → 0.
3. Rewired the hand-written consumers to import named classes instead of pulling in the
   whole `com` namespace object, which is what actually enables tree-shaking.
4. Deleted the monolithic `core-java.js` and `ts/core-java.ts`, the JSweet scripts and
   Gradle project, the `jsweet.yml` workflow, and `online/src/worker/java/`.

`dist/modules/vzome-legacy.js` — the chunk fetched to open a design — went from
**3,788,215 to 1,761,193 bytes (-53.5%)**, and the zomic interpreter split into its own
323KB chunk instead of loading with every design.

### Three bugs the migration exposed

Each was invisible while everything shared one global scope:

- **Seven Java static initializers** were run by the monolith's epilogue and by nothing
  else.  `XmlSymmetryFormat` registers every `.vZome` format namespace there, so without
  it `getFormat()` returned null and *every* design failed to open.  The 106 lazy
  `_$LI$()` primes in the same epilogue turned out to be genuinely optional; the 7 are not.
- **`java.util.Properties` and `java.io.StringWriter`** are not part of the j4ts candy —
  vZome transpiles its own.  Hand-written JS reaching them through `java.*` worked only
  because the monolith merged both sources into one namespace.
- **Module cycles whose base class is in the cycle** are fatal in ESM
  (`Class extends value undefined`).  Only one existed: `AbstractCommand` referenced two
  of its own subclasses for three string constants.

### Where to look now

- `online/src/worker/legacy/from-java/README.md` — what to know before editing the tree
- `AGENTS.md`, *Coding Conventions* — the Java ↔ TypeScript mirroring rule, which is now
  the load-bearing process constraint
- `online/developer-docs/povray-export-defects.md` — an open defect caused by one of the
  minimal `java/**` stubs diverging from real JDK behaviour

---

## Phase 2 — the rest of `online/` (proposed)

Phase 1 made TypeScript *possible* in this project.  Phase 2 is about making it *useful*
for the code we actually write by hand.  It should be gradual and opportunistic: there is
no deadline, no big-bang rewrite, and no value in converting files just to raise a
percentage.

### The surface we are talking about

Excluding the legacy tree, `online/src` holds ~176 JS/JSX files and ~60K lines, but most
of that bulk is generated or vendored — the j4ts bundle (31K lines), the ANTLR-generated
Zomic parser (~3.4K), `VRMLLoader.js` (~3K), and a generated CSS module.  **The
hand-written surface is ~169 files and ~22K lines**, which is a very tractable target.

### It already works — no build changes required

This was verified, not assumed:

- **esbuild transpiles `.ts` natively**, and `scripts/esbuild-solid-plugin.mjs` already
  matches `/\.(t|j)sx$/` and already loads `@babel/preset-typescript`.  A `.tsx` Solid
  component compiles through the real production config today.
- Renaming a file `.js` → `.ts` therefore needs **no config edit at all**.  98% of
  JS-module relative imports here already carry an explicit `.js` specifier (586 of 598),
  and TypeScript resolves a `.js` specifier to the `.ts` source, so *importers do not
  change when a file is renamed*.  That is what makes this safely incremental.  Only a
  dozen extensionless imports exist (e.g. `./drag`, `../fields/common`); they resolve
  fine either way, but are worth normalising when touched.

Two gaps to close before starting, both one-liners in `online/tsconfig.json`:

1. `include` currently covers only `src/worker/legacy/from-java/**`.  New `.ts` outside that tree
   would compile via esbuild but never be typechecked — the worst of both worlds.  Add the
   rest of `src/`.
2. `jsx` is not set.  SolidJS needs `"jsx": "preserve"` with `"jsxImportSource": "solid-js"`
   for `.tsx` to typecheck (solid-js ships its own JSX types).

**Keep the legacy tree and new code at different strictness.**  The 433 transpiled modules
pass only under `strict: false`; flipping `strict: true` on them yields **4,216 errors**
(measured).  Chasing those is not worth it — they are machine output we mirror by hand, and
the errors are overwhelmingly JSweet's `any`-heavy calling conventions rather than real
defects.  New code deserves `strict: true`.  Use a second config
(`tsconfig.app.json`) with its own `include` and stricter options, and have `yarn typecheck`
run both, rather than dragging everything down to the legacy baseline.

### Third-party types

Mostly already good: `solid-js`, `solid-three`, `three-stdlib`, `@thisbeyond/solid-dnd`,
`txml`, `antlr4`, and `chevrotain` all ship declarations.

**`three` (0.184) ships none** — it has no `types` entry and no `.d.ts` in the package.
The viewer is the area that would benefit most from typing *and* the area that leans
hardest on three.js, so `@types/three` (version-matched) is a prerequisite for that work,
not an afterthought.

### Where the value actually is

Ranked by payoff, not by size.  The ordering reflects one idea: **types are worth most
where a contract crosses a boundary that the runtime does not check.**

#### 1. The worker ↔ client message protocol — highest value by a wide margin

`vzome-worker-static.js` handles **22 distinct message types** (`ACTION_TRIGGERED`,
`PREVIEW_STRUT_MOVE`, `SNAPSHOT_SELECTED`, …).  Everything crosses `postMessage`, so it is
structurally unchecked: a renamed field or a wrong payload shape produces no error
anywhere — it just silently does nothing.

`worker-client-scene-protocol.md` already says this in as many words: these failures cost
"significant time to track down because they fail *silently* (no console error, no thrown
exception, just wrong or missing rendering)."  That is the exact failure mode a
discriminated union eliminates at the keystroke.

**The send side is already centralised, which makes this remarkably cheap.**
`viewer/util/actions.js` is 94 lines holding 23 action creators, all funnelled through one
helper:

```js
const workerAction = ( type, payload ) => ( { type, payload } );
export const selectSnapshot = ( snapshot, load=defaultLoad ) =>
  workerAction( 'SNAPSHOT_SELECTED', { snapshot, load } );
```

Converting this one file to `.ts` types **19 of the 22** message types (the other three —
`BOM_REQUESTED`, `WINDOW_LOCATION`, `WORKER_PROBE` — are lifecycle/probe messages sent from
elsewhere), and every caller gets checked for free: `editor.jsx`, `buildplane.jsx` and the
rest call these creators rather than building message objects by hand, so **they need no
changes at all**:

```ts
type WorkerAction =
  | { type: 'SNAPSHOT_SELECTED'; payload: { snapshot: number; load: LoadFlags } }
  | { type: 'PREVIEW_STRUT_MOVE'; payload: { direction: Direction } }
  | ...
const workerAction = <T extends WorkerAction>( type: T['type'], payload: T['payload'] ): T => ...
```

The receive side is the `switch` in `vzome-worker-static.js`; once the union exists,
converting that file makes the compiler enforce that every case is handled and that each
branch destructures the right payload.

**This is the single highest-leverage change in Phase 2**: one small file, no runtime
behaviour change, and it covers the boundary where the bugs actually live.

#### 2. The scene records — shapes, instances, orientations

`worker/legacy/scenes.js` builds the objects the renderer consumes:

```js
{ id, position: [x,y,z], orientation, color, selected, shapeId, type, label }   // instance
{ id, name, vertices, faces }                                                    // shape
```

These flow worker → client alongside the protocol above, and `symmetry-geometry.jsx` /
`symmetry-renderer.js` destructure them extensively.  Typing these records is what makes
the renderer legible to an editor and to an LLM: today, `instance.` offers no completion
anywhere, and nothing catches a field that quietly stopped being emitted.

Note the trap documented in `povray-export-defects.md`: `shapeId` (a random UUID) and
`shapeKey` (a content hash) are different identifiers used for different purposes, and
confusing them has already caused one real bug.  A named type per identifier —
`type ShapeId = string & { __brand: 'ShapeId' }` — makes that class of mistake impossible
rather than merely documented.

#### 3. SolidJS context values

There are **11 context providers** (`scene.jsx` 347 lines, `camera.jsx` 321,
`editor.jsx` 306, `worker.jsx` 137, …), and the project's stated design philosophy is
*prefer context over props*.

That philosophy makes typing them unusually valuable: when data arrives via context rather
than props, **nothing at the consumption site says what is available**.  An LLM reading a
component sees `useCamera()` and has to go find the provider to learn the shape; an editor
offers no completion at all.  Typing the context value fixes both at once, and it is a
small, self-contained edit per provider — the value object is usually constructed in one
place.

Start with `worker.jsx` (it is the protocol's client edge, so it pairs naturally with #1),
then `scene.jsx` and `camera.jsx`.

#### 4. `worker/fields/common.js` and the field modules

384 lines implementing exact algebraic arithmetic, with a small, sharply-defined API
(`createNumberFromPairs`, `simplify`, …) consumed by `jsweet2js.js` across the
JS ↔ transpiled-Java boundary.  Small, pure, heavily reused, and semantically fussy —
`number[]` versus `bigint[]` versus "pairs" is exactly the confusion a type catches.
Good early win: high leverage, low risk, no UI involvement.

### What to leave alone

- **`worker/legacy/from-java/**`** — already TypeScript; do not tighten it (see above).
- **Generated and vendored files** — the ANTLR Zomic parser, `VRMLLoader.js`, the j4ts
  bundle, generated CSS.  Converting generated output is pure cost.
- **`jsweet2js.js` and `core.js`** — the hand-written bridge into the transpiled world.
  They are dense with `any`-shaped interop and JSweet calling conventions; typing them
  honestly would mean a lot of `any`, and typing them dishonestly would be worse than
  leaving them plain.  Revisit only after the protocol and scene types exist, since those
  are what would give the bridge something real to be typed *against*.
- **Leaf `.jsx` components** with simple props.  Converting them is easy, which is exactly
  why it is tempting — but the payoff is small and it burns the appetite for the work that
  matters.  Let these follow naturally when a file is being edited anyway.

### Suggested order

1. `tsconfig` split: legacy stays loose, new code strict.  Add the `jsx` settings.
2. **`viewer/util/actions.js` → `.ts`** with the `WorkerAction` union.  94 lines, no
   callers change, covers 19 of 22 messages.  This is the one to do first.
3. `worker/vzome-worker-static.js` → `.ts`, so the receiving `switch` is checked against
   that union.  Larger (654 lines) but mechanical, and it closes the loop.
4. **Scene record types** — instance, shape, orientation, plus branded `ShapeId` /
   `ShapeKey`.  A `.d.ts` is enough at first; `scenes.js` can adopt it later.
5. `worker/fields/common.js` → `.ts`.  Small, pure, high-reuse.
6. `@types/three`, then `viewer/context/worker.jsx` and the other context providers.
7. Thereafter: opportunistic.  Convert a file when you are already editing it and the types
   would have helped.

### How to tell it is working

Not by counting converted files.  The signals worth watching:

- `yarn typecheck` catches a protocol mismatch **before** you reload the browser.
- An editor autocompletes `instance.` and `useCamera().` correctly.
- A renamed payload field produces a red squiggle at every call site instead of a silent
  no-op.

If a conversion does not move one of those, it was probably not worth doing yet.
