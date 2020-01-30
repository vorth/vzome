
//(c) Copyright 2011, Scott Vorthmann.

package org.vorthmann.zome.render.java3d;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.PointArray;
import javax.vecmath.Point3d;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.RealVector;
import com.vzome.core.math.symmetry.Embedding;
import com.vzome.core.model.Connector;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.Strut;
import com.vzome.core.render.Colors;
import com.vzome.core.render.RenderedManifestation;

public class Java3dWireframeFactory extends Java3dFactory
{
    public Java3dWireframeFactory( Colors colors, Boolean emissiveHighlights )
    {
        super( colors, emissiveHighlights );
    }

    @Override
    Geometry makeSolidGeometry( RenderedManifestation rm )
    {
        Manifestation man = rm .getManifestation();
        if ( man instanceof Strut )
            return makeLineGeometry( (Strut) man, rm .getShape() .getField(), rm .getEmbedding() );
        else if ( man instanceof Connector )
            return makePointGeometry( (Connector) man, rm .getShape() .getField(), rm .getEmbedding() );
        else
            // panels have polyhedron shapes
            return super .makeSolidGeometry( rm );
    }

    LineArray makeLineGeometry( Strut strut, AlgebraicField field, Embedding embedding )
    {
        LineArray result = new LineArray( 2, GeometryArray.COORDINATES );
        
        Point3d pt = null;
        AlgebraicVector gv = null;
        RealVector v = null;
        
        gv = strut .getLocation();

        v = embedding .embedInR3( gv );
        pt = new Point3d();
        pt.x = v.x; pt.y = v.y; pt.z = v.z;
        result .setCoordinate( 0, pt );
        
        gv = strut .getEnd();

        v = embedding .embedInR3( gv );
        pt = new Point3d();
        pt.x = v.x; pt.y = v.y; pt.z = v.z;
        result .setCoordinate( 1, pt );
        
        return result;
    }

    PointArray makePointGeometry( Connector ball, AlgebraicField field, Embedding embedding )
    {
        PointArray result = new PointArray( 1, GeometryArray.COORDINATES );
        
        Point3d pt = new Point3d();
        AlgebraicVector gv = null;
        RealVector v = null;
        
        gv = ball .getLocation();

        v = embedding .embedInR3( gv );
        pt.x = v.x; pt.y = v.y; pt.z = v.z;
        result .setCoordinate( 0, pt );
        
        return result;
    }

}
