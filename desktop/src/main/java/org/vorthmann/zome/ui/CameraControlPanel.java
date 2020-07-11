package org.vorthmann.zome.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.vorthmann.ui.Controller;

import com.vzome.desktop.controller.CameraController;
import com.vzome.desktop.controller.RenderingViewer;

/**
 * Description here.
 * 
 * @author Scott Vorthmann 2003
 */
public class CameraControlPanel extends JPanel {

    protected JSlider zslider;
    
//    protected final ViewPlatformTrackball mTrackball;
//    
//    protected ViewPlatformTrackball.Snapper mSnapper;
//	
//    private ViewPlatformModel mView;
    
    private static final float MAG_PER_TICKS = -50f, MAX_MAG = 4f, MIN_MAG = -2f;

    private JPanel trackpad;
    
    private static int magToTicks( float magnification )
    {
    	magnification = Math.max( magnification, MIN_MAG );
    	magnification = Math.min( magnification, MAX_MAG );
        return Math.round( MAG_PER_TICKS * ( magnification - 1f ) );
    }
    
    private static float ticksToMag( int ticks )
    {
        return ( ticks / MAG_PER_TICKS ) + 1f;
    }
   
    
//    public float zoomAdjusted( int amt )
//    {
//        int curAmt = zslider .getValue();
//        zslider .setValue( curAmt - amt );
//        return ticksToMag( zslider .getValue() );
//    }
    
    
    public CameraControlPanel( RenderingViewer viewer, final Controller controller )
	{
        this .setBorder( BorderFactory .createTitledBorder( "viewing" ) );
        int nearTicks = magToTicks( MAX_MAG );
        int farTicks = magToTicks( MIN_MAG );
        float startMag = Float .parseFloat( controller .getProperty( "magnification" ) );
        int startTicks = magToTicks( startMag );
        
        final boolean isEditor = controller .userHasEntitlement( "lesson.edit" ) && ! controller .propertyIsTrue( "reader.preview" );
        zslider = new JSlider( JSlider .VERTICAL, nearTicks, farTicks, startTicks );
        zslider .addChangeListener( new ChangeListener()
        {
            @Override
            public  void stateChanged( ChangeEvent e )
            {
                controller .setProperty( "magnification", Float .toString( ticksToMag( zslider .getValue() ) ) );
                //      		if ( mView != null )
                //      		mView .setMagnification( ticksToMag( zslider .getValue() ) );
            }
        } );
        zslider .setBorder( BorderFactory .createTitledBorder( "distance" ) );
        zslider .setMajorTickSpacing( 50 );
        zslider .setMinorTickSpacing( 10 );

        Hashtable<Integer, JComponent> labelTable = new Hashtable<>();
        labelTable.put( nearTicks, new JLabel("far") );  // I don't know why these are reversed
        labelTable.put( farTicks, new JLabel("near") );
        zslider.setLabelTable( labelTable );

        zslider .setPaintTicks( true );
        zslider .setPaintLabels( true );
        zslider .setToolTipText( "adjust viewing distance" );

        setLayout( new BorderLayout() );
        if ( isEditor )
            add( zslider, BorderLayout .EAST );

        trackpad = new JPanel( new BorderLayout() );
        Component trackballCanvas = viewer .getCanvas();
        ((CameraController) controller) .attachViewer( viewer, trackballCanvas );

        trackpad .add( trackballCanvas, BorderLayout.CENTER );
        trackpad .setAlignmentX( JLabel .CENTER_ALIGNMENT );
        trackpad .setBorder( BorderFactory .createTitledBorder( "rotation trackball" ) );
        trackpad .setMinimumSize( new Dimension( 100, 100 ) );
        trackpad .setPreferredSize( new Dimension( 200, 200 ) );
        if ( isEditor )
            add( trackpad, BorderLayout .CENTER );

        JPanel topPanel = new JPanel( new GridLayout( 2, 1 ) ); // one column, 2 rows
        add( topPanel, BorderLayout .NORTH );
                
        JPanel checkboxesPanel = new JPanel();
        topPanel .add( checkboxesPanel );
        
        ActionListener actionListener = new ControllerActionListener( controller );

        final JCheckBox perspectiveCheckbox = new JCheckBox( "perspective" );
        perspectiveCheckbox .addActionListener( actionListener );
        perspectiveCheckbox .setActionCommand( "togglePerspective" );
        perspectiveCheckbox .setSelected( "true" .equals( controller .getProperty( "perspective" ) ) );
        checkboxesPanel .add( perspectiveCheckbox );

        final JCheckBox outlinesCheckbox = new JCheckBox( "outlines" );
        outlinesCheckbox .addActionListener( actionListener );
        outlinesCheckbox .setActionCommand( "toggleOutlines" );
        outlinesCheckbox .setSelected( "true" .equals( controller .getProperty( "docDrawOutlines" ) ) );
        checkboxesPanel .add( outlinesCheckbox );

        checkboxesPanel = new JPanel();
        topPanel .add( checkboxesPanel );

        final JCheckBox snapperCheckbox = new JCheckBox( "snap" );
        snapperCheckbox .addActionListener( actionListener );
        snapperCheckbox .setActionCommand( "toggleSnap" );
        snapperCheckbox .setSelected( "true" .equals( controller .getProperty( "snap" ) ) );
        checkboxesPanel .add( snapperCheckbox );

        final JCheckBox wiggleCheckbox = new JCheckBox( "wiggle" );
        wiggleCheckbox .addActionListener( actionListener );
        wiggleCheckbox .setActionCommand( "toggleWiggle" );
        wiggleCheckbox .setSelected( "true" .equals( controller .getProperty( "wiggle" ) ) );
        checkboxesPanel .add( wiggleCheckbox );
        
        this .addMouseWheelListener( new MouseWheelListener()
        {
            @Override
            public void mouseWheelMoved( MouseWheelEvent e )
            {
                int amt = e .getWheelRotation();
                int curAmt = zslider .getValue();
                zslider .setValue( curAmt - amt );
//                controller .setProperty( "magnification", Float .toString( ticksToMag( curAmt - amt ) ) );
            }
        } );
        
        // TODO this is silly, to have both getProperty and addPropertyListener
        perspectiveCheckbox .setSelected( "true" .equals( controller .getProperty( "perspective" ) ) );
        snapperCheckbox .setSelected( "true" .equals( controller .getProperty( "snap" ) ) );
        wiggleCheckbox .setSelected( "true" .equals( controller .getProperty( "wiggle" ) ) );

        controller .addPropertyListener( new PropertyChangeListener()
        {
            @Override
            public void propertyChange( PropertyChangeEvent e )
            {
                if ( "magnification" .equals(  e .getPropertyName() ) ) {
                    float mag = Float .parseFloat( (String) e .getNewValue() );
                    zslider .setValue( magToTicks( mag ) );
                }
                else if ( "perspective" .equals(  e .getPropertyName() ) ) {
                    boolean enabling = ((Boolean) e .getNewValue()) .booleanValue();
                    perspectiveCheckbox .setSelected( enabling );
                }
                else if ( "snap" .equals( e .getPropertyName() ) ) {
                    boolean enabling = ((Boolean) e .getNewValue()) .booleanValue();
                    snapperCheckbox .setSelected( enabling );
                }
                else if ( "wiggle" .equals( e .getPropertyName() ) ) {
                    boolean enabling = ((Boolean) e .getNewValue()) .booleanValue();
                    wiggleCheckbox .setSelected( enabling );
                }
                else if ( "drawOutlines" .equals( e .getPropertyName() ) ) {
                    boolean enabling = ((Boolean) e .getNewValue()) .booleanValue();
                    outlinesCheckbox .setSelected( enabling );
                }
                else if ( "editor.mode" .equals( e .getPropertyName() ) )
                {
                    if ( isEditor )
                        switchToMode( (String) e .getNewValue() );
                }
            }
        } );
    }

	protected void switchToMode( String mode )
	{
	    if ( "model" .equals( mode ) )
	    {
            this .trackpad .setVisible( true );
            this .zslider .setVisible( true );
	    }
	    else
        {
            this .trackpad .setVisible( false );
            this .zslider .setVisible( false );
        }
	}
}
