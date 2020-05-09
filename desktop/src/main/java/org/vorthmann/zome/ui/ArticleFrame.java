package org.vorthmann.zome.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.FileDialog;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import org.vorthmann.j3d.J3dComponentFactory;
import org.vorthmann.ui.Controller;
import org.vorthmann.ui.ExclusiveAction;

import com.vzome.core.render.Scene;
import com.vzome.desktop.controller.RenderingViewer;

public class ArticleFrame extends DocumentFrame
{
    private static final long serialVersionUID = 1L;

    private ModelPanel modelPanel;
    
    private static final Logger logger = Logger.getLogger( "org.vorthmann.zome.ui" );
    
    private JPanel viewControl;

    private Controller cameraController;

    private Controller lessonController;
    	
	private ActionListener localActions;
	    
    private final FileDialog fileDialog = new FileDialog( this );
    
	private File mFile = null;
	
	private Controller.ErrorChannel errors;

    private Snapshot2dFrame snapshot2dFrame;

	private PropertyChangeListener appUI;
            
    void setAppUI( PropertyChangeListener appUI )
    {
        this .appUI = appUI;
    }
        
    public ArticleFrame( final Controller controller, final J3dComponentFactory factory3d )
    {
        super( controller, factory3d );
    }
    
    @Override
    protected void initialize()
    {
        mController .addPropertyListener( this );
        
        // Keep the tool tip showing
        ToolTipManager.sharedInstance().setDismissDelay( 20000 );

        String path = mController .getProperty( "window.file" );
        if ( path != null )
            this .mFile = new File( path ); // this enables "save" in localActions
        
        errors = new Controller.ErrorChannel()
        {
			@Override
            public void reportError( String errorCode, Object[] arguments )
            {
                if ( Controller.USER_ERROR_CODE.equals( errorCode ) ) {
                    errorCode = ( (Exception) arguments[0] ).getMessage();
                    // don't want a stack trace for a user error
                    logger.log( Level.WARNING, errorCode );
                } else if ( Controller.UNKNOWN_ERROR_CODE.equals( errorCode ) ) {
                    errorCode = ( (Exception) arguments[0] ).getMessage();
                    logger.log( Level.WARNING, "internal error: " + errorCode, ( (Exception) arguments[0] ) );
                    errorCode = "internal error has been logged";
                } else {
                    logger.log( Level.WARNING, "reporting error: " + errorCode, arguments );
                    // TODO use resources
                }

                JOptionPane .showMessageDialog( ArticleFrame.this, errorCode, "Command Failure", JOptionPane .ERROR_MESSAGE );
            }

			@Override
            public void clearError() {}
        };
        mController .setErrorChannel( errors );

        // ---- catch-all ActionListener for locally-handled actions

        final String initSystem = mController .getProperty( "symmetry" );
        localActions = new ActionListener()
        {
            private String system = initSystem;

            private final Map<String, JDialog> shapesDialogs = new HashMap<>();

            private final Map<String, JDialog> directionsDialogs = new HashMap<>();

			@Override
            public void actionPerformed( ActionEvent e )
            {
				Controller delegate = mController;
                String cmd = e.getActionCommand();
                switch ( cmd ) {
                
                case "close":
                    closeWindow();
                    break;

                case "snapshot.2d":
                    if ( snapshot2dFrame == null ) {
                        snapshot2dFrame = new Snapshot2dFrame( mController.getSubController( "snapshot.2d" ), fileDialog );
                    }
                    snapshot2dFrame.setPanelSize( modelPanel .getRenderedSize() );
                    snapshot2dFrame.pack();
                    if ( ! snapshot2dFrame .isVisible() )
                        snapshot2dFrame.repaint();
                    snapshot2dFrame.setVisible( true );
                    break;
                                
                
                case "configureShapes":
                    JDialog shapesDialog = shapesDialogs.get( system );
                    if ( shapesDialog == null ) {
                    	delegate = mController .getSubController( "symmetry." + system );
                        shapesDialog = new ShapesDialog( ArticleFrame.this, delegate );
                        shapesDialogs .put( system, shapesDialog );
                    }
                    shapesDialog .setVisible( true );
                    break;
                
                case "configureDirections":
                    JDialog symmetryDialog = directionsDialogs.get( system );
                    if ( symmetryDialog == null ) {
                    	delegate = mController .getSubController( "symmetry." + system );
                        symmetryDialog = new SymmetryDialog( ArticleFrame.this, delegate );
                        directionsDialogs .put( system, symmetryDialog );
                    }
                    symmetryDialog .setVisible( true );
                    break;
                    
                default:
                    if ( cmd .startsWith( "execCommandLine/" ) )
                    {
                        if ( mFile == null ) {
                            JOptionPane .showMessageDialog( ArticleFrame.this, "You must save your model before you can run a shell command.",
                                    "Command Failure", JOptionPane .ERROR_MESSAGE );
                            return;
                        }
                        String cmdLine = cmd .substring( "execCommandLine/" .length() );
                        cmdLine = cmdLine .replace( "{}", mFile .getName() );
                        logger.log( Level.INFO, "executing command line: " + cmdLine );
                        try {
                            Runtime .getRuntime() .exec( cmdLine, null, mFile .getParentFile() );
                        } catch ( IOException ioe ) {
                            System .err .println( "Runtime.exec() failed on " + cmdLine );
                            ioe .printStackTrace();
                        }
                    }
                    else if ( cmd .startsWith( "showProperties-" ) )
                    {
                        String key = cmd .substring( "showProperties-" .length() );
                        Controller subc = mController .getSubController( key + "Picking" );
                        JOptionPane .showMessageDialog( ArticleFrame.this, subc .getProperty( "objectProperties" ), "Object Properties",
                                JOptionPane.PLAIN_MESSAGE );
                    }
                    else
                        mController .actionPerformed( e .getSource(), e .getActionCommand() );
                    break;
                }
            }
        };

        // -------------------------------------- create panels and tools

        cameraController = mController .getSubController( "camera" );
        lessonController = mController .getSubController( "lesson" );
        lessonController .addPropertyListener( this );
        
        // Now the component containment hierarchy
        
        JPanel outerPanel = new JPanel( new BorderLayout() );
        setContentPane( outerPanel );
        {
            JPanel leftCenterPanel = new JPanel( new BorderLayout() );
            {
                Scene scene = ((Scene.Provider) mController) .getScene();
                RenderingViewer viewer = factory3d .createRenderingViewer( scene );
                modelPanel = new ModelPanel( mController, viewer, (ControlActions) this, false );
                leftCenterPanel .add( modelPanel, BorderLayout.CENTER );
            }
            outerPanel.add( leftCenterPanel, BorderLayout.CENTER );

            // String mag = props .getProperty( "default.magnification" );
            // if ( mag != null ) {
            // float magnification = Float .parseFloat( mag );
            // // TODO this seems to work, but ought not to!
            // viewControl .zoomAdjusted( (int) magnification );
            // }

            JPanel rightPanel = new JPanel( new BorderLayout() );
            {
                Scene scene = ((Scene.Provider) cameraController) .getScene();
                RenderingViewer viewer = factory3d .createRenderingViewer( scene );
                viewControl = new CameraControlPanel( viewer, cameraController );
                // this is probably moot for reader mode
                rightPanel .add( viewControl, BorderLayout.PAGE_START );
                
                JPanel lessonPanel = new LessonPanel( lessonController );
                lessonPanel .setMinimumSize( new Dimension( 400, 500 ) );
                lessonPanel .setPreferredSize( new Dimension( 400, 800 ) );
                rightPanel .add( lessonPanel, BorderLayout.CENTER );
            }
            outerPanel.add( rightPanel, BorderLayout.LINE_END );
        }

        JPopupMenu.setDefaultLightWeightPopupEnabled( false );
        ToolTipManager ttm = ToolTipManager.sharedInstance();
        ttm .setLightWeightPopupEnabled( false );

        this .setJMenuBar( new DocumentMenuBar( mController, this ) );

        this.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        this.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing( WindowEvent we )
            {
                closeWindow();
            }
        } );

		// Find the screen with the largest area if this is a multi-monitor system.
		// Set the frame size to just a bit smaller than the screen
		//	so the frame will fit on the screen if the user un-maximizes it.
		// Default to opening the window as maximized on the selected (or default) monitor.
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		if(gs.length > 0) {
			int bestIndex = 0;
			GraphicsDevice bestDevice = gs[bestIndex];
			DisplayMode bestMode = bestDevice.getDisplayMode();
			int bestArea = bestMode.getHeight() * bestMode.getWidth();
			for (int i = bestIndex+1; i < gs.length; i++) {
				GraphicsDevice testDevice = gs[i];
				DisplayMode testMode = testDevice.getDisplayMode();
				int testArea = testMode.getHeight() * testMode.getWidth();
				if(bestArea < testArea) {
					bestArea = testArea;					
					bestMode = testMode;					
					bestDevice = testDevice;
				}
			}
			Rectangle bounds = bestDevice.getDefaultConfiguration().getBounds();
			this.setLocation(bounds.x, bounds.y);
			int n = 15, d = n + 1; // set size to 15/16 of full screen size then maximize it
			this.setSize(bestMode.getWidth() * n/d, bestMode.getHeight() * n/d);
		}
		this.setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);

        this.pack();
        this.setVisible( true );
        this.setFocusable( true );


        new ExclusiveAction( this .getExcluder() )
        {
			@Override
            protected void doAction( ActionEvent e ) throws Exception
            {
                mController .actionPerformed( this, "finish.load" );

                String title = mController .getProperty( "window.title" );
                boolean migrated = mController .propertyIsTrue( "migrated" );
                
                boolean asTemplate = mController .propertyIsTrue( "as.template" );

                mController .actionPerformed( this, "switchToArticle" );
                migrated = false;

                if ( ! asTemplate && migrated ) { // a migration
                    final String NL = System .getProperty( "line.separator" );
                    if ( mController .propertyIsTrue( "autoFormatConversion" ) )
                    {
                        if ( mController .propertyIsTrue( "formatIsSupported" ) )
                            JOptionPane .showMessageDialog( ArticleFrame.this,
                                    "This document was created by an older version." + NL + 
                                    "If you save it now, it will be converted automatically" + NL +
                                    "to the current format.  It will no longer open using" + NL +
                                    "the older version.",
                                    "Automatic Conversion", JOptionPane.INFORMATION_MESSAGE );
                        else
                        {
                            title = null;
                            ArticleFrame.this .makeUnnamed();
                            JOptionPane .showMessageDialog( ArticleFrame.this,
                                    "You have \"autoFormatConversion\" turned on," + NL + 
                                    "but the behavior is disabled until this version of vZome" + NL +
                                    "is stable.  This converted document is being opened as" + NL +
                                    "a new document.",
                                    "Automatic Conversion Disabled", JOptionPane.INFORMATION_MESSAGE );
                        }
                    }
                    else
                    {
                        title = null;
                        ArticleFrame.this .makeUnnamed();
                        JOptionPane .showMessageDialog( ArticleFrame.this,
                                "This document was created by an older version." + NL + 
                                "It is being opened as a new document, so you can" + NL +
                                "still open the original using the older version.",
                                "Outdated Format", JOptionPane.INFORMATION_MESSAGE );
                    }
                }

                if ( title == null )
                    title = mController .getProperty( "untitled.title" );
                
                ArticleFrame.this .setTitle( title );
            }

			@Override
            protected void showError( Exception e )
            {
                JOptionPane .showMessageDialog( ArticleFrame.this,
                        e .getLocalizedMessage(),
                        "Error Loading Document", JOptionPane.ERROR_MESSAGE );
                // setting "visible" to FALSE will remove this document from the application controller's 
                // document collection so its document count is correct and it cleans up correctly 
                mController .setProperty( "visible", Boolean.FALSE );
                ArticleFrame.this .dispose();
            }
            
        } .actionPerformed( null );
    }

    private ExclusiveAction getExclusiveAction( final String action, final Controller controller )
    {
        return new ExclusiveAction( getExcluder() )
        {
			@Override
            protected void doAction( ActionEvent e ) throws Exception
            {
				controller .actionPerformed( e .getSource(), action );
            }

			@Override
            protected void showError( Exception e )
            {
                JOptionPane.showMessageDialog( ArticleFrame.this, e.getMessage(), "Command failure", JOptionPane.ERROR_MESSAGE );
            }
        };
    }
    
    @Override
    public AbstractButton setButtonAction( String command, Controller controller, AbstractButton control )
    {
        control .setActionCommand( command );
        boolean enable = true;
        switch ( command ) {

        case "save":
        case "saveAs":
        case "saveDefault":
            enable = false;
            break;

        default:
            if ( command .startsWith( "export." ) ) {
                enable = true;
            }
        }
        control .setEnabled( enable );
        if ( control .isEnabled() )
        {
            ActionListener actionListener = new ControllerActionListener(controller);
            switch ( command ) {

            // these can fall through to the ApplicationController
            case "quit":
            case "new":
            case "new-rootTwo":
            case "new-rootThree":
            case "new-heptagon":
            case "new-snubDodec":
            case "openURL":
            case "showAbout":

                // these will be handled by the DocumentController
            case "toggleWireframe":
            case "toggleOrbitViews":
            case "toggleStrutScales":
            case "toggleFrameLabels":
            case "toggleNormals":
            case "toggleOutlines":
                break;

            case "open":
            case "newFromTemplate":
            case "openDeferringRedo":
                actionListener = new ControllerFileAction( fileDialog, true, command, "vZome", controller );
                break;

            case "close":
            case "snapshot.2d":
            case "configureShapes":
            case "configureDirections":
                actionListener = this .localActions;
                break;

            case "capture-animation":
                actionListener = new ControllerFileAction( fileDialog, false, command, "png", controller );
                break;

            default:
                if ( command .startsWith( "openResource-" ) ) {
                }
                else if ( command .startsWith( "LoadVEF/" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "execCommandLine/" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "showProperties-" ) ) {
                    actionListener = this .localActions;
                }
                else if ( command .startsWith( "capture." ) ) {
                    String ext = command .substring( "capture." .length() );
                    actionListener = new ControllerFileAction( fileDialog, false, command, ext, controller );
                }
                else if ( command .startsWith( "export2d." ) ) {
                    String ext = command .substring( "export2d." .length() );
                    actionListener = new ControllerFileAction( fileDialog, false, command, ext, controller );
                }
                else if ( command .startsWith( "export." ) ) {
                    String ext = command .substring( "export." .length() );
                    ext = controller .getProperty( "exportExtension." + ext );
                    switch ( ext ) {
                    case "vrml": ext = "wrl"; break;
                    case "size": ext = "txt"; break;
                    case "partslist": ext = "txt"; break;
                    case "partgeom": ext = "vef"; break;
                    default:
                        break;
                    }
                    actionListener = new ControllerFileAction( fileDialog, false, command, ext, controller );
                }
                else {
                    actionListener = getExclusiveAction( command, controller );
                    this .getExcluder() .addExcludable( control );
                }
                break;
            }
            control .addActionListener( actionListener );
        }
        return control;
    }
    
    @Override
    public JMenuItem setMenuAction( String command, Controller controller, JMenuItem menuItem )
    {
        return (JMenuItem) this .setButtonAction( command, controller, menuItem );
    }

	@Override
    public void propertyChange( PropertyChangeEvent e )
    {
		switch ( e .getPropertyName() ) {

		case "window.title":
            this .setTitle( e .getNewValue() .toString() );
            break;
			
		case "visible":
			if ( Boolean.TRUE .equals( e .getNewValue() ) ) {
				this .appUI .propertyChange( e ); // remove this window from the UI's collection
				this .setVisible( true );
			}
            break;
		}
    }

	// called from ApplicationUI on quit
	//
	boolean closeWindow()
	{
	    dispose();
	    mController .setProperty( "visible", Boolean.FALSE );
	    return true;
	}

	public void makeUnnamed()
	{
		this .mFile = null;
	}
}
