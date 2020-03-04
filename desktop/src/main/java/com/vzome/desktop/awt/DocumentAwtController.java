package com.vzome.desktop.awt;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.vorthmann.zome.app.impl.DocumentController;

import com.vzome.core.editor.DocumentModel;
import com.vzome.core.editor.SymmetrySystem;
import com.vzome.core.math.Polyhedron;
import com.vzome.core.math.symmetry.Direction;
import com.vzome.core.render.RenderedManifestation;
import com.vzome.desktop.controller.SymmetryController;
import com.vzome.desktop.controller.ThumbnailRendererImpl;

public class DocumentAwtController extends DocumentController
{
    private Java2dSnapshotController java2dController = null;

    public DocumentAwtController( DocumentModel document, ApplicationAwtController app, Properties props )
    {
        super( document, app, props );

        java2dController = new Java2dSnapshotController( this .documentModel, cameraController.getView(), this.sceneLighting,
                this.currentSnapshot, this.drawOutlines );
        this .addSubController( "snapshot.2d", java2dController );

        thumbnails = new ThumbnailRendererImpl( app .getJ3dFactory(), sceneLighting );
    }
    
    @Override
    protected SymmetryController createSymmetryController( SymmetrySystem system )
    {
        return new SymmetryAwtController( this, system, mRenderedModel );
    }

    @Override
    public void doAction( String action ) throws Exception
    {
        switch ( action ) {
        
        case "usedOrbits":
            {
                Set<Direction> usedOrbits = new HashSet<>();
                for ( RenderedManifestation rm : mRenderedModel ) {
                    Polyhedron shape = rm .getShape();
                    Direction orbit = shape .getOrbit();
                    if ( orbit != null )
                        usedOrbits .add( orbit );
                }
                symmetryController .doAction( "setNoDirections" );
                for ( Direction orbit : usedOrbits ) {
                    symmetryController .doAction( "enableDirection." + orbit .getName() );
                }
            }
            break;

        case "refresh.2d":
            this .java2dController .setScene( cameraController.getView(), this.sceneLighting, this.currentSnapshot, this.drawOutlines );
            break;

        default:
            super .doAction( action );
        }
    }
}
