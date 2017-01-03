

package com.vzome.core.math.symmetry;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicMatrix;
import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.RealVector;

/**
 * @author Scott Vorthmann
 */
public class Direction implements Comparable<Direction>, Iterable<Axis>
{
    private String mName;
    
    private final Axis[][] mAxes;
    
    private final Map<AlgebraicVector, Axis> mVectors = new HashMap<>();
    
    private final Symmetry mSymmetryGroup;
    
    private final AlgebraicVector mPrototype;
    
    private boolean mStandard, mAutomatic, hasHalfSizes;
    
    private final String[] scaleNames = new String[]{ "shorter", "short", "medium", "long" };

    public final AlgebraicNumber[] scales = new AlgebraicNumber[ scaleNames.length ];

    private AlgebraicNumber unitLength, unitLengthReciprocal;
    
    private double dotX = -999d, dotY = -999d;

    public final void setAutomatic( boolean auto )
    {
        mAutomatic = auto;
    }
    
    public boolean isAutomatic()
    {
        return mAutomatic;
    }
    
    public boolean isStandard()
    {
        return mStandard;
    }
    
    private static int globalIndex = 0;
    
    private final int index;

    private int canonicalize = 0;
    
    public Direction( String name, Symmetry group, int prototype, int rotatedPrototype, AlgebraicVector vector, boolean isStd )
    {
        this.index = globalIndex++; // we want to just retain the order used to create these
        mStandard = isStd;
        mName = name;
        mSymmetryGroup = group;
        for ( int i = 0; i < scales .length; i++ ) {
            scales[ i ] = mSymmetryGroup .getField() .createPower( i - 1 );
        }
        mPrototype = vector;
        mAxes = new Axis[ 2 ][ group .getChiralOrder() ];
        for ( int i = 0; i < mAxes[ Symmetry.PLUS ] .length; i++ ) {
            AlgebraicMatrix transform = group .getMatrix( i );
            if ( transform == null )
                return; // only happens for the "base" direction, like "blue" for icosa
            Permutation perm = group .getPermutation( i );
            int j = perm .mapIndex( prototype );
            int rotated = perm .mapIndex( rotatedPrototype );
            AlgebraicVector normal = transform .timesColumn( vector );

//            if ( ! Arrays.equals( field .dot( normal, normal ), field .dot( vector, vector ) ) )
//                throw new IllegalStateException( "rotated normal has bad length" );

            int rot = group .getMapping( j, rotated );
            createAxis( j, rot, normal );
        }
    }
    
    @Override
    public String toString()
    {
        return mSymmetryGroup .getName() + " " + this .mName;
    }

    /**
     * Create a new automatic direction.
     * @param name
     * @param group
     * @param orientation
     * @param rotation
     * @param vector
     */
    public Direction( String name, Symmetry group, int orientation, int rotation, AlgebraicVector vector )
    {
        this( name, group, orientation, rotation, vector, false );
        this .setAutomatic( true );
    }
    
    public AlgebraicVector getPrototype()
    {
        return mPrototype;
    }
    
    @Override
    public Iterator<Axis> iterator()
    {
        return mVectors .values() .iterator();
    }
    
    /**
    * @deprecated Consider using a JDK-5 for-loop if possible. Otherwise use {@link #iterator()} instead.
    */
    @Deprecated
    public Iterator<Axis> getAxes()
    {
        return this .iterator();
    }
        
    public Symmetry getSymmetry()
    {
        return mSymmetryGroup;
    }
    
        public String getName()
    {
        return mName;
    }
    
    public Axis getAxis( AlgebraicVector vector )
    {
        for (Axis axis : mVectors .values()) {
            AlgebraicVector normal = axis .normal();
            if ( normal .cross( vector ) .isOrigin() ) {
                // parallel
                AlgebraicNumber dotProd = normal .dot( vector );
                if ( dotProd .evaluate() > 0 )
                    return axis;
                else {
                    int opp = ( axis .getSense() + 1 ) % 2;
                    return getAxis( opp, axis .getOrientation() );
                }
            }
        }
        return null;
    }
    
