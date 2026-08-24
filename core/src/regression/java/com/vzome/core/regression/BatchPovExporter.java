
package com.vzome.core.regression;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import com.vzome.api.Application;
import com.vzome.api.Document;
import com.vzome.api.Exporter;
import com.vzome.core.commands.Command;

/**
 * Exports every ".vZome" file under a folder to POV-Ray format, writing the results into a
 * mirrored folder hierarchy.
 *
 * Two modes:
 *
 *   record  (no golden folder)  -- write the ".pov" files.  This produces the golden baseline.
 *   compare (golden folder set) -- write the ".pov" files, then compare each one against the
 *                                  corresponding file in the golden tree, and exit non-zero if
 *                                  anything differs or fails.  This is the regression test.
 *
 * Comparison is plain equality, which only works because POVRayExporter now emits its
 * manifestations in a deterministic, content-derived order.
 *
 * PARALLELISM IS BY PROCESS, NOT BY THREAD.  Loading a design cannot be done concurrently
 * within one JVM: XmlSaveFormat instances are cached in a static map (XmlSaveFormat.FORMATS)
 * but carry per-document state -- initialize() sets the field, scale and multiplier that are
 * then read throughout history replay -- so two simultaneous loads corrupt each other, and
 * designs fail to open at random.  Loading is also ~96% of the runtime, so serializing just
 * the load would leave almost nothing to parallelize anyway (measured: 35.2s with 6 threads
 * and a load lock, versus 36.6s single-threaded).
 *
 * Instead, --split/--shard let several independent JVMs each take a slice of the files.
 * Statics are not shared between processes, so this is both correct and genuinely parallel.
 * Use the "batchPov" Gradle task, which forks the workers for you.
 *
 * @author Scott Vorthmann
 */
public class BatchPovExporter
{
    private static final String VZOME_SUFFIX = ".vzome";  // compared lowercased

    /**
     * A folder containing this file lists exactly what should be tested there; anything omitted
     * is a known failure being skipped.  The name and semantics come from the existing collection.
     */
    private static final String SKIP_FILE = "skipKnownFailures.testsuite";

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    /** One Application for the whole process; it is reused for every design. */
    private final Application application = new Application( new Command.FailureChannel()
    {
        @Override
        public void reportFailure( Command.Failure f )
        {
            // Recorded per-file by the caller; a failure here is not fatal to the run.
            System .err .println( "  failure: " + f .getMessage() );
        }
    } );

    enum Status { MATCHED, DIFFERED, RECORDED, MISSING_GOLDEN, FAILED }

    static class Result
    {
        final Path relative;
        final Status status;
        final String detail;

        Result( Path relative, Status status, String detail )
        {
            this .relative = relative;
            this .status = status;
            this .detail = detail;
        }
    }

    private final Path inputRoot, outputRoot, goldenRoot;

    /** This process handles the files whose position satisfies (i % split) == shard. */
    private final int shard, split, timeoutSeconds;

    /**
     * When set, comparison ignores the camera and lighting preamble, so a difference is only
     * reported for the geometry -- which is what the edit history actually produces.  Camera
     * and lighting are viewing preferences, and the Javascript exporter cannot reproduce the
     * light directions headlessly, so comparing them is noise.
     */
    private boolean geometryOnly;

    BatchPovExporter( Path inputRoot, Path outputRoot, Path goldenRoot, int shard, int split, int timeoutSeconds )
    {
        this .inputRoot = inputRoot;
        this .outputRoot = outputRoot;
        this .goldenRoot = goldenRoot;
        this .shard = shard;
        this .split = split;
        this .timeoutSeconds = timeoutSeconds;
    }

    /**
     * The base folder that relative paths are computed against.  When a single file is given,
     * that is its parent, so the output is a single file rather than a deep tree.
     */
    private Path baseFolder()
    {
        return Files .isDirectory( this .inputRoot ) ? this .inputRoot : this .inputRoot .getParent();
    }

