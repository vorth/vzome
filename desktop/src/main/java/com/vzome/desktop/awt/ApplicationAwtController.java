package com.vzome.desktop.awt;

import java.lang.reflect.Constructor;
import java.util.Properties;

import org.vorthmann.j3d.J3dComponentFactory;
import org.vorthmann.zome.app.impl.DocumentController;

import com.vzome.core.editor.DocumentModel;
import com.vzome.core.render.Colors;
import com.vzome.desktop.controller.ApplicationController;

public class ApplicationAwtController extends ApplicationController
{    
    public interface UI
    {
        public void doAction( String action );
    }

    private final UI ui;
    private J3dComponentFactory rvFactory;

    public ApplicationAwtController( UI ui, Properties commandLineArgs, J3dComponentFactory rvFactory )
    {
        super( commandLineArgs );
        this .ui = ui;
        this .rvFactory = rvFactory;
    }

    public J3dComponentFactory getJ3dFactory()
    {
        if ( rvFactory == null )
        {
            Colors colors = modelApp .getColors();
            boolean useEmissiveColor = ! propertyIsTrue( "no.glowing.selection" );
            // need this set up before we do any loadModel
            String factoryName = getProperty( "RenderingViewer.Factory.class" );
            if ( factoryName == null )
                factoryName = "org.vorthmann.zome.render.jogl.JoglFactory";
            try {
                Class<?> factoryClass = Class.forName( factoryName );
                Constructor<?> constructor = factoryClass .getConstructor( new Class<?>[] { Colors.class, Boolean.class } );
                this .rvFactory = (J3dComponentFactory) constructor.newInstance( new Object[] { colors, useEmissiveColor } );
            } catch ( Exception e ) {
                mErrors.reportError( "Unable to instantiate RenderingViewer.Factory class: " + factoryName, new Object[] {} );
                System.exit( 0 );
            }
        }
        return this .rvFactory;
    }

    @Override
    protected void newDocumentController( final String name, final DocumentModel document, final Properties props )
    {
        DocumentController newest = new DocumentAwtController( document, this, props );
        super .registerDocumentController( name, newest );
        // trigger window creation in the UI
        this .firePropertyChange( "newDocument", null, newest );
    }

    @Override
    public void doAction( String action )
    {
        if ( action .equals( "showAbout" ) 
                || action .equals( "openURL" ) 
                || action .equals( "quit" )
                || action .startsWith( "browse-" )
                )
        {
            this .ui .doAction( action );
        }
        else
            super .doAction( action );
    }
}