    public Axis getAxis( RealVector vector )
    {
        return this .getSymmetry() .getAxis( vector, Collections .singleton( this ) );
    }
    
    Axis getChiralAxis( RealVector vector )
    {
        if ( RealVector .ORIGIN .equals( vector ) ) {
            return null;
        }
        // largest cosine means smallest angle
        //  and cosine is (a . b ) / |a| |b|
        double vectorLength = vector .length();
        Set<Axis> checked = new HashSet<>();
        int closestOrientation = 0;
        int closestSense = Symmetry.PLUS;
        Axis closestAxis = this .getCanonicalAxis( Symmetry.PLUS, 0 );
        checked .add( closestAxis );
        RealVector axisV = closestAxis .normal() .toRealVector(); // TODO invert the Embedding to get this right
        double maxCosine = vector .dot( axisV ) / (vectorLength * axisV .length());
        if ( maxCosine < 0 ) {
            // wrong hemisphere, flip to the other one
            closestAxis = this .getCanonicalAxis( Symmetry.MINUS, 0 );
            closestSense = Symmetry.MINUS;
            checked .add( closestAxis );
            axisV = closestAxis .normal() .toRealVector();
            maxCosine = vector .dot( axisV ) / (vectorLength * axisV .length());
        }
        boolean finished = false;
        while ( !finished ) {
            int[] incidentOrientations = this .getSymmetry() .getIncidentOrientations( closestOrientation );
            if ( incidentOrientations == null ) {
                // this symmetry group has not implemented getIncidentOrientations, so we'll fall through to brute force
                break;
            }
            int reverseSense = (closestSense+1)%2;
            for ( int i : incidentOrientations ) {
                Axis neighbor = this .getCanonicalAxis( reverseSense, i );
                if ( checked .contains( neighbor ) )
                    continue;
                checked .add( neighbor );
                axisV = neighbor .normal() .toRealVector();
                double cosine = vector .dot( axisV ) / (vectorLength * axisV .length());
                if ( cosine > maxCosine ) {
                    maxCosine = cosine;
                    closestAxis = neighbor;
                    closestOrientation = i;
                    closestSense = reverseSense;  // this assignment prevents the "success" return below
                }
            }
            if ( reverseSense != closestSense ) {
                // didn't flip the sense, which means we didn't find a better cosine,
                //   so we're done, maxed out.
                return closestAxis;
            }
        }
        // Fall back to brute force search
        return this .getAxisBruteForce( vector );
    }
    
    public Axis getAxisBruteForce( RealVector vector )
    {        
        Axis closestAxis = null;
        double maxCosine = -1d;
        for ( Iterator<Axis> axes = this .getAxes(); axes .hasNext(); ) {
            Axis axis = axes .next();
            RealVector axisV = axis .normal() .toRealVector(); // TODO invert the Embedding to get this right
            double cosine = vector .dot( axisV ) / (vector .length() * axisV .length());
            if ( cosine > maxCosine ) {
                maxCosine = cosine;
                closestAxis = axis;
            }
        }
        return closestAxis;
    }
    
    public Axis getAxis( int sense, int index )
    {
        return mAxes[ sense ][ index ];
    }
    
    public Direction withCorrection()
    {
        Axis treatedAs0 = this .getAxisBruteForce( RealVector.DIRECTION_0 );
        this .canonicalize = treatedAs0 .getOrientation();
        if ( treatedAs0 .getSense() == Symmetry.MINUS )
            this .canonicalize *= -1;
        return this;
    }
    
    /**
     * Get the axis that protrudes from the canonical direction on the zome ball.
     * Many Directions (orbits) are created without regard to whether "axis 0" actually sticks out
     * of the ball in the fundamental domain with index 0.
     * @param sense
     * @param index
     * @return
     */
    public Axis getCanonicalAxis( int sense, int index )
    {
        if ( this.canonicalize != 0 )
        {
            if ( this .canonicalize < 0 )
                sense = ( sense + 1 ) % 2;
            Permutation target = this .mSymmetryGroup .getPermutation( index );
            index = target .mapIndex( Math.abs( this .canonicalize ) );
        }
        return this .getAxis( sense, index );
    }