    private List<Path> collectDesigns() throws IOException
    {
        if ( ! Files .isDirectory( this .inputRoot ) )
            return Collections .singletonList( this .inputRoot );

        List<Path> designs = new ArrayList<>();
        collectFolder( this .inputRoot, designs );
        // Sort, so progress output and reports read in a predictable order.
        Collections .sort( designs, Comparator .comparing( Path::toString ) );
        return designs;
    }

    /**
     * Walks one folder, honoring the "skipKnownFailures.testsuite" convention that the historical
     * collection already uses (and that TestVZomeFiles.Collector implements for the JUnit-XML
     * harness).  When such a file is present, it acts as an ALLOW-list: only the files and
     * subfolders it names are visited, so any sibling it omits is skipped as a known failure.
     * Blank lines and "#" comments are ignored, which is how a skip gets annotated with a reason.
     */
    private void collectFolder( Path folder, List<Path> designs ) throws IOException
    {
        Path suite = folder .resolve( SKIP_FILE );
        if ( Files .isRegularFile( suite ) ) {
            for ( String line : Files .readAllLines( suite, StandardCharsets .UTF_8 ) ) {
                line = line .trim();
                if ( line .isEmpty() || line .startsWith( "#" ) )
                    continue;
                Path entry = folder .resolve( line );
                if ( Files .isDirectory( entry ) )
                    collectFolder( entry, designs );
                else if ( Files .isRegularFile( entry ) && isDesign( entry ) )
                    designs .add( entry );
                else
                    System .err .println( "  listed in " + suite + " but not found: " + line );
            }
            return;
        }
        try ( Stream<Path> children = Files .list( folder ) ) {
            List<Path> entries = new ArrayList<>();
            children .forEach( entries::add );
            for ( Path entry : entries ) {
                if ( Files .isDirectory( entry ) )
                    collectFolder( entry, designs );
                else if ( isDesign( entry ) )
                    designs .add( entry );
            }
        }
    }

    private static boolean isDesign( Path path )
    {
        return path .getFileName() .toString() .toLowerCase() .endsWith( VZOME_SUFFIX );
    }

    private Path relativize( Path design )
    {
        return this .baseFolder() .relativize( design );
    }

    private static Path withPovSuffix( Path relative )
    {
        String name = relative .getFileName() .toString();
        name = name .substring( 0, name .length() - VZOME_SUFFIX .length() ) + ".pov";
        Path parent = relative .getParent();
        return parent == null ? Paths .get( name ) : parent .resolve( name );
    }

    private Result exportOne( Path design ) throws Exception
    {
        Path relative = relativize( design );
        Path relativePov = withPovSuffix( relative );

        Exporter exporter = this .application .getExporter( "pov" );

        StringWriter buffer = new StringWriter();
        try ( InputStream bytes = new FileInputStream( design .toFile() ) ) {
            Document doc = this .application .loadDocument( bytes );  // this overload replays the edit history
            try ( PrintWriter out = new PrintWriter( buffer ) ) {
                // A null File (inside the api.Exporter facade) suppresses the ".ini" sidecar.
                exporter .doExport( doc, out, 1080, 1920 );
            }
        }
        String exported = buffer .toString();

        Path target = this .outputRoot .resolve( relativePov );
        Files .createDirectories( target .getParent() );
        // Write to a temp file and move into place, so an interrupted run never leaves a
        // half-written ".pov" that a later comparison would silently misread.
        Path temp = Files .createTempFile( target .getParent(), target .getFileName() .toString(), ".tmp" );
        Files .write( temp, exported .getBytes( StandardCharsets .UTF_8 ) );
        Files .move( temp, target, StandardCopyOption .REPLACE_EXISTING );

        if ( this .goldenRoot == null )
            return new Result( relativePov, Status .RECORDED, null );

        Path golden = this .goldenRoot .resolve( relativePov );
        if ( ! Files .exists( golden ) )
            return new Result( relativePov, Status .MISSING_GOLDEN, golden .toString() );

        String expected = new String( Files .readAllBytes( golden ), StandardCharsets .UTF_8 );
        String left = this .geometryOnly ? geometryOf( expected ) : expected;
        String right = this .geometryOnly ? geometryOf( exported ) : exported;
        if ( left .equals( right ) )
            return new Result( relativePov, Status .MATCHED, null );

        return new Result( relativePov, Status .DIFFERED, describeDifference( left, right ) );
    }

