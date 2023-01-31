package com.vzome.desktop.awt;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.vorthmann.j3d.J3dComponentFactory;
import org.vorthmann.j3d.Platform;
import org.vorthmann.zome.ui.ApplicationUI;

import com.vzome.core.commands.Command.Failure;
import com.vzome.core.commands.Command.FailureChannel;
import com.vzome.core.editor.DocumentModel;
import com.vzome.core.exporters.GeometryExporter;
import com.vzome.core.exporters2d.SnapshotExporter;
import com.vzome.core.math.symmetry.AntiprismSymmetry;
import com.vzome.core.math.symmetry.Symmetry;
import com.vzome.core.render.Colors;
import com.vzome.core.render.RenderedModel;
import com.vzome.core.render.Scene;
import com.vzome.core.viewing.AntiprismTrackball;
import com.vzome.core.viewing.Lights;
import com.vzome.desktop.api.Controller;
import com.vzome.desktop.controller.DefaultController;

public class ApplicationController extends DefaultController
{
    private static final Logger LOGGER = Logger.getLogger( "org.vorthmann.zome.controller" );

    private final Map<String, DocumentController> docControllers = new HashMap<>();

    private final UI ui;

    private final Properties storedConfig = new Properties();

    private final Properties properties = new Properties();

    private J3dComponentFactory rvFactory;

    private final com.vzome.core.editor.Application modelApp;

    private final File configFile;

    private int lastUntitled = 0;

    private Map<String, RenderedModel> symmetryModels = new HashMap<String, RenderedModel>();
    
    public interface UI
    {
        void doAction( String action );
        
        void runScript( String script, File file );
        
        void openApplication( File file );
    }

