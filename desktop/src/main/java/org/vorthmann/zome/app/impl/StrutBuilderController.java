
package org.vorthmann.zome.app.impl;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.vecmath.Quat4d;

import org.vorthmann.j3d.MouseTool;
import org.vorthmann.j3d.MouseToolDefault;
import org.vorthmann.j3d.MouseToolFilter;
import org.vorthmann.j3d.Trackball;
import org.vorthmann.ui.DefaultController;
import org.vorthmann.ui.LeftMouseFilter;
import org.vorthmann.ui.MouseDragAdapter;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.construction.Point;
import com.vzome.core.math.Line;
import com.vzome.core.model.Connector;
import com.vzome.core.model.Manifestation;
import com.vzome.core.render.ManifestationPicker;
import com.vzome.core.render.RenderingChanges;
import com.vzome.core.viewing.Lights;
import com.vzome.desktop.controller.CameraController;
import com.vzome.desktop.controller.Controller3d;
import com.vzome.desktop.controller.RenderingViewer;

public class StrutBuilderController extends DefaultController implements Controller3d
{    
    private boolean useGraphicalViews = false;

    private boolean showStrutScales = false;

    private boolean useWorkingPlane = false;

    private AlgebraicVector workingPlaneAxis = null;

    private PreviewStrut previewStrut;

    private MouseTool previewStrutRoll, previewStrutPlanarDrag, previewStrutLength;

    private DocumentController docController;

    private CameraController cameraController;

    private Component canvas;

    private ManifestationPicker picker;

    public StrutBuilderController( DocumentController docController, CameraController cameraController )
    {
        super();
        this .docController = docController;
        this .cameraController = cameraController;
    }

    public StrutBuilderController withGraphicalViews( boolean value )
    {
        this .useGraphicalViews = value;
        return this;
    }

    public StrutBuilderController withShowStrutScales( boolean value )
    {
        this .showStrutScales = value;
        return this;
    }

    @Override
    public String getProperty( String propName )
    {
        switch ( propName ) {

        case "useGraphicalViews":
            return Boolean.toString( this.useGraphicalViews );

        case "useWorkingPlane":
            return Boolean .toString( useWorkingPlane );

        case "workingPlaneDefined":
            return Boolean .toString( workingPlaneAxis != null );

        case "showStrutScales":
            return Boolean.toString( this.showStrutScales );

        default:
            return super .getProperty( propName );
        }
    }
    @Override
    public void setModelProperty( String name, Object value )
    {
        switch ( name ) {

        case "useGraphicalViews": {
            boolean old = useGraphicalViews;
            this.useGraphicalViews = "true".equals( value );
            firePropertyChange( name, old, this.useGraphicalViews );
            break;
        }
            
        case "showStrutScales": {
            boolean old = showStrutScales;
            this.showStrutScales = "true" .equals( value );
            firePropertyChange( name, old, this.showStrutScales );
            break;
        }

        default:
            super .setModelProperty( name, value );
        }
    }
    
    @Override
    public void doAction( String action, ActionEvent e ) throws Exception
    {
        switch ( action ) {

        case "toggleWorkingPlane":
            useWorkingPlane = ! useWorkingPlane;
            break;

        case "toggleOrbitViews": {
            boolean old = useGraphicalViews;
            useGraphicalViews = ! old;
            firePropertyChange( "useGraphicalViews", old, this.useGraphicalViews );
            break;
        }

        case "toggleStrutScales": {
            boolean old = showStrutScales;
            showStrutScales = ! old;
            firePropertyChange( "showStrutScales", old, this.showStrutScales );
            break;
        }

        default:
            super .doAction( action, e );
        }
    }
    
    @Override
    public void attachViewer( RenderingViewer viewer, RenderingChanges scene, Component canvas )
    {
        this .canvas = canvas;
        this .picker = viewer;

        // The preview strut rendering is the main reason we distinguish the mainScene as a listener
        AlgebraicField field = this .docController .getModel() .getField();
        this .previewStrut = new PreviewStrut( field, scene, cameraController );

        this .previewStrutLength = new MouseToolFilter( cameraController .getZoomScroller() )
        {
            @Override
            public void mouseWheelMoved( MouseWheelEvent e )
            {
                LengthController length = previewStrut .getLengthModel();
                if ( length != null )
                {
                    // scroll to scale the preview strut (when it is rendered)
                    length .getMouseTool() .mouseWheelMoved( e );
                    // don't adjustPreviewStrut() here, let the prop change trigger it,
                    // so we don't flicker for every tick of the mousewheel
                }
                else
                {
                    // no strut build in progress, so zoom the view
                    super .mouseWheelMoved( e );
                }
            }
        };

        // trackball to adjust the preview strut (when it is rendered)
        this .previewStrutRoll = new LeftMouseFilter( new MouseDragAdapter( new Trackball()
        {
            @Override
            protected void trackballRolled( Quat4d roll )
            {
                previewStrut .trackballRolled( roll );
            }
        } ) );
        
        // working plane drag events to adjust the preview strut (when it is rendered)
        this .previewStrutPlanarDrag = new LeftMouseFilter( new MouseDragAdapter( new MouseToolDefault()
        {
            @Override
            public void mouseDragged( MouseEvent e )
            {
                Line ray = viewer .pickRay( e );
                previewStrut .workingPlaneDrag( ray );
            }
        } ) );
    }

    @Override
    public Lights getSceneLighting()
    {
        return null;
    }

    @Override
    public MouseTool getMouseTool()
    {
        return new MouseDragAdapter( new MouseToolFilter( this .cameraController .getTrackball( 0.7 ) ) // could use getMouseTool(), if it were implemented
        {
            private boolean draggingStrut;

            @Override
            public void mousePressed( MouseEvent e )
            {
                this .draggingStrut = false;
                Manifestation target = picker .pickManifestation( e );
                if ( target != null && ( target instanceof Connector ) ) {
                    this .draggingStrut  = true;

                    mErrors .clearError();
                    Point point = (Point) target .getFirstConstruction();
                    AlgebraicVector workingPlaneNormal = null;
                    if ( useWorkingPlane && (workingPlaneAxis != null ) )
                        workingPlaneNormal = workingPlaneAxis;
                    previewStrut .startRendering( docController .getSymmetryController(), point, workingPlaneNormal );

                    previewStrutRoll .startHandlingMouseEvents( canvas );
                    previewStrutPlanarDrag .startHandlingMouseEvents( canvas );
                    previewStrutLength .startHandlingMouseEvents( canvas );
                }
                else {
                    super .mousePressed( e ); // let the camera trackball handle
                }
            }

            @Override
            public void mouseDragged( MouseEvent e )
            {
                if ( ! this .draggingStrut )
                    super .mouseDragged( e ); // let the camera trackball handle
            }

            @Override
            public void mouseReleased( MouseEvent e )
            {
                if ( this .draggingStrut  ) {
                    previewStrutRoll .stopHandlingMouseEvents( canvas );
                    previewStrutPlanarDrag .stopHandlingMouseEvents( canvas );
                    previewStrutLength .stopHandlingMouseEvents( canvas );

                    previewStrut .finishPreview( docController .getModel() );

                    this .draggingStrut = false;
                }
                else
                    super .mouseReleased( e ); // let the camera trackball handle
            }
        } );
    }

    public void setSymmetryController( SymmetryController symmetryController )
    {
        if ( previewStrut != null )
            previewStrut .setSymmetryController( symmetryController );
    }

    public void setWorkingPlaneAxis( AlgebraicVector axis )
    {
        this .workingPlaneAxis = axis;
        this .firePropertyChange( "workingPlaneDefined", false, true );
    }
}
