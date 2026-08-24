/**
 * Batch POV-Ray export, using the transpiled vZome core, for regression testing.
 *
 * This is the Javascript counterpart to core's BatchPovExporter, and it takes the same
 * arguments, so the two can be pointed at the same designs and their output compared:
 *
 *   node scripts/batch-pov.mjs <inputRoot> <outputRoot> [--compare <goldenRoot>]
 *
 * Without --compare it records ".pov" files into a mirrored folder hierarchy; with it, each
 * export is compared against the corresponding file in the golden tree and the process exits
 * non-zero if anything differs or fails.
 *
 * Folders may contain a "skipKnownFailures.testsuite" file listing exactly what to test there;
 * anything it omits is skipped as a known failure.  This matches the Java harness.
 *
 * Running the legacy code outside a browser needs three small accommodations, all of them
 * because j4ts (the Java-to-Javascript runtime) assumes a browser or a Rhino-style shell:
 *   - `require` and `process` must be visible as globals, or j4ts decides it is not on Node
 *     and replaces `console` with a stub that has no `log`
 *   - `fetch` is used to load classpath resources (shape VEFs, the POV-Ray preamble); here it
 *     reads the same files from disk instead
 *   - the whole thing has to be bundled, because the legacy sources use extensionless imports
 *     and import ".vef" files as modules
 * The bundling is handled by run-batch-pov.mjs, which esbuilds this file and then runs it.
 */

import fs from 'fs';
import path from 'path';

import { resourceIndex } from '../src/revision.js';
import { initialize, loadAndInjectResource } from '../src/worker/legacy/core.js';
import { Interpreter, RenderHistory, Step } from '../src/worker/legacy/interpreter.js';
import { export3dDocument } from '../src/worker/legacy/exporters.js';

const SKIP_FILE = 'skipKnownFailures.testsuite';

// Kept in step with defaultCamera() / defaultLighting() in online/src/viewer/context/camera.jsx.
const INITIAL_DISTANCE = 108;

const DEFAULT_CAMERA = {
  distance: INITIAL_DISTANCE,
  near: INITIAL_DISTANCE * ( 0.1 / INITIAL_DISTANCE ),
  far: INITIAL_DISTANCE * 2.0,
  width: INITIAL_DISTANCE * 0.45,
  lookAt: [ 0, 0, 0 ],
  up: [ 0, 1, 0 ],
  lookDir: [ 0, 0, -1 ],
  perspective: true,
  default: true,
};

const DEFAULT_LIGHTING = {
  backgroundColor: '#8CC2E7',
  ambientColor: '#333333',
  directionalLights: [
    { direction: [  1, -1, -0.3 ], color: '#FDFDFD' },
    { direction: [ -1,  0, -0.2 ], color: '#B5B5B5' },
    { direction: [  0,  0, -1   ], color: '#303030' },
  ],
};
// The runner passes the online/ folder, because after bundling this file no longer knows
// where it lives on disk.
const ONLINE_ROOT = process.env.VZOME_ONLINE_ROOT ?? path.resolve( '.' );
const RESOURCE_ROOT = path.join( ONLINE_ROOT, 'serve/app/classic/resources' );