    public ApplicationController( UI ui, Properties commandLineArgs, J3dComponentFactory rvFactory )
    {
        super();
        
        // This is executed on the EDT.  Should it be?

        long starttime = System.currentTimeMillis();

        if ( LOGGER .isLoggable( Level .INFO ) )
            LOGGER .info( "ApplicationController .initialize() starting" );

        this.ui = ui;

        File prefsFolder = Platform .getPreferencesFolder();        
        File prefsFile = new File( prefsFolder, "vZome.preferences" );
        if ( ! prefsFile .exists() ) {
            prefsFolder = new File( System.getProperty( "user.home" ) );
            prefsFile = new File( prefsFolder, "vZome.preferences" );
        }
        if ( ! prefsFile .exists() ) {
            prefsFile = new File( prefsFolder, ".vZome.prefs" );
        }
        Properties userPrefs = new Properties();
        if ( ! prefsFile .exists() ) {
            LOGGER .config( "Used default preferences." );
        } else {
            try {
                InputStream in = new FileInputStream( prefsFile );
                userPrefs .load( in );
                LOGGER .config( "User Preferences loaded from " + prefsFile .getAbsolutePath() );
            } catch ( Throwable t ) {
                LOGGER .severe( "problem reading user preferences: " + t.getMessage() );
            }
        }

        this.configFile = new File( new File( System.getProperty( "user.home" ) ), ".vZome-config.properties" );
        if ( this.configFile .exists() ) {
            try {
                InputStream in = new FileInputStream( this.configFile );
                storedConfig .load( in );
                LOGGER .config( "Stored config loaded from " + this.configFile .getAbsolutePath() );
            } catch ( Throwable t ) {
                LOGGER .severe( "problem reading stored config: " + t.getMessage() );
            }
        }

        Properties defaults = new Properties();
        String defaultRsrc = "org/vorthmann/zome/app/defaultPrefs.properties";
        try {
            ClassLoader cl = ApplicationUI.class.getClassLoader();
            InputStream in = cl.getResourceAsStream( defaultRsrc );
			if (in != null) {
				defaults.load(in); // override the core defaults
			} else {
				LOGGER.warning("RESOURCE NOT FOUND. Ensure that the build path for the desktop project includes " + defaultRsrc);
			}
        } catch ( IOException ioe ) {
            LOGGER.severe( "problem reading default preferences: " + defaultRsrc );
        }

        // last-wins, so getProperty() will show command-line args overriding stored configs,
        //   which override user prefs, which override built-in defaults
        properties .putAll( defaults );
        properties .putAll( userPrefs );
        properties .putAll( this.storedConfig );
        properties .putAll( commandLineArgs );

        // This seems to get rid of the "white-out" problem on David's (Windows) computer.
        // Otherwise it shows up sporadically, but still frequently. 
        // It is usually, but not always, triggered by such events as context menus,
        // tool tips, or modal dialogs being rendered on top of the main frame.
        // Since the problem has not been reported elsewhere, this fix will be configurable, rather than hard coded.
        final String NOERASEBACKGROUND = "sun.awt.noerasebackground";
        if( propertyIsTrue(NOERASEBACKGROUND)) { // if it's set to true in the prefs file or command line
            System.setProperty(NOERASEBACKGROUND, "true"); // then set the System property so the AWT/Swing components will use it.
            LOGGER .config( NOERASEBACKGROUND + " is set to 'true'." );
        }

        final FailureChannel failures = new FailureChannel()
        {	
            @Override
            public void reportFailure( Failure f )
            {
                mErrors.reportError( USER_ERROR_CODE, new Object[] { f } );
            }
        };
        modelApp = new com.vzome.core.editor.Application( true, failures, properties );

        if ( rvFactory != null ) {
            this .rvFactory = rvFactory;
        }
        else
        {
            // need this set up before we do any loadModel
            String factoryName = getProperty( "RenderingViewer.Factory.class" );
            if ( factoryName == null )
                factoryName = "org.vorthmann.zome.render.jogl.JoglFactory";
            try {
                Class<?> factoryClass = Class.forName( factoryName );
                Constructor<?> constructor = factoryClass .getConstructor( new Class<?>[] {} );
                this .rvFactory = (J3dComponentFactory) constructor.newInstance( new Object[] {} );
            } catch ( Exception e ) {
                mErrors.reportError( "Unable to instantiate RenderingViewer.Factory class: " + factoryName, new Object[] {} );
                System.exit( 0 );
            }
        }

        long endtime = System.currentTimeMillis();
        if ( LOGGER .isLoggable( Level .INFO ) )
            LOGGER .log(Level.INFO, "ApplicationController initialization in milliseconds: {0}", ( endtime - starttime ));
    }