    /**
     * Everything the edit history produced, with the camera and lighting preamble removed.
     * That preamble is the "#declare" view settings, the "light_source" lines, the ambient and
     * background colors, and the boilerplate preamble between them -- none of which say
     * anything about the model.  The geometry proper is the shape, transform and color
     * declarations plus the "triangle" and "object" lines, which is what is kept here.
     */
    static String geometryOf( String pov )
    {
        StringBuilder kept = new StringBuilder();
        for ( String line : pov .split( "\n", -1 ) ) {
            String trimmed = line .trim();
            if ( trimmed .isEmpty() )
                continue;
            boolean isGeometry = trimmed .startsWith( "object " ) || trimmed .startsWith( "object{" )
                    || trimmed .startsWith( "triangle" )
                    || trimmed .startsWith( "#declare S" )
                    || trimmed .startsWith( "#declare trans" )
                    || trimmed .startsWith( "#declare color" )
                    || trimmed .startsWith( "#declare embedding" )
                    || trimmed .startsWith( "mesh {" ) || trimmed .equals( "}" );
            if ( isGeometry )
                kept .append( line ) .append( '\n' );
        }
        return kept .toString();
    }

    /**
     * Reports the first differing line, rather than a whole-file diff.  With thousands of files
     * and gigabytes of output, full diffs in a report are unreadable; the two ".pov" files are
     * left on disk for a real diff when a case needs investigating.
     */
    static String describeDifference( String expected, String actual )
    {
        String[] expectedLines = expected .split( "\n", -1 );
        String[] actualLines = actual .split( "\n", -1 );
        int limit = Math .min( expectedLines .length, actualLines .length );
        for ( int i = 0; i < limit; i++ ) {
            String expectedLine = expectedLines[ i ];
            String actualLine = actualLines[ i ];
            if ( ! expectedLine .equals( actualLine ) ) {
                // POV-Ray instance lines run long, so show a window around the first differing
                // character rather than the start of the line -- otherwise two lines that differ
                // near their end look identical in the report.
                int column = 0;
                int shared = Math .min( expectedLine .length(), actualLine .length() );
                while ( column < shared && expectedLine .charAt( column ) == actualLine .charAt( column ) )
                    column++;
                return "line " + ( i + 1 ) + ", column " + ( column + 1 ) + ":"
                        + "\n      golden: " + excerpt( expectedLine, column )
                        + "\n      actual: " + excerpt( actualLine, column );
            }
        }
        return "golden has " + expectedLines .length + " lines, actual has " + actualLines .length;
    }

    private static final int EXCERPT_CONTEXT = 60;

    /**
     * A window of the line centered on the given column, with ellipses marking any truncation,
     * so the reader can see what actually changed.
     */
    private static String excerpt( String line, int column )
    {
        int start = Math .max( 0, column - EXCERPT_CONTEXT );
        int end = Math .min( line .length(), column + EXCERPT_CONTEXT );
        String text = line .substring( start, end )
                .replace( "\t", "\\t" ) .replace( "\r", "\\r" );
        return ( start > 0 ? "..." : "" ) + text + ( end < line .length() ? "..." : "" )
                + ( line .length() == end && line .endsWith( " " ) ? "<trailing space>" : "" );
    }

