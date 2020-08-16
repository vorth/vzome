/*
 * Created on Jul 22, 2004
 */
package com.vzome.core.viewing;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Vector3f;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.vzome.core.model.Color;
import com.vzome.xml.DomUtils;

/**
 * This is really a SceneModel
 * @author Scott Vorthmann
 *
 */
public class Lights implements Iterable<Lights.DirectionalLight>
{
    public static class DirectionalLight
    {
        public DirectionalLight( Vector3f direction, Color color )
        {
            super();
            this.direction = direction;
            this.color = color;
        }

        public Vector3f direction;
        public Color color;
    }

    private final PropertyChangeSupport pcs = new PropertyChangeSupport( this );

    public void addPropertyListener( PropertyChangeListener listener )
    {
        pcs .addPropertyChangeListener( listener );
    }

    public void removePropertyListener( PropertyChangeListener listener )
    {
        pcs .removePropertyChangeListener( listener );
    }

    public void setProperty( String cmd, Object value )
    {
        if ( "backgroundColor".equals( cmd ) ) {
            this .backgroundColor = new Color( Integer .parseInt( (String) value, 16 ) );
            pcs .firePropertyChange( cmd, null, value );
        }
    }

    protected final List<DirectionalLight> directionalLights = new ArrayList<>(3);

    protected Color mAmbientLightColor;

    private Color backgroundColor;


    public Lights()
    {
        super();
    }


    public Lights( Lights prototype )
    {
        this();

        this .backgroundColor = prototype .backgroundColor;
        this .mAmbientLightColor = prototype .mAmbientLightColor;
        for ( int i = 0; i < prototype .directionalLights .size(); i++ ) {
            addDirectionLight( prototype .directionalLights .get(i) );
        }
    }


    public Lights( Element element )
    {
        this();
        String str = element .getAttribute( "background" );
        this .backgroundColor = Color .parseColor( str );
        str = element .getAttribute( "ambientLight" );
        this .mAmbientLightColor = Color .parseColor( str );
        NodeList nodes = element .getChildNodes();
        for ( int i = 0; i < nodes .getLength(); i++ ) {
            Node node = nodes .item( i );
            if ( node instanceof Element ) {
                Element viewElem = (Element) node;
                str = viewElem .getAttribute( "color" );
                Color color = Color .parseColor( str );
                Vector3f pos = new Vector3f( 
                        Float .parseFloat( viewElem .getAttribute( "x" ) ),  
                        Float .parseFloat( viewElem .getAttribute( "y" ) ),  
                        Float .parseFloat( viewElem .getAttribute( "z" ) )
                        );
                addDirectionLight( new DirectionalLight( pos, color ) );
            }
        }
    }

    public int size()
    {
        return this .directionalLights .size();
    }

    public final void addDirectionLight( DirectionalLight light )
    {
        directionalLights .add( light );
    }

    public void setAmbientColor( Color color )
    {
        mAmbientLightColor = color;
    }

    public Color getAmbientColor()
    {
        return mAmbientLightColor;
    }
    
    public void getDirectionalLights()
    {
        
    }

    public Color getDirectionalLight( int i, Vector3f direction )
    {
        DirectionalLight light = this .directionalLights .get( i );
        direction .set( light .direction );
        return light .color;
    }

    public Color getBackgroundColor()
    {
        return this .backgroundColor;
    }

    public void setBackgroundColor( Color color )
    {
        this .backgroundColor = color;
    }

    public Element getXml( Document doc )
    {
        Element result = doc .createElement( "sceneModel" );
        DomUtils .addAttribute( result, "ambientLight", mAmbientLightColor .toString() );
        DomUtils .addAttribute( result, "background", backgroundColor .toString() );
        for ( int i = 0; i < this .directionalLights .size(); i++ ) {
            DirectionalLight light = this .directionalLights .get( i );
            Element child = doc .createElement( "directionalLight" );
            DomUtils .addAttribute( child, "x", Float .toString( light .direction .x ) );
            DomUtils .addAttribute( child, "y", Float .toString( light .direction .y ) );
            DomUtils .addAttribute( child, "z", Float .toString( light .direction .z ) );
            DomUtils .addAttribute( child, "color", light .color .toString() );
            result .appendChild( child );
        }
        return result;
    }

    @Override
    @JsonGetter( "directionalLights" )
    public Iterator<DirectionalLight> iterator()
    {
        return this .directionalLights .iterator();
    }

    public void addDirectionLight( Color color, Vector3f dir )
    {
        this .addDirectionLight( new DirectionalLight( dir, color ) );
    }
}
