package com.vzome.core.math.symmetry;

import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;

public class GeometryDerivations
{
    /**
     * from http://astronomy.swin.edu.au/~pbourke/geometry/planeline/:
     *
     *
     The equation of a plane (points P are on the plane with normal N and point P3 on the plane) can be written as

    N dot (P - P3) = 0

     The equation of the line (points P on the line passing through points P1 and P2) can be written as

    P = P1 + u (P2 - P1)

     The intersection of these two occurs when

    N dot (P1 + u (P2 - P1)) = N dot P3

     Solving for u gives
     
          u = ( N dot (P3-P1) ) / ( N dot (P2-P1) )
          
     If the denominator is zero, the line is parallel to the plane.
     
     * @author Scott Vorthmann
     */
	public static AlgebraicVector linePlaneIntersection( AlgebraicVector lineStart, AlgebraicVector lineDir, AlgebraicVector planePoint, AlgebraicVector planeNormal )
	{        
        AlgebraicNumber denom = planeNormal .dot( lineDir );
        if ( denom .isZero() )
            return null;

        AlgebraicVector p1p3 = planePoint .minus( lineStart );
        AlgebraicNumber numerator = planeNormal .dot( p1p3 );
        AlgebraicNumber u = numerator .dividedBy( denom );
        AlgebraicVector p = lineStart .plus( lineDir .scale( u ) );
        return p;
	}
}