    private int run() throws Exception
    {
        List<Path> all = collectDesigns();
        if ( all .isEmpty() ) {
            System .err .println( "No .vZome files found under " + this .inputRoot );
            return 1;
        }
        // Take this process's slice.  Round-robin rather than contiguous blocks, so that a
        // folder of unusually large designs does not all land on one worker.
        List<Path> designs = new ArrayList<>();
        for ( int i = 0; i < all .size(); i++ )
            if ( i % this .split == this .shard )
                designs .add( all .get( i ) );

        boolean comparing = this .goldenRoot != null;
        // Machine-readable progress, for the parent process to aggregate across shards; see
        // the batchPov task.  Printed even when running standalone -- it is easy to ignore.
        System .out .println( "@total " + designs .size() );
        System .out .flush();
        String label = this .split > 1 ? " (shard " + this .shard + " of " + this .split + ")" : "";
        System .out .println( ( comparing ? "Comparing " : "Exporting " ) + designs .size()
                + " of " + all .size() + " designs" + label );
        System .out .println( "  input:  " + this .inputRoot );
        System .out .println( "  output: " + this .outputRoot );
        if ( comparing ) {
            System .out .println( "  golden: " + this .goldenRoot );
            if ( this .geometryOnly )
                System .out .println( "  comparing geometry only; camera and lighting ignored" );
        }

        Files .createDirectories( this .outputRoot );

        List<Result> results = new ArrayList<>();
        long started = System .currentTimeMillis();

        // One design at a time.  Loading cannot be parallelized inside a JVM (see the class
        // comment); a watchdog thread is used only to bound how long a single pathological
        // design may take.
        ExecutorService watchdog = Executors .newSingleThreadExecutor( runnable -> {
            Thread thread = new Thread( runnable, "batch-pov-export" );
            thread .setDaemon( true );
            return thread;
        } );
        try {
            for ( Path design : designs ) {
                Path relative = relativize( design );
                Future<Result> future = watchdog .submit( () -> exportOne( design ) );
                try {
                    results .add( future .get( this .timeoutSeconds, TimeUnit .SECONDS ) );
                } catch ( TimeoutException te ) {
                    future .cancel( true );
                    results .add( new Result( relative, Status .FAILED,
                            "timed out after " + this .timeoutSeconds + "s" ) );
                } catch ( Throwable t ) {
                    Throwable cause = t .getCause() == null ? t : t .getCause();
                    String message = cause .getMessage();
                    results .add( new Result( relative, Status .FAILED,
                            cause .getClass() .getSimpleName()
                            + ( message == null ? "" : ": " + message ) ) );
                }
                Result latest = results .get( results .size() - 1 );
                System .out .println( "@progress " + latest .status + " " + latest .relative );
                System .out .flush();
            }
        } finally {
            watchdog .shutdownNow();
        }

        return report( results, comparing, System .currentTimeMillis() - started );
    }

    private int report( List<Result> results, boolean comparing, long elapsedMillis ) throws IOException
    {
        List<Result> differed = new ArrayList<>();
        List<Result> failed = new ArrayList<>();
        List<Result> missing = new ArrayList<>();
        int matched = 0, recorded = 0;
        for ( Result result : results )
            switch ( result .status ) {
                case MATCHED:        matched++;                 break;
                case RECORDED:       recorded++;                break;
                case DIFFERED:       differed .add( result );   break;
                case MISSING_GOLDEN: missing .add( result );    break;
                case FAILED:         failed .add( result );     break;
            }

        Comparator<Result> byPath = Comparator .comparing( r -> r .relative .toString() );
        Collections .sort( differed, byPath );
        Collections .sort( failed, byPath );
        Collections .sort( missing, byPath );

        System .out .println();
        System .out .println( "Finished in " + ( elapsedMillis / 1000 ) + "s" );
        if ( comparing ) {
            System .out .println( "  matched:        " + matched );
            System .out .println( "  DIFFERED:       " + differed .size() );
            System .out .println( "  missing golden: " + missing .size() );
        }
        else
            System .out .println( "  exported:       " + recorded );
        System .out .println( "  failed:         " + failed .size() );

        for ( Result result : differed )
            System .out .println( "  DIFFERED  " + result .relative + "  " + result .detail );
        for ( Result result : failed )
            System .out .println( "  FAILED    " + result .relative + "  " + result .detail );

        writeReport( "failures.txt", failed );
        if ( comparing ) {
            writeReport( "regressions.txt", differed );
            writeReport( "missing-golden.txt", missing );
        }

        // Missing golden files are not a regression -- they are simply designs the baseline does
        // not cover yet -- so they are reported but do not fail the run.
        boolean bad = ! failed .isEmpty() || ( comparing && ! differed .isEmpty() );
        return bad ? 1 : 0;
    }

