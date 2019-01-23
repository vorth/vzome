
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package com.vzome.core.math.symmetry;

import java.util.Iterator;
import java.util.Set;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicMatrix;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.RealVector;

/**
 * @author Scott Vorthmann
 *
 */
public interface Symmetry extends Iterable<Direction>, Embedding
{
	public enum SpecialOrbit {
	    BLUE,    // used for orbit dots
	    RED,     // used for orbit dots and in RotationTool, for the default rotation axis
	    YELLOW,  // used for orbit dots
	    BLACK    // if defined, used for orbit finding in getAxis
    }

    int PLUS = Axis.PLUS, MINUS = Axis.MINUS;
	
	int NO_ROTATION = -1;
    
    String TETRAHEDRAL = "tetrahedral", PYRITOHEDRAL = "pyritohedral";
    
    int getChiralOrder();
    
    String getName();

    Axis getAxis( AlgebraicVector vector );
    
    Axis getAxis( AlgebraicVector vector, OrbitSet orbits );

    Axis getAxis( RealVector vector, Set<Direction> filter );
    
    int getMapping( int from, int to );

    Permutation getPermutation( int i );

    AlgebraicMatrix getMatrix( int i );

	int inverse(int orientation);
	
    String[] getDirectionNames();

    Iterator<Direction> getDirections();
    
	Direction getDirection( String name );
    
    AlgebraicField getField();

    OrbitSet getOrbitSet();

    /**
     * Generate a subgroup, by taking the closure of some collection of Permutations
     * @param perms an array of Permutations indices
     * @return an array of Permutations indices
     */
    int[] closure( int[] perms );
    
    int[] subgroup( String name );
    
    Direction createNewZoneOrbit( String name, int prototype, int rotatedPrototype, AlgebraicVector vector );

    String getDefaultStyle();

    public abstract int[] getIncidentOrientations( int orientation );

    public abstract Direction getSpecialOrbit( SpecialOrbit which );

	Axis getPreferredAxis();

	/**
	 * Get the transformation matrix that maps zone 7 to zone -7 (for example).
	 * If null, the matrix is implicitly a central inversion, negating vectors.
	 * @return {@link AlgebraicMatrix}
	 */
	public abstract AlgebraicMatrix getPrincipalReflection();
	
	/**
	 * Compute the orbit triangle dots for all existing orbits,
	 * and leave behind an OrbitDotLocator for new ones.
	 * The result is just a VEF string, for debugging.
	 * @return
	 */
	public String computeOrbitDots();

    public boolean reverseOrbitTriangle();
}
