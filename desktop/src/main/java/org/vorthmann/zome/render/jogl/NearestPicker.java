package org.vorthmann.zome.render.jogl;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Ray;
import com.jogamp.opengl.math.geom.AABBox;
import com.vzome.core.math.Line;
import com.vzome.core.render.RenderedManifestation;
import com.vzome.core.render.ShapeAndInstances;

class NearestPicker implements ShapeAndInstances.Intersector
{        
    private final Ray ray;
    private final Vec3f rayPoint, rayDirection;

    private RenderedManifestation nearest = null;
    private float nearestZ = Float .MAX_VALUE;
    private final float[] modelViewProjection = new float[16];

    NearestPicker( Line rayLine, float[] modelView, float[] projection )
    {
        super();
        FloatUtil .multMatrix( projection, modelView, this .modelViewProjection );
        this .ray = new Ray();
        rayLine .getOrigin() .toArray( this .ray .orig );
        rayLine .getDirection() .toArray( this .ray .dir );
        this .rayPoint = new Vec3f( ray.orig );
        this .rayDirection = new Vec3f( ray.dir );
        this .rayDirection .normalize();
    }

    // temporary storage, reset and reused during intersections
    private final float[] dpyTmp1V3 = new float[3];
    private final float[] dpyTmp2V3 = new float[3];
    private final float[] dpyTmp3V3 = new float[3];

    @Override
    public void intersectAABBox( float[] min, float[] max, RenderedManifestation rm )
    {
        AABBox sbox = new AABBox( min, max );
        float[] result = new float[3];
        if( sbox.intersectsRay(ray) ) {
            float[] intersection = sbox .getRayIntersection( result, ray, FloatUtil.EPSILON, true, dpyTmp1V3, dpyTmp2V3, dpyTmp3V3 );
            if( null == intersection ) {
                System.out.println( "Failure to getRayIntersection" );
            } else {
                setNearest( intersection[0], intersection[1], intersection[2], rm );
            }
        }
    }
    
    // temporary storage, reset and reused during intersections
    private final float[] verticesIn = new float[16];
    private final float[] verticesOut = new float[16];
    private final float[] vertices3x3 = new float[9];

    @Override
    public void intersectTriangle( float[] verticesArray, int offset, RenderedManifestation rm, float scale, float[] orientation, float[] location )
    {        
        // Incoming verticesArray is an array larger than 9, and the three vertices are stored in order.

        if ( orientation == null ) {
            for ( int i = 0; i < 3; i++ ) {
                for ( int j = 0; j < 3; j++ ) {
                    this .vertices3x3[ 3*i + j ] = verticesArray[ offset + 3*i + j ] * scale + location[ j ];
                }
            }
        }
        else {
            // If we have an orientation, we have to put them into a 4x4 matrix in order to orient them all at once.
            // Then we add the location and put them back into a 9-element array, vertices3x3.

            for ( int i = 0; i < 3; i++ ) { // ith column
                for ( int j = 0; j < 3; j++ ) { // jth row
                    this .verticesIn[ 4*i + j ] = verticesArray[ offset + 3*i + j ];
                }
            }
            FloatUtil .multMatrix( orientation, this .verticesIn, this .verticesOut );
            for ( int i = 0; i < 3; i++ ) {
                for ( int j = 0; j < 3; j++ ) {
                    this .vertices3x3[ 3*i + j ] = this .verticesOut[ 4*i + j ] * scale + location[ j ];
                }
            }
        }
        intersectTriangle( this .vertices3x3, 0, rm, 1f );
    }

    // temporary storage, reset and reused during intersections
    private final Vec3f v0 = new Vec3f();
    private final Vec3f v1 = new Vec3f();
    private final Vec3f v2 = new Vec3f();
    private final Vec3f tuv = new Vec3f();
    private final RayTriangleIntersection rayTriangleIntersection = new RayTriangleIntersection();

    @Override
    public void intersectTriangle( float[] verticesArray, int i, RenderedManifestation rm, float scale )
    {
        // The triangle is represented by 9 floats from the array, starting at i
        this .v0 .set( verticesArray[ i+0 ], verticesArray[ i+1 ], verticesArray[ i+2 ] );
        this .v1 .set( verticesArray[ i+3 ], verticesArray[ i+4 ], verticesArray[ i+5 ] );
        this .v2 .set( verticesArray[ i+6 ], verticesArray[ i+7 ], verticesArray[ i+8 ] );

        // TODO figure out why we need this scale factor!!
        this .v0 .scale( scale );
        this .v1 .scale( scale );
        this .v2 .scale( scale );
        
        boolean hits = this .rayTriangleIntersection .intersectTriangle( rayPoint, rayDirection, v0, v1, v2, tuv );
        if ( hits ) {
            float u = tuv.y();
            float v = tuv.z();
            v1 .scale( u );
            v1 .addScaled( v1, v, v2 );
            v1 .addScaled( v1, 1f - u - v, v0 );
            setNearest( v1.x(), v1.y(), v1.z(), rm );
        }
    }
    
    private void setNearest( float x, float y, float z, RenderedManifestation rm )
    {
        float[] intInWorldCoords = new float[] { x, y, z, 1 };
        float[] intInDeviceCoords = new float[4];
        FloatUtil .multMatrixVec( modelViewProjection, intInWorldCoords, intInDeviceCoords );
        if ( intInDeviceCoords[2] < nearestZ ) {
            nearest = rm;
            nearestZ = intInDeviceCoords[2];
        }
    }
    
    RenderedManifestation getNearest()
    {
        return this .nearest;
    }
}