    private void writeReport( String name, List<Result> results ) throws IOException
    {
        // Shards run as separate processes writing into one output tree, so each needs its own
        // report file; the Gradle task merges them when the run finishes.
        if ( this .split > 1 )
            name = name .replaceFirst( "\\.txt$", "-" + this .shard + ".txt" );
        Path report = this .outputRoot .resolve( name );
        if ( results .isEmpty() ) {
            Files .deleteIfExists( report );
            return;
        }
        StringBuilder text = new StringBuilder();
        for ( Result result : results )
            text .append( result .relative ) .append( "  " )
                 .append( result .detail == null ? "" : result .detail ) .append( "\n" );
        Files .write( report, text .toString() .getBytes( StandardCharsets .UTF_8 ) );
        System .out .println( "  wrote " + report );
    }

    public static void main( String[] args ) throws Exception
    {
        List<String> positional = new ArrayList<>();
        Path golden = null;
        int shard = 0, split = 1;
        int timeout = DEFAULT_TIMEOUT_SECONDS;
        boolean geometryOnly = false;
        for ( int i = 0; i < args .length; i++ ) {
            switch ( args[ i ] ) {
            case "--compare":
                golden = Paths .get( requireValue( args, ++i, "--compare" ) ) .toAbsolutePath() .normalize();
                break;
            case "--shard":
                shard = Integer .parseInt( requireValue( args, ++i, "--shard" ) );
                break;
            case "--split":
                split = Integer .parseInt( requireValue( args, ++i, "--split" ) );
                break;
            case "--geometry-only":
                geometryOnly = true;
                break;
            case "--timeout":
                timeout = Integer .parseInt( requireValue( args, ++i, "--timeout" ) );
                break;
            default:
                positional .add( args[ i ] );
            }
        }
        if ( positional .size() < 2 ) {
            System .err .println( "usage: BatchPovExporter <inputRoot> <outputRoot>"
                    + " [--compare <goldenRoot>] [--geometry-only]"
                    + " [--shard N --split M] [--timeout seconds]" );
            System .exit( 2 );
        }
        if ( split < 1 || shard < 0 || shard >= split ) {
            System .err .println( "invalid --shard/--split: need 0 <= shard < split" );
            System .exit( 2 );
        }
        Path input = Paths .get( positional .get( 0 ) ) .toAbsolutePath() .normalize();
        Path output = Paths .get( positional .get( 1 ) ) .toAbsolutePath() .normalize();

        if ( ! Files .exists( input ) ) {
            System .err .println( "input does not exist: " + input );
            System .exit( 2 );
        }
        if ( golden != null && ! Files .isDirectory( golden ) ) {
            System .err .println( "golden folder does not exist: " + golden );
            System .exit( 2 );
        }

        BatchPovExporter exporter = new BatchPovExporter( input, output, golden, shard, split, timeout );
        exporter .geometryOnly = geometryOnly;
        int exitCode = exporter .run();
        System .exit( exitCode );
    }

    private static String requireValue( String[] args, int index, String option )
    {
        if ( index >= args .length ) {
            System .err .println( option + " requires a value" );
            System .exit( 2 );
        }
        return args[ index ];
    }
}
