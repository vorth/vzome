package com.vzome.core.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicMatrix;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.Polyhedron;
import com.vzome.core.math.RealVector;
import com.vzome.core.math.symmetry.Symmetry;
import com.vzome.core.render.RenderedModel.OrbitSource;
import com.vzome.opengl.Scene;
import com.vzome.opengl.ShapeClass;

public class OpenGlSceneLoader
{
	private static final float MODEL_SCALE_FACTOR = 0.05f;


    public static class Config
    {
        public Set<RenderedManifestation> instances = new HashSet();
        public float[] color;
    }

	public static Scene getOpenGlScene( RenderedModel rmodel, Colors colors )
    {
    	// wireframe of struts, just for quick rendering feedback
//    	float[] black = new float[] { 0f, 0f, 0f, 1f };
//    	Strut[] parts = doc .getStruts() .toArray( new Strut[0] );
//        float[] verticesArray = new float[ShapeClass.COORDS_PER_VERTEX * 2 * parts.length];
//        for (int i = 0; i < parts.length; i++) {
//            Vector vector = parts[i] .location();
//            verticesArray[i * 2 * ShapeClass.COORDS_PER_VERTEX + 0] = (float) vector.getX().value()/2;  // TODO: figure out why we need "/2"
//            verticesArray[i * 2 * ShapeClass.COORDS_PER_VERTEX + 1] = (float) vector.getY().value()/2;
//            verticesArray[i * 2 * ShapeClass.COORDS_PER_VERTEX + 2] = (float) vector.getZ().value()/2;
//            Vector offset = parts[i] .offset();
//            vector = vector .plus( offset );
//            verticesArray[i * 2 * ShapeClass.COORDS_PER_VERTEX + 3] = (float) vector.getX().value()/2;
//            verticesArray[i * 2 * ShapeClass.COORDS_PER_VERTEX + 4] = (float) vector.getY().value()/2;
//            verticesArray[i * 2 * ShapeClass.COORDS_PER_VERTEX + 5] = (float) vector.getZ().value()/2;
//        }
//        this.struts = new ShapeClass( verticesArray, null, null, black );
    
    	Map<Polyhedron,Config> shapeClassConfigs = new HashMap<Polyhedron,Config>();
    	Set<ShapeClass> shapes = new HashSet<ShapeClass>();
    	Iterator rms = rmodel .getRenderedManifestations();
    	while ( rms .hasNext() ) {
    		RenderedManifestation rman = (RenderedManifestation) rms .next();

    		Polyhedron shape = rman .getShape();
    		Config scc = shapeClassConfigs .get( shape );
    		if ( scc == null ) {
    			scc = new Config();
    			shapeClassConfigs .put( shape, scc );
    			Color color = colors .getColor( rman .getColorName() );
    			float[] rgb = new float[3];
    			color .getRGBColorComponents( rgb );
    			scc .color = new float[]{ rgb[0], rgb[1], rgb[2], 1f };
    		}
    		scc .instances .add( rman );
    	}
    	for( Map.Entry<Polyhedron, Config> entry : shapeClassConfigs.entrySet() )
    	{
    		Config config = entry .getValue();
            ShapeClass shapeClass = create( entry .getKey(), config.instances, config.color );
            shapes .add( shapeClass );
    	}
    	return new Scene( shapes, getOrientations( rmodel .getOrbitSource() ) );
    }
    
    private static ShapeClass create( Polyhedron shape, Set<RenderedManifestation> parts, float[] color )
    {
        List vertices = new ArrayList();
        List normals = new ArrayList();
        List vertexList = shape.getVertexList();
        Set<Polyhedron.Face> faces = shape.getFaceSet();
        for (Polyhedron.Face face : faces) {
            AlgebraicVector normal = face.getNormal();
            RealVector rn = normal.negate().toRealVector().normalize();
            int count = face.size();
            AlgebraicVector vertex = (AlgebraicVector) vertexList.get(face.getVertex(0));
            RealVector rv0 = vertex.toRealVector();
            vertex = (AlgebraicVector) vertexList.get(face.getVertex(1));
            RealVector rvPrev = vertex.toRealVector();
            for (int i = 2; i < count; i++) {
                vertex = (AlgebraicVector) vertexList.get(face.getVertex(i));
                RealVector rv = vertex.toRealVector();
                vertices.add(rv0);
                normals.add(rn);
                vertices.add(rvPrev);
                normals.add(rn);
                vertices.add(rv);
                normals.add(rn);
                rvPrev = rv;
            }
        }

        float[] verticesArray = new float[ShapeClass.COORDS_PER_VERTEX * vertices.size()];
        float[] normalsArray = new float[ShapeClass.COORDS_PER_VERTEX * normals.size()];  // same size!
        for (int i = 0; i < vertices.size(); i++) {
            RealVector vector = (RealVector) vertices.get(i);
            vector = vector .scale( MODEL_SCALE_FACTOR );
            verticesArray[i * ShapeClass.COORDS_PER_VERTEX] = (float) vector.x;
            verticesArray[i * ShapeClass.COORDS_PER_VERTEX + 1] = (float) vector.y;
            verticesArray[i * ShapeClass.COORDS_PER_VERTEX + 2] = (float) vector.z;
            vector = (RealVector) normals.get(i);
            normalsArray[i * ShapeClass.COORDS_PER_VERTEX] = (float) vector.x;
            normalsArray[i * ShapeClass.COORDS_PER_VERTEX + 1] = (float) vector.y;
            normalsArray[i * ShapeClass.COORDS_PER_VERTEX + 2] = (float) vector.z;
        }

        float[] offsets = new float[4 * parts.size()];
        int i = 0;
        for( RenderedManifestation part : parts ) {
            AlgebraicVector vector = part .getManifestation() .getLocation();
            int zone = part .getStrutZone();
            offsets[i * 4 + 0] = MODEL_SCALE_FACTOR * (float) vector .getComponent( AlgebraicVector.X ) .evaluate();
            offsets[i * 4 + 1] = MODEL_SCALE_FACTOR * (float) vector .getComponent( AlgebraicVector.Y ) .evaluate();
            offsets[i * 4 + 2] = MODEL_SCALE_FACTOR * (float) vector .getComponent( AlgebraicVector.Z ) .evaluate();
            offsets[i * 4 + 3] = (float) zone;
            ++i;
        }
        return new ShapeClass( verticesArray, normalsArray, offsets, color );
    }

	private static float[][] getOrientations( OrbitSource orbits )
	{
		Symmetry symmetry = orbits .getSymmetry();
		AlgebraicField field = symmetry .getField();
		int order = symmetry .getChiralOrder();
		float[][] result = new float[order][];
		for ( int orientation = 0; orientation < order; orientation++ )
		{
			float[] asFloats = new float[ 16 ];
			AlgebraicMatrix transform = symmetry .getMatrix( orientation );
	        for ( int i = 0; i < 3; i++ )
	        {
	            AlgebraicVector columnSelect = field .basisVector( 3, i );
	            AlgebraicVector columnI = transform .timesColumn( columnSelect );
	            RealVector colRV = columnI .toRealVector();
	            asFloats[ i*4+0 ] = (float) colRV.x;
	            asFloats[ i*4+1 ] = (float) colRV.y;
	            asFloats[ i*4+2 ] = (float) colRV.z;
	            asFloats[ i*4+3 ] = 0f;
	        }
	        asFloats[ 12 ] = 0f;
	        asFloats[ 13 ] = 0f;
	        asFloats[ 14 ] = 0f;
	        asFloats[ 15 ] = 1f;
	        result[ orientation ] = asFloats;
		}
		return result;
	}

}
