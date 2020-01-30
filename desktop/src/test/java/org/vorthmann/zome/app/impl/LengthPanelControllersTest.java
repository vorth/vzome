package org.vorthmann.zome.app.impl;

import static com.vzome.controller.ControllerTesting.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;
import org.vorthmann.ui.Controller;

import com.vzome.core.editor.FieldApplication;
import com.vzome.core.editor.FieldApplication.SymmetryPerspective;
import com.vzome.core.editor.SymmetrySystem;
import com.vzome.core.kinds.HeptagonFieldApplication;
import com.vzome.core.render.Colors;

public class LengthPanelControllersTest
{
    @Test
    public void testSwitchOrbit() 
    {
        FieldApplication app = new HeptagonFieldApplication();
        SymmetryPerspective perspective = app .getDefaultSymmetryPerspective();
        SymmetrySystem system = new SymmetrySystem( null, perspective, null, new Colors( new Properties() ), true );

        Controller strutBuilder = new StrutBuilderController( null, null ) .withShowStrutScales( true );
        Controller symmController = new SymmetryController( strutBuilder, system, null );
        Controller buildOrbits = getSubController( symmController, "buildOrbits" );
        assertNotNull( buildOrbits );
        
        Controller red = getSubController( symmController, "length.red" );
        assertNotNull( red );
        
        Controller blue = getSubController( symmController, "length.blue" );
        assertNotNull( blue );
        
        Controller currentLength = getSubController( buildOrbits, "currentLength" );
        assertEquals( currentLength, red );

        Controller redScale = getSubController( red, "scale" );
        assertNotNull( redScale );
        assertPropertyValue( redScale, "scale", "0" );
        doActions( currentLength, "scale=2" );
        doActions( currentLength, "scaleUp" );
        assertPropertyValue( redScale, "scale", "3" );
        
        Controller blueScale = getSubController( blue, "scale" );
        assertNotNull( blueScale );
        assertPropertyValue( blueScale, "scale", "0" );
        
        doActions( buildOrbits, "setSingleDirection.blue" );
        
        currentLength = getSubController( buildOrbits, "currentLength" );
        assertEquals( currentLength, blue );

        doActions( currentLength, "scale=1" );
        assertPropertyValue( blueScale, "scale", "1" );
        
        doActions( buildOrbits, "setSingleDirection.red" );
        
        currentLength = getSubController( buildOrbits, "currentLength" );
        assertEquals( currentLength, red );

        assertPropertyValue( redScale, "scale", "3" );
        assertPropertyValue( blueScale, "scale", "1" );
    }
}