// The legacy resource loader fetches over HTTP in the browser; read from disk instead.
globalThis.fetch = async ( url ) => {
  const name = decodeURIComponent( String( url ) .replace( /^.*\/resources\//, '' ) );
  const file = path .join( RESOURCE_ROOT, name );
  if ( ! fs .existsSync( file ) )
    return { ok: false, status: 404, text: async () => '' };
  return { ok: true, status: 200, text: async () => fs .readFileSync( file, 'utf-8' ) };
};

const isDesign = name => name .toLowerCase() .endsWith( '.vzome' );

/**
 * Collects designs under a folder, honoring the "skipKnownFailures.testsuite" allow-list.
 */
const collectDesigns = ( folder, into = [] ) =>
{
  const suite = path .join( folder, SKIP_FILE );
  if ( fs .existsSync( suite ) ) {
    for ( const raw of fs .readFileSync( suite, 'utf-8' ) .split( '\n' ) ) {
      const line = raw .trim();
      if ( ! line || line .startsWith( '#' ) )
        continue;
      const entry = path .join( folder, line );
      if ( ! fs .existsSync( entry ) )
        console .error( `  listed in ${suite} but not found: ${line}` );
      else if ( fs .statSync( entry ) .isDirectory() )
        collectDesigns( entry, into );
      else if ( isDesign( entry ) )
        into .push( entry );
    }
    return into;
  }
  for ( const name of fs .readdirSync( folder ) ) {
    const entry = path .join( folder, name );
    const stat = fs .statSync( entry );
    if ( stat .isDirectory() )
      collectDesigns( entry, into );
    else if ( isDesign( name ) )
      into .push( entry );
  }
  return into;
}

/** Loads one design and returns its POV-Ray export, replaying the full edit history. */
const exportDesign = ( core, xml ) =>
{
  const design = core .parse( xml );
  if ( design.field.unknown )
    throw new Error( `Field "${design.field.name}" is not supported.` );

  // Replay the whole edit history, as initializeDesign() does when loading normally.
  const renderHistory = new RenderHistory( design, true );
  new Interpreter( design, renderHistory ) .interpret( Step.DONE );
  design.history .goToEdit( design.targetEdit );

  // Older designs carry no saved camera or lighting, so fall back to the same defaults the
  // viewer uses (see defaultCamera / defaultLighting in viewer/context/camera.jsx).  Without
  // this, every design predating those being saved fails to export.
  const camera = design.camera ?? DEFAULT_CAMERA;
  const lighting = design.lighting ?? DEFAULT_LIGHTING;
  // NOTE: the editor's export path sets lighting.useWorldDirection = true (see
  // controllers/editor.js, case 'pov'), because POV-Ray wants light directions in world
  // coordinates.  We deliberately do NOT set it here: worldDirection is computed on the client
  // from the live camera (viewer/context/viewer.jsx calls mapViewToWorld), and there is no
  // camera context in a headless run.  The consequence is that the three light_source lines
  // differ from a Java export of the same design; everything else -- all the geometry -- matches
  // exactly.  That is fine for regression testing, which compares a JS export against an earlier
  // JS export, but it is why a Java baseline and a JS baseline are not byte-identical.
  return export3dDocument( design, camera, lighting, { format: 'pov', height: 1080, width: 1920 } );
}

/** Reports the first differing line, with a window around the first differing character. */
const describeDifference = ( expected, actual ) =>
{
  const a = expected .split( '\n' ), b = actual .split( '\n' );
  const limit = Math .min( a.length, b.length );
  for ( let i = 0; i < limit; i++ ) {
    if ( a[ i ] !== b[ i ] ) {
      let column = 0;
      const shared = Math .min( a[ i ].length, b[ i ].length );
      while ( column < shared && a[ i ][ column ] === b[ i ][ column ] ) column++;
      const excerpt = line => {
        const start = Math .max( 0, column - 60 ), end = Math .min( line.length, column + 60 );
        return ( start > 0 ? '...' : '' ) + line .slice( start, end ) + ( end < line.length ? '...' : '' );
      };
      return `line ${i + 1}, column ${column + 1}:\n      golden: ${excerpt( a[ i ] )}\n      actual: ${excerpt( b[ i ] )}`;
    }
  }
  return `golden has ${a.length} lines, actual has ${b.length}`;
}

const main = async () =>
{
  const args = process.argv .slice( 2 );
  let golden = null;
  const positional = [];
  for ( let i = 0; i < args.length; i++ ) {
    if ( args[ i ] === '--compare' ) golden = path .resolve( args[ ++i ] );
    else positional .push( args[ i ] );
  }
  if ( positional.length < 2 ) {
    console .error( 'usage: batch-pov.mjs <inputRoot> <outputRoot> [--compare <goldenRoot>]' );
    process .exit( 2 );
  }
  const input = path .resolve( positional[ 0 ] );
  const output = path .resolve( positional[ 1 ] );
  if ( ! fs .existsSync( input ) ) {
    console .error( `input does not exist: ${input}` );
    process .exit( 2 );
  }

  const base = fs .statSync( input ) .isDirectory() ? input : path .dirname( input );
  const designs = ( fs .statSync( input ) .isDirectory() ? collectDesigns( input ) : [ input ] ) .sort();
  if ( ! designs.length ) {
    console .error( `No .vZome files found under ${input}` );
    process .exit( 1 );
  }

  console .log( `${golden ? 'Comparing' : 'Exporting'} ${designs.length} designs` );
  console .log( `  input:  ${input}` );
  console .log( `  output: ${output}` );
  if ( golden ) console .log( `  golden: ${golden}` );

  await Promise .all( resourceIndex .map( p => loadAndInjectResource( p, `/resources/${p}` ) ) );
  const core = await initialize();

  let matched = 0, recorded = 0;
  const differed = [], failed = [], missing = [];

  for ( const [ index, design ] of designs .entries() ) {
    const relative = path .relative( base, design ) .replace( /\.vZome$/i, '.pov' );
    try {
      const text = exportDesign( core, fs .readFileSync( design, 'utf-8' ) );
      const target = path .join( output, relative );
      fs .mkdirSync( path .dirname( target ), { recursive: true } );
      fs .writeFileSync( target, text );

      if ( ! golden )
        recorded++;
      else {
        const expected = path .join( golden, relative );
        if ( ! fs .existsSync( expected ) )
          missing .push( { relative, detail: expected } );
        else {
          const want = fs .readFileSync( expected, 'utf-8' );
          if ( want === text ) matched++;
          else differed .push( { relative, detail: describeDifference( want, text ) } );
        }
      }
    } catch ( error ) {
      failed .push( { relative, detail: `${error?.constructor?.name}: ${error?.message}` } );
    }
    if ( ( index + 1 ) % 25 === 0 || index + 1 === designs.length )
      console .log( `  ${index + 1}/${designs.length}` );
  }

  console .log();
  if ( golden ) {
    console .log( `  matched:        ${matched}` );
    console .log( `  DIFFERED:       ${differed.length}` );
    console .log( `  missing golden: ${missing.length}` );
  } else
    console .log( `  exported:       ${recorded}` );
  console .log( `  failed:         ${failed.length}` );

  for ( const r of differed ) console .log( `  DIFFERED  ${r.relative}  ${r.detail}` );
  for ( const r of failed )   console .log( `  FAILED    ${r.relative}  ${r.detail}` );

  fs .mkdirSync( output, { recursive: true } );
  const writeReport = ( name, rows ) => {
    const file = path .join( output, name );
    if ( ! rows.length ) { if ( fs .existsSync( file ) ) fs .unlinkSync( file ); return; }
    fs .writeFileSync( file, rows .map( r => `${r.relative}  ${r.detail ?? ''}` ) .join( '\n' ) + '\n' );
    console .log( `  wrote ${file}` );
  };
  writeReport( 'failures.txt', failed );
  if ( golden ) {
    writeReport( 'regressions.txt', differed );
    writeReport( 'missing-golden.txt', missing );
  }

  // Missing golden files are not a regression, just designs the baseline does not cover yet.
  process .exit( failed.length || ( golden && differed.length ) ? 1 : 0 );
}

main() .catch( e => { console .error( 'FAILED:', e?.stack ?? e ); process .exit( 1 ); } );
