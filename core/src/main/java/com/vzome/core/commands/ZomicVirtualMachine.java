
package com.vzome.core.commands;

import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.construction.AnchoredSegment;
import com.vzome.core.construction.Construction;
import com.vzome.core.construction.ConstructionChanges;
import com.vzome.core.construction.Point;
import com.vzome.core.construction.Segment;
import com.vzome.core.construction.SegmentEndPoint;
import com.vzome.core.math.symmetry.Axis;
import com.vzome.core.math.symmetry.Symmetry;
import com.vzome.core.render.AbstractZomicEventHandler;


public class ZomicVirtualMachine extends AbstractZomicEventHandler
{
    private Point mLocation;
    
    private final ConstructionChanges mEffects;

    public AlgebraicVector getLocation()
    {
        return mLocation .getLocation();
    }
    
    /**
     * @return
     */
    public Construction getLastPoint()
    {
        return mLocation;
    }

    @Override
    public void step( Axis axis, AlgebraicNumber length )
    {
        axis = mOrientation .permute( axis, mHandedNess );
        length = length .times( mScale );
        
        Segment segment = new AnchoredSegment( axis, length, mLocation );
        Point pt2 = new SegmentEndPoint( segment );
        
        if ( mAction != JUST_MOVE ) {
            mEffects .constructionAdded( mLocation );
            mEffects .constructionAdded( segment );
            mEffects .constructionAdded( pt2 );
//            mEffects .constructionHidden( segment );
//            mEffects .constructionHidden( pt2 );  // TODO THIS IS A BUG!
        }
        mLocation = pt2;
    }
    
    public ZomicVirtualMachine( Point start, ConstructionChanges effects, Symmetry symm )
    {
        super( symm );
        mLocation = start;
        mEffects = effects;
    }

    @Override
    protected AbstractZomicEventHandler copyLocation()
    {
        return new ZomicVirtualMachine( mLocation, mEffects, mSymmetry );
    }

    @Override
    protected void restoreLocation( AbstractZomicEventHandler changed )
    {
        mLocation = ((ZomicVirtualMachine) changed) .mLocation;
    }
    
}
