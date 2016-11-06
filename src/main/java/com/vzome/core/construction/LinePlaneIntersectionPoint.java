

package com.vzome.core.construction;

import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.algebra.Bivector3dHomogeneous;
import com.vzome.core.algebra.Trivector3dHomogeneous;
import com.vzome.core.algebra.Vector3dHomogeneous;
import com.vzome.core.math.symmetry.GeometryDerivations;


public class LinePlaneIntersectionPoint extends Point
{
    // parameters
    private final Plane mPlane;
    private final Line mLine;

    public LinePlaneIntersectionPoint( Plane plane, Line line )
    {
        super( line .field );
        mPlane = plane;
        mLine = line;
        mapParamsToState();
    }

    /**
     * From Vince, GA4CG, p. 196.
     * 
     * @author Scott Vorthmann
     */
    protected boolean mapParamsToState_usingGA()
    {
        if ( mPlane .isImpossible() || mLine .isImpossible() )
            return setStateVariable( null, true );
                
        Trivector3dHomogeneous plane = mPlane .getHomogeneous();
        Bivector3dHomogeneous line = mLine .getHomogeneous();
        
        Vector3dHomogeneous intersection = plane .dual() .dot( line );
        // TODO find out why this does not work as well as the original below
        if ( ! intersection .exists() )
            return setStateVariable( null, true );
        
        return setStateVariable( intersection .getVector(), false );
    }
    
    @Override
    protected final boolean mapParamsToState()
    {
        if ( mPlane .isImpossible() || mLine .isImpossible() )
            return setStateVariable( null, true );

        AlgebraicVector p = GeometryDerivations .linePlaneIntersection( mLine .getStart(), mLine .getDirection(), mPlane .getBase(), mPlane .getNormal() );
        if ( p == null )
            return setStateVariable( null, true );
        else
        	return setStateVariable( p, false );
    }

}
