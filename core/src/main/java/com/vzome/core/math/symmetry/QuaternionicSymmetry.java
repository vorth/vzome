
package com.vzome.core.math.symmetry;

import java.util.StringTokenizer;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.algebra.Quaternion;
import com.vzome.core.math.VefParser;
import com.vzome.xml.ResourceLoader;

/**
 * @author Scott Vorthmann
 *
 */
public class QuaternionicSymmetry
{    
//    private static final IntegralNumber ONE = GoldenNumber.ONE, SIGMA = GoldenNumber.TAU_INV.neg(),
//                            TAU = GoldenNumber.TAU, ZERO = GoldenNumber.ZERO, HALF = GoldenNumber.HALF;
    
//    public static final AlgebraicVector I_GENERATOR = new GoldenNumberVector( ONE, SIGMA, TAU, ZERO, HALF );
//    
//    public static final AlgebraicVector T_GENERATOR = new GoldenNumberVector( ONE, ZERO, ZERO, ZERO );
    
//    public static final AlgebraicVector OMEGA = new GoldenNumberVector( ONE, ONE, ONE, ONE.neg(), HALF );
    
//    public static final QuaternionicSymmetry IxT = new QuaternionicSymmetry( 
//                                                         new AlgebraicVector[]{ I_GENERATOR, OMEGA }, 
//                                                         new AlgebraicVector[]{ T_GENERATOR, OMEGA } );
    
    private Quaternion[] mRoots;
    
    private final String mName;
    
    public QuaternionicSymmetry( String name, String rootsResource, AlgebraicField field )
    {
        mName = name;
        String vefData = ResourceLoader.loadStringResource( rootsResource );
        RootParser parser = new RootParser( field );
        parser .parseVEF( vefData, field );
        mRoots = parser .getQuaternions();
    }

    public Quaternion[] getRoots()
    {
        return mRoots;
    }
    
    private final static class RootParser extends VefParser
    {
        private Quaternion[] mRoots;

        private final AlgebraicField field;
        
        RootParser( AlgebraicField field )
        {
            this.field = field;
            HALF = field .createRational( 1, 2 );
        }

        @Override
        protected void startVertices( int numVertices )
        {
            mRoots = new Quaternion[ numVertices ];
        }

        public Quaternion[] getQuaternions()
        {
            return mRoots;
        }

        private final AlgebraicNumber HALF;
        
        @Override
        protected void addVertex( int index, AlgebraicVector location )
        {
            mRoots[ index ] = new Quaternion( field, location .scale( HALF ) );
        }

        @Override
        protected void startEdges( int numEdges )
        {}

        @Override
        protected void addEdge( int index, int v1, int v2 )
        {}

        @Override
        protected void startFaces( int numFaces )
        {}

        @Override
        protected void addFace( int index, int[] verts )
        {}

        @Override
        protected void addBall( int index, int vertex )
        {}

        @Override
        protected void startBalls( int numVertices )
        {}

        @Override
        protected void endFile( StringTokenizer tokens )
        {}
        
    }

    public String getName()
    {
        return mName;
    }



    
}
