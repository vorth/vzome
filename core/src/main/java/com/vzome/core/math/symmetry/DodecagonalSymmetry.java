
package com.vzome.core.math.symmetry;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicMatrix;
import com.vzome.core.algebra.AlgebraicVector;

/**
 * @author Scott Vorthmann
 *
 */
public class DodecagonalSymmetry extends AbstractSymmetry
{
    private final static int ORDER = 12;
    
	public final Permutation IDENTITY = new Permutation( this, null );

    
    public DodecagonalSymmetry( AlgebraicField field)
    {
        super( ORDER, field, "blue" );
    }
    
    @Override
    protected void createInitialPermutations()
    {
        mOrientations[ 0 ] = IDENTITY;
        // compute the rotation around the Z axis
        int[] map = new int[ ORDER ];
        for ( int i = 0; i < ORDER; i++ )
            map[ i ] = (i+1) % ORDER;
        mOrientations[ 1 ] = new Permutation( this, map );
    }
    
    @Override
    protected void createFrameOrbit( String frameColor )
    {
        AlgebraicVector xAxis = this .mField .createVector( new int[][]{ {1,1,0,1}, {0,1,0,1}, {0,1,0,1} } );
        Direction dir = createZoneOrbit( frameColor, 0, 15, xAxis, true );

        dir .createAxis(  0, NO_ROTATION, xAxis );
        dir .createAxis(  1, NO_ROTATION, new int[][]{ { 0,1, 1,2}, { 1,2, 0,1}, {0,1,0,1} } );
        dir .createAxis(  2, NO_ROTATION, new int[][]{ { 1,2, 0,1}, { 0,1, 1,2}, {0,1,0,1} } );
        dir .createAxis(  3, NO_ROTATION, new int[][]{ { 0,1, 0,1}, { 1,1, 0,1}, {0,1,0,1} } );
        dir .createAxis(  4, NO_ROTATION, new int[][]{ {-1,2, 0,1}, { 0,1, 1,2}, {0,1,0,1} } );
        dir .createAxis(  5, NO_ROTATION, new int[][]{ { 0,1,-1,2}, { 1,2, 0,1}, {0,1,0,1} } );
        dir .createAxis(  6, NO_ROTATION, new int[][]{ {-1,1, 0,1}, { 0,1, 0,1}, {0,1,0,1} } );
        dir .createAxis(  7, NO_ROTATION, new int[][]{ { 0,1,-1,2}, {-1,2, 0,1}, {0,1,0,1} } );
        dir .createAxis(  8, NO_ROTATION, new int[][]{ {-1,2, 0,1}, { 0,1,-1,2}, {0,1,0,1} } );
        dir .createAxis(  9, NO_ROTATION, new int[][]{ { 0,1, 0,1}, {-1,1, 0,1}, {0,1,0,1} } );
        dir .createAxis( 10, NO_ROTATION, new int[][]{ { 1,2, 0,1}, { 0,1,-1,2}, {0,1,0,1} } );
        dir .createAxis( 11, NO_ROTATION, new int[][]{ { 0,1, 1,2}, {-1,2, 0,1}, {0,1,0,1} } );

        AlgebraicVector zAxis = this .mField .createVector( new int[][]{ {0,1,0,1}, {0,1,0,1}, {1,1,0,1} } );
        for ( int p = 0; p < ORDER; p++ ) {
            int x = mOrientations[ p ] .mapIndex( 0 );
            int y = mOrientations[ p ] .mapIndex( 3 );
            mMatrices[ p ] = new AlgebraicMatrix(
                    dir .getAxis( PLUS, x ) .normal(),
                    dir .getAxis( PLUS, y ) .normal(),
                    zAxis );
            
            Axis axis = dir .getAxis( PLUS, p );
            AlgebraicVector norm = mMatrices[ p ] .timesColumn( xAxis );  // I don't know why this has to be left-multiplication
                                         // here, but not for OctahedralSymmetry
            if ( ! norm .equals( axis .normal() ) )
                throw new IllegalStateException( "matrix wrong: " + p );
        }
    }
    
    @Override
    protected void createOtherOrbits()
    {
        createZoneOrbit( "green", 0, NO_ROTATION, new int[][]{ {1,1, 1,2}, {1,2, 0,1}, {0,1, 0,1} }, true );
        createZoneOrbit( "red",   0, 1, new int[][]{ {0,1, 0,1}, {0,1 ,0,1}, {1,1, 0,1} }, true );
    }
    
    @Override
    public Direction getSpecialOrbit( SpecialOrbit which )
    {
        switch ( which ) {

        case BLUE:
            return this .getDirection( "blue" );

        case RED:
            return this .getDirection( "red" );

        case YELLOW:
            return this .getDirection( "green" );

        default:
            return null;
        }
    }

    @Override
    public Axis getPreferredAxis()
    {
        return this .getDirection( "red" ) .getAxis( 0, 0 );
    }

    @Override
    public String getName()
    {
        return "dodecagonal";
    }

    @Override
    public int[] subgroup( String name )
    {
        return null;
    }
}
