/*
 * Created on Jul 22, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
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

import com.vzome.core.math.DomUtils;
import com.vzome.core.render.Color;

/**
 * This is really a SceneModel
 * @author Scott Vorthmann
 *
 */
public class Lights //extends DefaultController
{
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

    protected final List mDirectionalLightColors = new ArrayList(3);

	protected final List mDirectionalLightVectors = new ArrayList(3);
	
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
        for ( Iterator dirs = prototype .mDirectionalLightVectors.iterator(), colors = prototype .mDirectionalLightColors.iterator(); dirs.hasNext(); ) {
            Vector3f pos = (Vector3f) dirs.next();
            Color color = (Color) colors .next();
            addDirectionLight( color, pos );
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
                Vector3f pos = new Vector3f( Float .parseFloat( viewElem .getAttribute( "x" ) ),  Float .parseFloat( viewElem .getAttribute( "y" ) ),  Float .parseFloat( viewElem .getAttribute( "z" ) ));
                addDirectionLight( color, pos );
            }
        }
    }

	
	
	public void addDirectionLight( Color color, Vector3f dir )
	{
		mDirectionalLightColors .add( color );
		mDirectionalLightVectors .add( dir );
	}
	
	
	public void setAmbientColor( Color color )
	{
	    mAmbientLightColor = color;
	}

	public Color getAmbientColor()
	{
		return mAmbientLightColor;
	}
	
	public Color getDirectionalLight( int i, Vector3f direction )
	{
		direction .set( (Vector3f) mDirectionalLightVectors .get( i ) );
		return (Color) mDirectionalLightColors .get( i );
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
        for ( Iterator dirs = mDirectionalLightVectors.iterator(), colors = mDirectionalLightColors.iterator(); dirs.hasNext(); ) {
            Vector3f pos = (Vector3f) dirs.next();
            Color color = (Color) colors .next();
            Element child = doc .createElement( "directionalLight" );
            DomUtils .addAttribute( child, "x", Float .toString( pos .x ) );
            DomUtils .addAttribute( child, "y", Float .toString( pos .y ) );
            DomUtils .addAttribute( child, "z", Float .toString( pos .z ) );
            DomUtils .addAttribute( child, "color", color .toString() );
            result .appendChild( child );
        }
        return result;
    }
}