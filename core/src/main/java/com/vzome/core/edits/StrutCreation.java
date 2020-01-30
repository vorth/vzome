
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package com.vzome.core.edits;


import java.util.Map;

import org.w3c.dom.Element;

import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.commands.AttributeMap;
import com.vzome.core.commands.XmlSaveFormat;
import com.vzome.core.construction.AnchoredSegment;
import com.vzome.core.construction.Point;
import com.vzome.core.construction.Segment;
import com.vzome.core.construction.SegmentEndPoint;
import com.vzome.core.editor.ChangeManifestations;
import com.vzome.core.editor.EditorModel;
import com.vzome.core.math.symmetry.Axis;
import com.vzome.core.model.RealizedModel;

public class StrutCreation extends ChangeManifestations
{
    private Point mAnchor;
    private Axis mAxis;
    private AlgebraicNumber mLength;
    
    public StrutCreation( EditorModel editor )
    {
        this( null, null, null, editor .getRealizedModel() );
    }
    
    @Override
    public void configure( Map<String, Object> params )
    {
        this.mAnchor = (Point) params .get( "anchor" );
        this.mAxis = (Axis) params .get( "zone" );
        this.mLength = (AlgebraicNumber) params .get( "length" );
    }

    public StrutCreation( Point anchor, Axis axis, AlgebraicNumber len, RealizedModel realized )
    {
        super( null, realized );
        
        mAnchor = anchor;
        mAxis = axis;
        mLength = len;
    }
    
    @Override
    public void perform()
    {
        Segment segment = new AnchoredSegment( mAxis, mLength, mAnchor );
        manifestConstruction( segment );
        Point point = new SegmentEndPoint( segment );
        manifestConstruction( point );

        redo();
    }

    @Override
    protected void getXmlAttributes( Element xml )
    {
        XmlSaveFormat .serializePoint( xml, "anchor", mAnchor );
        XmlSaveFormat .serializeAxis( xml, "symm", "dir", "index", "sense", mAxis );
        XmlSaveFormat .serializeNumber( xml, "len", mLength );
    }

    @Override
    public void setXmlAttributes( Element xml, XmlSaveFormat format )
    {
        if ( format .rationalVectors() )
        {
            mAnchor = format .parsePoint( xml, "anchor" );
            mAxis = format .parseAxis( xml, "symm", "dir", "index", "sense" );
            mLength = format .parseNumber( xml, "len" );
        }
        else
        {
            AttributeMap attrs = format .loadCommandAttributes( xml, /*projectTo3d*/ true );
            mAnchor = (Point) attrs .get( "anchor" );
            mAxis = (Axis) attrs .get( "axis" );
            mLength = (AlgebraicNumber) attrs .get( "len" );
        }
    }

    @Override
    protected String getXmlElementName()
    {
        return "StrutCreation";
    }
}
