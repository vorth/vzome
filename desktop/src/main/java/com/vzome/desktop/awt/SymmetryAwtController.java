
//(c) Copyright 2007, Scott Vorthmann.  All rights reserved.

package com.vzome.desktop.awt;

import org.vorthmann.ui.Controller;
import org.vorthmann.zome.app.impl.LengthController;

import com.vzome.core.editor.SymmetrySystem;
import com.vzome.core.math.symmetry.Direction;
import com.vzome.core.render.RenderedModel;
import com.vzome.desktop.controller.SymmetryController;

public class SymmetryAwtController extends SymmetryController
{
    public OrbitSetController availableController;
    public OrbitSetController snapController;
    public OrbitSetController buildController;
    public OrbitSetController renderController;

    public SymmetryAwtController( Controller parent, SymmetrySystem model, RenderedModel mRenderedModel )
    {
        super( parent, model, mRenderedModel );
        availableController = new OrbitSetController( availableOrbits, this .symmetrySystem .getOrbits(), this .symmetrySystem, false );
        this .addSubController( "availableOrbits", availableController );
        snapController = new OrbitSetController( snapOrbits, availableOrbits, this .symmetrySystem, false );
        this .addSubController( "snapOrbits", snapController );
        buildController = new OrbitSetController( buildOrbits, availableOrbits, this .symmetrySystem, true );
        this .addSubController( "buildOrbits", buildController );
        renderController = new OrbitSetController( renderOrbits, this .symmetrySystem .getOrbits(), this .symmetrySystem, false );
        this .addSubController( "renderOrbits", renderController );
    }

    @Override
    public LengthController getLengthController( Direction dir )
    {
        LengthController result = super .getLengthController( dir );
        buildController .addSubController( "length." + dir .getName(), result ); // idempotent
        return result;
    }

    @Override
    public void doAction( String action ) throws Exception
    {
        switch ( action ) {

        case "rZomeOrbits":
        case "predefinedOrbits":
        case "setNoDirections":
        case "setAllDirections":
            availableController .doAction( action );
            break;

        default:
            if ( action .startsWith( "enableDirection." ) )
                availableController .doAction( action );
            else if ( action .startsWith( "setSingleDirection." ) )
                buildController .doAction( action );
            else
                super .doAction( action );
        }
    }
}
