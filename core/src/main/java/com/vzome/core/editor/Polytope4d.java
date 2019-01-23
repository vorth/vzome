
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package com.vzome.core.editor;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.commands.AttributeMap;
import com.vzome.core.commands.XmlSaveFormat;
import com.vzome.core.construction.FreePoint;
import com.vzome.core.construction.Point;
import com.vzome.core.construction.Segment;
import com.vzome.core.construction.SegmentJoiningPoints;
import com.vzome.core.math.DomUtils;
import com.vzome.core.math.Projection;
import com.vzome.core.math.QuaternionProjection;
import com.vzome.core.math.symmetry.WythoffConstruction;
import com.vzome.core.model.RealizedModel;

public class Polytope4d extends ChangeManifestations
{
    private int index;
    private final AlgebraicField field;
    private Projection proj;
    private AlgebraicVector quaternion;
    private String groupName;

    // new controls
    private int edgesToRender = 0xF;
    private AlgebraicNumber[] edgeScales = new AlgebraicNumber[4];
    private String renderGroupName;
    private final FieldApplication fieldApp;

    public Polytope4d( Selection selection, RealizedModel realized,
            FieldApplication fieldApp,
            AlgebraicVector quaternion, int index, String groupName,
            int edgesToRender, AlgebraicNumber[] edgeScales,
            String renderGroupName, boolean groupInSelection )
    {
        super( selection, realized, groupInSelection );
        this.fieldApp = fieldApp;
        this.field = fieldApp .getField();

        this.index = index;
        this.quaternion = quaternion;
        this.groupName = groupName;

        this .renderGroupName = renderGroupName;
        this .edgesToRender = edgesToRender;
        if ( edgeScales != null )
            this .edgeScales = edgeScales;
        else
            for (int i = 0; i < this .edgeScales .length; i++)
            {
                this .edgeScales[ i ] = this .field .createPower( 0 );
            }
    }

    public Polytope4d( Selection selection, RealizedModel realized,
            FieldApplication fieldApp,
            Segment symmAxis, int index, String groupName, boolean groupInSelection )
    {
        this( selection, realized, fieldApp,
                ( symmAxis == null )? null : symmAxis .getOffset() .inflateTo4d(),
                        index, groupName, index, null, groupName, groupInSelection );
    }

    @Override
    protected String getXmlElementName()
    {
        return "Polytope4d";
    }

    @Override
    public void getXmlAttributes( Element xml )
    {
        if ( quaternion != null )
            DomUtils .addAttribute( xml, "quaternion", quaternion .toParsableString() );        
        DomUtils .addAttribute( xml, "group", this.groupName );
        DomUtils .addAttribute( xml, "wythoff", Integer .toString( this.index, 2 ) );
        if ( this .edgesToRender != 0xF )
            DomUtils .addAttribute( xml, "renderEdges", Integer .toString( this.edgesToRender, 2 ) );
        if ( ! this .renderGroupName .equals( this .groupName ) )
            DomUtils .addAttribute( xml, "renderGroup", this.renderGroupName );
    }

    @Override
    public void setXmlAttributes( Element xml, XmlSaveFormat format )
    {
        String binary = xml .getAttribute( "wythoff" );
        this .index = Integer .parseInt( binary, 2 );
        String renderString = xml .getAttribute( "renderEdges" );
        this .edgesToRender = ( renderString==null || renderString .isEmpty() )? this .index : Integer .parseInt( renderString, 2 );
        this .groupName = xml .getAttribute( "group" );
        String rgString = xml .getAttribute( "renderGroup" );
        this .renderGroupName = ( rgString==null || rgString .isEmpty() )? this .groupName : rgString;

        String quatString = xml .getAttribute( "quaternion" );
        if ( quatString != null && ! "" .equals( quatString ) ) {
            // newest format
            if ( quatString .contains( "+" ) ) {
                // First, deal with the bug we had in serialization...
                quatString = quatString .replace( ',', ' ' );
                quatString = quatString .replace( '(', ' ' );
                quatString = quatString .replace( ')', ' ' );
                quatString = quatString .replace( '+', ' ' );
                char irrat = this .field .getIrrational( 0 ) .charAt( 0 );
                quatString = quatString .replace( irrat, ' ' );
                quatString = quatString + " 0 0 0";  // This is probably OK, but
                //  it is known to work only for those particular files I have seen,
                //  in which the X, Y, and Z coordinates are all zero.
            }
            this .quaternion = this .field .parseVector( quatString );
        }
        else {
            // legacy formats
            Segment segment = null;
            if ( format .commandEditsCompacted() )
                segment = format .parseSegment( xml, "start", "end" );
            else
            {
                AttributeMap attrs = format .loadCommandAttributes( xml );
                segment = (Segment) attrs .get( "rotation" );
            }
            if ( segment != null )
                this.quaternion = segment .getOffset() .inflateTo4d();
        }
    }


    @Override
    public void perform()
    {
        if ( quaternion == null )
            this.proj = new Projection .Default( field );
        else
            this.proj = new QuaternionProjection( field, null, quaternion .scale( field .createPower( -5 ) ) );
        this .fieldApp .constructPolytope( groupName, this.index, this .edgesToRender, this .edgeScales, new WythoffListener() );
        redo();
    }


    private class WythoffListener implements WythoffConstruction.Listener
    {
        private int numVertices = 0;
        Map<AlgebraicVector, Point> vertices = new HashMap<>();

        @Override
        public Object addEdge( Object p1, Object p2 )
        {
            Segment edge = new SegmentJoiningPoints( (Point) p1, (Point) p2 );
            manifestConstruction( edge );
            return edge;
        }

        @Override
        public Object addFace( Object[] vertices )
        {
            return null;
        }

        @Override
        public Object addVertex( AlgebraicVector vertex )
        {
            Point p = vertices .get( vertex );
            if ( p == null )
            {
                AlgebraicVector projected = vertex;
                if ( proj != null )
                    projected = proj .projectImage( vertex, true );

                projected = projected .scale( field .createPower( 5 ) );

                p = new FreePoint( projected );
                p .setIndex( numVertices++ );
                manifestConstruction( p );
                vertices .put( vertex, p );
            }
            return p;
        }
    }

    public static String[] getSupportedGroups()
    {
        return new String[]{ "A4", "B4/C4", "D4", "F4", "H4" };
    }
}

