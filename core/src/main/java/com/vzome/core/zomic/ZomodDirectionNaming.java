
package com.vzome.core.zomic;

import java.util.HashMap;
import java.util.Map;

import com.vzome.core.math.symmetry.Axis;
import com.vzome.core.math.symmetry.Direction;
import com.vzome.core.math.symmetry.DirectionNaming;
import com.vzome.core.math.symmetry.Symmetry;


class ZomodDirectionNaming extends DirectionNaming
	{
		private final int[] mMapping;
		
		private final Map<Axis, String> mBackMap = new HashMap<>();
		
		ZomodDirectionNaming( Direction dir, int[] mapping )
		{
			super( dir, dir .getName() );
			mMapping = mapping;
			for ( int i = 0; i < mMapping .length; i++ ) {
			    Axis axis = dir .getAxis( Symmetry.PLUS, mMapping[ i ] );
			    mBackMap .put( axis, "+" + i );
			    axis = dir .getAxis( Symmetry.MINUS, mMapping[ i ] );
			    mBackMap .put( axis, "-" + i );

//			    System .out .println();
//			    for ( int sense = Symmetry.PLUS; sense <= Symmetry.MINUS; sense++ ) {
//			        String name = SIGN[sense] + i;
//			        axis = getAxis( name );
//			        System .out .println( dir .getName() + " " + name + " is orientation: " + SIGN[axis .getSense()] + axis .getOrientation() );
//			    }
			}

//			System .out .println();
//			for ( int sense = Symmetry.PLUS; sense <= Symmetry.MINUS; sense++ )
//			    for ( int i = 0; i < dir .getSymmetry() .getChiralOrder(); i++ )
//			        System .out .println( dir .getName() + " " + SIGN[sense] + i + " is called: " + getName( dir .getAxis( sense, i ) ) );
		}
		
        @Override
		public Axis getAxis( String axisName )
		{
		    int sense = getSign( axisName );
		    int index = getInteger( axisName );
			return getDirection() .getAxis( sense, mMapping[ index ] );
		}
		
        @Override
		public String getName( Axis axis )
		{
		    return mBackMap .get( axis );
		}
	}
