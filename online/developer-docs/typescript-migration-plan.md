# TypeScript migration of the legacy code — COMPLETED

The generated TypeScript under `online/src/worker/legacy/ts/` is now the source of truth
for online vZome, edited directly.  JSweet, the monolithic bundles, and the Java build for
`online/` have all been removed.

This file previously held a *plan*.  It is kept as a record of what was actually done,
because the outcome differs from the plan in ways worth knowing.

## What the plan got wrong

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

## What was actually done

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

## Three bugs the migration exposed

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

## Where to look now

- `online/src/worker/legacy/ts/README.md` — what to know before editing the tree
- `AGENTS.md`, *Coding Conventions* — the Java ↔ TypeScript mirroring rule, which is now
  the load-bearing process constraint
- `online/developer-docs/povray-export-defects.md` — an open defect caused by one of the
  minimal `java/**` stubs diverging from real JDK behaviour
