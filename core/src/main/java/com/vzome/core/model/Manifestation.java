

package com.vzome.core.model;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.construction.Construction;
import com.vzome.core.render.RenderedManifestation;

/**
 * @author Scott Vorthmann
 */
public abstract class Manifestation implements GroupElement
{
    protected final Set<Construction> mManifests = new HashSet<>(5);

    protected RenderedManifestation mRendered = null;

    private boolean hidden = false;

    private int mId = NO_ID;

    private Color color;

    private static final int NO_ID = -1;

    private static int NEXT_ID = 0;

    void resetId()
    {
        NEXT_ID = 0;
        mId = NO_ID;
    }

    int getId()
    {
        if ( mId == NO_ID )
            mId = NEXT_ID++;
        return mId;
    }

    public void addConstruction( Construction c )
    {
        mManifests .add( c );
    }

    public void removeConstruction( Construction c )
    {
        mManifests .remove( c );
    }

    public Iterator<Construction> getConstructions()
    {
        return mManifests .iterator();
    }

    /**
     * This is different from toConstruction, because we must support
     * the legacy behavior, which used the iterator.
     * @return
     */
    public Construction getFirstConstruction()
    {
        return mManifests .iterator() .next();
    }

    public boolean isUnnecessary()
    {
        return mManifests .isEmpty();
    }
    
    public Color getColor()
    {
        if(this.color == null && mRendered != null) {
            // TODO: The underlying color should really be a property 
            // of either Manifestation or RenderedManifestation, not both, 
            // although both classes could have getter and setter methods
            // acting on which ever class contains the actual color value.
            // In the mean time, this hack improves the situation.
            this.color = mRendered.getColor(); 
        }
        return this.color;
    }

    public void setColor( Color color )
    {
        this.color = color;
    }

    public void setRenderedObject( RenderedManifestation obj )
    {
        mRendered = obj;
        if ( obj != null )
            this .color = obj .getColor();
    }

    public RenderedManifestation getRenderedObject()
    {
        return mRendered;
    }

    public boolean isHidden()
    {
        return this .hidden;
    }

    public abstract AlgebraicVector getLocation();

    public abstract AlgebraicVector getCentroid();

    /**
     * This is guaranteed to return a 3D construction,
     * and will return the same object as getFirstConstruction()
     * when possible.
     * @return
     */
    public abstract Construction toConstruction();

    private Group mContainer;

    public Group getContainer()
    {
        return mContainer;
    }

    @Override
    public void setContainer( Group container )
    {
        mContainer = container;
    }

    public void setHidden( boolean hidden )
    {
        this.hidden = hidden;
    }

    public boolean isRendered()
    {
        return mRendered != null;
    }

    public Element getXml( Document doc )
    {
        return mManifests .isEmpty()
                ? doc .createElement( "NoConstructions" )
                        : mManifests .iterator() .next() .getXml( doc );
    }
}
