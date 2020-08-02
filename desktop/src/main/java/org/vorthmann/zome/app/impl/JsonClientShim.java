package org.vorthmann.zome.app.impl;

import java.io.File;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.vorthmann.j3d.J3dComponentFactory;
import org.vorthmann.ui.Controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vzome.core.render.RenderedModel;
import com.vzome.core.render.Scene;
import com.vzome.desktop.controller.JsonClientRendering;
import com.vzome.desktop.controller.RenderingViewer;

public abstract class JsonClientShim implements JsonClientRendering.EventDispatcher
{
    protected final ApplicationController applicationController;
    private final ObjectMapper objectMapper = new ObjectMapper();
    protected final ObjectWriter objectWriter = objectMapper .writer();
    
    private final static Logger rootLogger = Logger .getLogger("");

    public JsonClientShim( String logLevel )
    {
        System .setProperty( "java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n" );
        Level minLevel = Level.parse( logLevel );
        if ( rootLogger .getLevel() .intValue() > minLevel.intValue() )
        {
            rootLogger.setLevel( minLevel );  // Set minimum logging level
        }

        // If a ConsoleHandler is already pre-configured by the logging.properties file then just use it as-is.
        ConsoleHandler consoleHandler = null;
        for ( Handler handler : rootLogger.getHandlers() ) {
            if  (handler.getClass().isAssignableFrom( ConsoleHandler.class ) ) {
                consoleHandler = (ConsoleHandler) handler;
                break;
            }
        }

        // If no ConsoleHandler was pre-configured, then initialze our own default
        if ( consoleHandler == null )
        {
            try {
                consoleHandler = new ConsoleHandler();
            } catch (Exception e1) {
                rootLogger.log( Level.WARNING, "unable to set up syserr console log handler", e1 );
            }
            if (consoleHandler != null) {
                consoleHandler .setFormatter( new SimpleFormatter() );
                rootLogger .addHandler( consoleHandler );
            }
        }
        if (consoleHandler != null)
            consoleHandler .setLevel( minLevel );

        Properties props = new Properties();
        props .setProperty( "entitlement.model.edit", "true" );
        props .setProperty( "keep.alive", "true" );

        this .applicationController = new ApplicationController( new ApplicationController.UI()
        {   
            @Override
            public void doAction( String action )
            {
                rootLogger .warning( "WARNING: Unhandled UI event: " + action );
            }
        }, props, new J3dComponentFactory()
        {
            @Override
            public RenderingViewer createRenderingViewer( Scene scene )
            {
                rootLogger .warning( "WARNING: createRenderingViewer called" );
                return null;
            }
        });
        this .applicationController .setErrorChannel( new Controller.ErrorChannel()
        {
            @Override
            public void reportError(String errorCode, Object[] arguments)
            {
                rootLogger .severe( errorCode );
                dispatchEvent( "GENERAL_ERROR", errorCode );
                if ( arguments .length > 0 ) {
                    Object arg = arguments[ 0 ];
                    if ( arg instanceof Throwable ) {
                        ((Throwable) arg) .printStackTrace();
                    }
                }
            }

            @Override
            public void clearError() {}
        });
        if ( rootLogger .isLoggable( Level.INFO ) ) rootLogger .info( "ApplicationController created" );
    }

    public void dispatchEventNode( ObjectNode event )
    {
        try {
            String eventStr = this .objectWriter .writeValueAsString( event );
            dispatchSerializedJson( eventStr );
        } catch (JsonProcessingException e) {
            e .printStackTrace();
            rootLogger .severe( "dispatchEventNode failed: " + e .getMessage() );
        }
    }

    public abstract void dispatchSerializedJson( String eventStr );

    public void dispatchEvent( String type, JsonNode payload )
    {
        ObjectNode event = this .objectMapper .createObjectNode();
        event .put( "type", type );
        event .set( "payload", payload );
        dispatchEventNode( event );
    }

    private void dispatchEvent( String type, String payload )
    {
        ObjectNode event = this .objectMapper .createObjectNode();
        event .put( "type", type );
        event .put( "payload", payload );
        dispatchEventNode( event );
    }

    protected DocumentController renderDocument( String path )
    {
        DocumentController docController = (DocumentController) this .applicationController .getSubController( path );
        if ( docController == null ) {
            rootLogger .severe( "Document load FAILURE: " + path );
            dispatchEvent( "LOAD_FAILED", "Document load FAILURE: " + path );
            return null;
        }
        String bkgdColor = docController .getProperty( "backgroundColor" );
        if ( bkgdColor != null ) {
            dispatchEvent( "BACKGROUND_SET", bkgdColor );
        }
        // TODO: define a callback to support the ControllerWebSocket case?
        //        consumer.start();
        JsonClientRendering clientRendering = new JsonClientRendering( this );
        RenderedModel renderedModel = docController .getModel() .getRenderedModel();
        renderedModel .addListener( clientRendering );
        RenderedModel .renderChange( new RenderedModel( null, null ), renderedModel, clientRendering ); // get the origin ball
        try {
            docController .actionPerformed( this, "finish.load" );
            if ( rootLogger .isLoggable( Level.INFO ) ) rootLogger .info( "Document load success: " + path );
            dispatchEvent( "MODEL_LOADED", "" );
        } catch ( Exception e ) {
            e.printStackTrace();
            rootLogger .severe( "Document load unknown FAILURE: " + path );
            dispatchEvent( "LOAD_FAILED", "Document load unknown FAILURE: " + path );
        }
        return docController;
    }

    public static void main( String[] args )
    {
        try {
            JsonClientShim shim = new JsonClientShim( "INFO" )
            {
                @Override
                public void dispatchSerializedJson( String eventStr )
                {
                    rootLogger .fine( eventStr );
                }
            };
            
            String path = "/Users/vorth/Downloads/greenTetra.vZome";
            shim .applicationController .doFileAction( "open", new File( path ) );
            DocumentController documentController = shim .renderDocument( path );

            documentController .doFileAction( "export.dae", new File( "/Users/vorth/Downloads/greenTetra.dae" ) );
        } catch (Throwable t) {
            t .printStackTrace();
        }
    }
}