    public void createAxis( int orientation, int rotation, int[] norm )
    {
        AlgebraicVector aNorm = this .mSymmetryGroup .getField() .createVector( norm );
        this .createAxis( orientation, rotation, aNorm );
    }
    /**
     * @param orientation the index of this axis (zone) in its Direction (orbit)
     * @param rotation the index of the permutation that is a rotation around this axis, or NO_ROTATION
     */
    public final void createAxis( int orientation, int rotation, AlgebraicVector norm )
    {
        AlgebraicVector key = norm;
        Axis axis = mVectors .get( key );
        Permutation perm = mSymmetryGroup .getPermutation( rotation );
        if ( axis == null ) {
            axis = new Axis( this, orientation, Symmetry.PLUS, rotation, perm, norm );
            mVectors .put( key, axis );
        } else if ( axis .getSense() == Symmetry .MINUS )
            axis .rename( Symmetry.PLUS, orientation );
        mAxes[ Symmetry.PLUS ][ orientation ] = axis;
        norm = norm .negate();
        if ( perm != null )
            perm = perm .inverse();
        key = norm;
        axis = mVectors .get( key );
        if ( axis == null ) {
        	// This is a bug! We should have adjusted rotation to be perm .mapIndex( 0 );
            axis = new Axis( this, orientation, Symmetry.MINUS, rotation, perm, norm );
            mVectors .put( key, axis );
        }
        mAxes[ Symmetry.MINUS ][ orientation ] = axis;
    }

    @Override
    public int compareTo( Direction other )
    {
        return this.index - other.index;
    }
    
    public void setHalfSizes( boolean value )
    {
        hasHalfSizes = value;
    }

    public boolean hasHalfSizes()
    {
        return hasHalfSizes;
    }
    
    public void setScaleNames( String[] names )
    {
        for (int i = 0; i < names.length; i++)
        {
            scaleNames[ i ] = names[ i ];
        }
    }
    
    public String getScaleName( int scale )
    {
        if ( scale < scaleNames.length && scale >= 0 )
            return scaleNames[ scale ];
        else
            return "scale " + (scale-1);
    }
    
    public void setUnitLength( AlgebraicNumber unitLength )
    {
        this .unitLength = unitLength;
        this .unitLengthReciprocal = unitLength .reciprocal(); // do the matrix inversion just once
    }

    public AlgebraicNumber getUnitLength()
    {
        if ( unitLength == null)
            return mSymmetryGroup .getField() .createPower( 0 );
        else
            return unitLength;
    }
    
    public static final int USER_SCALE = 3;
    
    public AlgebraicNumber getLengthInUnits( AlgebraicNumber rawLength )
    {
        // reproduce the calculation in LengthModel .setActualLength()
        // TODO rationalize that
        AlgebraicField field = mSymmetryGroup .getField();
        AlgebraicNumber scaledLength = rawLength .times( field .createPower( - USER_SCALE ) );
        if ( unitLength == null)
            return scaledLength;
        else
            return scaledLength .times( unitLengthReciprocal );
    }

    public String getLengthName( AlgebraicNumber length )
    {
        for ( int i = 0; i < scales .length; i++ ) {
            if ( scales[ i ] .equals( length ) ) {
                return scaleNames[ i ];
            }
        }
        return "";
    }

    public void getLengthExpression( StringBuffer buf, AlgebraicNumber length )
    {
        int bufLen = buf.length();
        buf .append(getLengthName( length ) );
        if(buf.length() == bufLen) {
            // Be sure to append something before the colon delimiter
            // so the StringTokenizer that uses buf doesn't choke on a missing token.
            buf .append( " " );
        }
        buf .append( ":" );
        length .getNumberExpression( buf, AlgebraicField .EXPRESSION_FORMAT );
    }

	public double getDotX()
	{
		return this .dotX;
	}

	public double getDotY()
	{
		return this .dotY;
	}
	
	public void setDotLocation( double x, double y )
	{
		this .dotX = x;
		this .dotY = y;
	}
}