    public RenderedModel getSymmetryModel( String path, Symmetry symmetry )
    {
        String key = path;
        if(symmetry instanceof AntiprismSymmetry) {
            // Create distinct keys for antiprism symmetries.
            // Otherwise, the cache does not care if the symmetry matches.
            key = path + "@" + symmetry.getName();
        }
        RenderedModel result = this .symmetryModels .get( key );
        if ( result != null ) {
            return result;
        }
        
        ClassLoader cl = this .getClass() .getClassLoader();
        InputStream bytes = cl.getResourceAsStream( path );
        
        if(symmetry instanceof AntiprismSymmetry) {
            bytes = AntiprismTrackball.getTrackballModelStream(bytes, (AntiprismSymmetry)symmetry);
        }

        try {
            DocumentModel document = this .modelApp .loadDocument( bytes );
            document .finishLoading( false, false );
            result = document .getRenderedModel();
            this .symmetryModels .put( key, result );
            return result;
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void doAction( String action )
    {
        try {
            if ( action .equals( "showAbout" ) 
                    || action .equals( "openURL" ) 
                    || action .equals( "quit" )
                    || action .equals( "new-polygon" )
                    || action .startsWith( "browse-" )
                    )
            {
                this .ui .doAction( action );
                return;
            }

            if( "launch".equals(action) ) {
                String sawWelcome = this.properties .getProperty( "saw.welcome" );
                if ( sawWelcome == null )
                {
                    String welcome = this.properties .getProperty( "welcome" );
                    doAction( "openResource-" + welcome );
                    this.storedConfig .setProperty( "saw.welcome", "true" );
                    FileWriter writer;
                    try {
                        writer = new FileWriter( this.configFile );
                        this.storedConfig .store( writer, "This file is managed by vZome.  Do not edit." );
                        writer .close();
                    } catch ( IOException e ) {
                        LOGGER.fine(e.toString());
                    }
                    return;
                }
                action = "new";
            }

            if ( "new" .equals( action ) ) {
                String fieldName = properties .getProperty( "default.field" );
                action = "new-" + fieldName;
            }

            if ( action .startsWith( "new-" ) )
            {
                String fieldName = action .substring( "new-" .length() );
                File prototype = new File( Platform .getPreferencesFolder(), "Prototypes/" + fieldName + ".vZome" );
                if ( prototype .exists() ) {
                    LOGGER.log(Level.CONFIG, "Loading default template from {0}", prototype.getCanonicalPath());
                    doFileAction( "newFromTemplate", prototype );
                }
                else
                {
                    // creating a new Document
                    Properties docProps = new Properties();
                    docProps .setProperty( "new.document", "true" );
                    DocumentModel document = modelApp .createDocument( fieldName );
                    String title = "Untitled " + ++lastUntitled;
                    docProps .setProperty( "window.title", title );
                    newDocumentController( title, document, docProps );
                }
            }
            else if ( action .startsWith( "openResource-" ) )
            {
                Properties docProps = new Properties();
                docProps .setProperty( "reader.preview", "true" );
                String path = action .substring( "openResource-" .length() );
                docProps .setProperty( "window.title", path );
                ClassLoader cl = this .getClass() .getClassLoader();
                InputStream bytes = cl .getResourceAsStream( path );
                loadDocumentController( path, bytes, docProps );
            }
            else if ( action .startsWith( "newFromResource-" ) )
            {
                Properties docProps = new Properties();
                docProps .setProperty( "as.template", "true" ); // don't set window.file!
                final String args = action .substring( "newFromResource-" .length() );
				// use a delimiter character that shouldn't ever be in a path
				// and doesn't need to be escaped in a regular expression
                final String[] parts = args.split(":");
                final String path = parts[0];
                ClassLoader cl = this .getClass() .getClassLoader();
                InputStream bytes = cl .getResourceAsStream( path );
                // handle the special case of "openTrackballModel" for AntiprismSymmetry
                if(parts.length == 2 && path.endsWith("antiprism-trackball-template.vZome")) {
                	final String targetSymmName = parts[1];
                	// symmetryModels is a Map, so the values are in no particular order.
                	// It contains an instance of each symmetry that the application has ever opened
                	// for all documents including those that have been closed, not just the current document.
                	// It's possible that other documents have opened fields having different antiprism symmetries
                	// so we can't just open the first antiprismSymmetry we encounter in the loop.
                	// Instead, we've appended the symmetry name from the current document to the action
                	// so we can identify that specific symmetry   
                    for(RenderedModel rm : this.symmetryModels.values()) {
                        Symmetry symm = rm.getOrbitSource().getSymmetry();
                        if(symm.getName().equals(targetSymmName)) {
                        	 LOGGER.fine("Loading " + targetSymmName + " trackball model");
                            bytes = AntiprismTrackball.getTrackballModelStream(bytes, (AntiprismSymmetry)symm);
                            break;
                        }
                    }
                }
                loadDocumentController( path, bytes, docProps );
            }
            else if ( action .startsWith( "openURL-" ) )
            {
                Properties docProps = new Properties();
                docProps .setProperty( "as.template", "true" );
                String path = action .substring( "openURL-" .length() );
                docProps .setProperty( "window.title", path );
                try {
                    URL url = new URL( path );
                    InputStream bytes= null;
                    HttpURLConnection conn = (HttpURLConnection) url .openConnection();
                    // See https://stackoverflow.com/questions/1884230/urlconnection-doesnt-follow-redirect
                    //  This won't switch protocols, but seems to work otherwise.
                    conn .setInstanceFollowRedirects( true );
                    bytes = conn .getInputStream();
                    loadDocumentController( path, bytes, docProps );
                }
                catch ( Throwable e ) {
                    e.printStackTrace();
                    this .mErrors .reportError( "Unable to open URL: " + e .getMessage(), new Object[]{ e } );
                }
            }
            else 
            {
                this .mErrors .reportError( UNKNOWN_ACTION, new Object[]{ action } );
            }
        } catch ( Exception e ) {
            this .mErrors .reportError( UNKNOWN_ERROR_CODE, new Object[]{ e } );
        }
    }

    @Override
    public void doFileAction( String command, File file )
    {
        if ( LOGGER .isLoggable( Level.INFO ) ) LOGGER .info( String.format( "ApplicationController.doFileAction: %s %s", command, file .getAbsolutePath() ) );
        if ( file != null )
        {
            Properties docProps = new Properties();
            Path filePath = file .toPath();
            String path = filePath .toAbsolutePath() .toString();

            String lowerPath = path .toLowerCase();
            int pos = lowerPath .lastIndexOf( ".vzome." );
            if( pos > 0 && ! lowerPath .endsWith( ".vzome" ) ) {
                /*
                 * This allows the user to select a file named "foo.vzome.png" that they can preview,
                 * as a "proxy" that will actually attempt to open the corresponding vzome file.
                 * 
                 * Note that such a "proxy" image file with a ".vome.png" extension is generated automatically 
                 * upon saving a vZome file by adding "save.exports=capture.png" to .vZome.prefs.
                 *
                 * On Windows, there is a pattern for saving a copy of an existing file that triggers a
                 * duplicate extension, like "whatever.vzome.vZome", so we want to exclude that case.
                 */
                path = path.substring( 0, pos += 6 );
                file = Paths .get( path ) .toFile();
                if ( ! file.exists() ) {
                    this .mErrors .reportError( "File does not exist: " + path, new Object[]{} );
                    return;
                }
            }

            docProps .setProperty( "window.title", path );
            switch ( command ) {

            case "open":
                docProps .setProperty( "window.file", path );
                docProps .setProperty( "original.path", path );
                break;

            case "newFromTemplate":
                String title = "Untitled " + ++lastUntitled;
                docProps .setProperty( "window.title", title ); // override the default above
                docProps .setProperty( "as.template", "true" ); // don't set window.file!
                break;

            case "openDeferringRedo":
                docProps .setProperty( "open.undone", "true" );
                docProps .setProperty( "window.file", path );
                docProps .setProperty( "original.path", path );
                break;

            default:
                this .mErrors .reportError( UNKNOWN_ACTION, new Object[]{ command } );
                return;
            }
            try {
                InputStream bytes = new FileInputStream( file );
                loadDocumentController( path, bytes, docProps );
            } catch ( Exception e ) {
                e .printStackTrace();
                this .mErrors .reportError( UNKNOWN_ERROR_CODE, new Object[]{ e } );
            }
        }
    }

    private void loadDocumentController( final String name, final InputStream bytes, final Properties properties )
    {
        try {
            DocumentModel document = modelApp .loadDocument( bytes );
            newDocumentController( name, document, properties );
        } catch ( Exception e ) {
            e.printStackTrace();
            this .mErrors .reportError( "Unable to load; this may not be a vZome file.", new Object[]{ e } );
        }
    }

    public J3dComponentFactory getJ3dFactory()
    {
        return rvFactory;
    }

    @Override
    public boolean userHasEntitlement( String propName )
    {
        switch ( propName ) {

        case "save.files":
            return getProperty( "licensed.user" ) != null;

        case "all.tools":
            return propertyIsTrue( "entitlement.all.tools" );

        case "developer.extras":
            return getProperty( "vZomeDeveloper" ) != null;

        default:
            // TODO make this work more like developer.extras
            return propertyIsTrue( "entitlement." + propName );
            // this IS the backstop controller, so no purpose in calling super
        }
    }

    @Override
    public final String getProperty( String propName )
    {
        switch ( propName ) {

        case "formatIsSupported":
            return "true";

        case "untitled.title":
            return "Untitled " + ++lastUntitled;

        case "coreVersion":
            return this .modelApp .getCoreVersion();

        default:
            if ( propName .startsWith( "field.label." ) )
            {
                String fieldName = propName .substring( "field.label." .length() );
                // TODO implement AlgebraicField.getLabel()
                switch ( fieldName ) {

                case "golden":
                    return "Zome (Golden)";

                case "rootTwo":
                    return "\u221A2";

                case "rootThree":
                    return "\u221A3";

                case "snubCube":
                    return "Snub Cube";

                case "snubDodec":
                    return "Snub Dodec";

                case "sqrtPhi":
                    return "\u221A\u03C6";

                case "superGolden":
                    return "Super Golden";

                case "plasticNumber":
                    return "Plastic Number";

                case "plasticPhi":
                    return "Plastic \u03C6";

                default:
                    if( fieldName.startsWith("sqrt") ) {
                        return fieldName.replace("sqrt","\u221A");
                    } else {
                        // capitalize first letter
                        return Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    }
                }
            }
            if ( propName .startsWith( "enable." ) && propName .endsWith( ".field" ) )
            {
                String fieldName = propName .substring( "enable." .length() );
                fieldName = fieldName .substring( 0, fieldName .lastIndexOf( ".field" ) );

                switch ( fieldName ) {

                case "golden":
                    return "false"; // this one is forcibly enabled by the menu, and we don't want it listed twice

                case "dodecagon":
                    return "false"; // this is just an alias for rootThree

                default:
                    // fall through
                }

                if ( getProperty( "vZomeDeveloper" ) != null )
                    return "true"; // developer sees all available fields

                switch ( fieldName ) {

                case "rootTwo":
                case "rootThree":
                case "heptagon":
                case "sqrtPhi":
                    return "true"; // these are enabled for everyone

                default:
                    // fall through, see if it is explicitly set
                }
            }
            return properties .getProperty( propName );
        }
    }

    @Override
    public void setModelProperty( String name, Object value )
    {
        this.properties .setProperty( name, value .toString() );
        switch ( name ) {

        case "githubAccessToken":
        case "sharing-generatePost":
        case "sharing-publishImmediately":
            
            this.storedConfig .setProperty( name, value .toString() );
            FileWriter writer;
            try {
                writer = new FileWriter( this.configFile );
                this.storedConfig .store( writer, "This file is managed by vZome.  Do not edit." );
                writer .close();
            } catch ( IOException e ) {
                LOGGER.fine(e.toString());
            }
            break;

        default:
            break;
        }
    }

    @Override
    public Controller getSubController( final String name )
    {
        return docControllers .get( name );
    }

    private void newDocumentController( final String name, final DocumentModel document, final Properties props )
    {
        props .setProperty( "githubClientId", this .properties .getProperty( "githubClientId" ) );
        props .setProperty( "githubClientSecret", this .properties .getProperty( "githubClientSecret" ) );
        String repoNameOverride = this .properties .getProperty( "githubRepoName" );
        if ( repoNameOverride != null && ! "".equals( repoNameOverride ) )
            props .setProperty( "githubRepoName", repoNameOverride );
        
//        props .setProperty( "edition", this .properties .getProperty( "edition" ) );
        props .setProperty( "version", this .properties .getProperty( "version" ) );
        props .setProperty( "buildNumber", this .properties .getProperty( "buildNumber" ) );
        props .setProperty( "gitCommit", this .properties .getProperty( "gitCommit" ) );

        DocumentController newest = new DocumentController( document, this, props );
        this .registerDocumentController( name, newest );
        // trigger window creation in the UI
        this .firePropertyChange( "newDocument", null, newest );
    }

    private void registerDocumentController( final String name, final DocumentController newest )
    {
        this .docControllers .put( name, newest );
        newest .addPropertyListener( new PropertyChangeListener()
        {
            @Override
            public void propertyChange( PropertyChangeEvent evt )
            {
                switch ( evt .getPropertyName() ) {

                case "name":
                    docControllers .remove( name );
                    // important to re-register under the new name, AND get a new listener, or removes won't work
                    newest .removePropertyListener( this );
                    registerDocumentController( (String) evt .getNewValue(), newest );
                    break;

                case "visible":
                    if ( Boolean.FALSE .equals( evt .getNewValue() ) ) {
                        docControllers .remove( name );
                        if ( ! propertyIsTrue( "keep.alive" ) && docControllers .isEmpty() )
                            // closed the last window, so we're exiting
                            System .exit( 0 );
                    }
                    break;

                default:
                    break;
                }
            }
        });
    }

    public Colors getColors()
    {
        return this .modelApp .getColors();
    }

    public GeometryExporter getExporter( String format )
    {
        return this .modelApp .getExporter( format );
    }

    public SnapshotExporter get2dExporter( String format )
    {
        return this .modelApp .getSnapshotExporter( format );
    }

    // public RenderingViewer.Factory getRenderingViewerFactory()
    // {
    // return mRVFactory;
    // }

    @Override
    public String[] getCommandList( String listName )
    {
        if ( listName .startsWith( "fields" ) )
        {
            Set<String> names = modelApp .getFieldNames();
            SortedSet<String> sorted = new TreeSet<String>( names );
            return sorted .toArray( new String[]{} );
        }
        else if ( listName .startsWith( "symmetries." ) )
        {
            //        	return (String[]) this .symmetryPerspective .getGeometries() .stream() .map( e -> e .getName() ) .toArray();
            return null;  // TODO probably unused
        }
        return new String[0];
    }

    public Lights getLights()
    {
        return modelApp .getLights();
    }
    
    @Override
    protected void runScript( String script, File file )
    {
        this .ui .runScript( script, file );
    }

    @Override
    protected void openApplication( File file )
    {
        String script = this .getProperty( "export.script" );
        if ( script != null )
            this .ui .runScript( script, file );
        else
            this .ui .openApplication( file );
    }

    public static void main(String[] args)
    {
        String filePath = "noFilePath";
        if ( args.length > 0 )
            filePath = args[ 0 ];
        try {
            Properties props = new Properties();
            props .setProperty( "entitlement.model.edit", "true" );
            props .setProperty( "keep.alive", "true" );

            ApplicationController appC = new ApplicationController( new ApplicationController.UI()
            {   
                @Override
                public void doAction( String action )
                {
                    System .out .println( "UI event: " + action );
                }

                @Override
                public void runScript( String script, File file )
                {}

                @Override
                public void openApplication( File file )
                {}
            }, props, new J3dComponentFactory()
            {
                @Override
                public RenderingViewer createRenderingViewer( Scene scene, boolean lightweight )
                {
                    // Should never be called
                    return null;
                }
            });
            appC .setErrorChannel( new Controller.ErrorChannel() {

                @Override
                public void reportError(String errorCode, Object[] arguments)
                {
                    System .out .println( errorCode );
                }

                @Override
                public void clearError() {}
            });
            appC .doFileAction( "open", new File( filePath ) );
            System.out.println( "successfully opened " + filePath );
        } catch (Throwable e) {
            e .printStackTrace();
        }
    }
}
