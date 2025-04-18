package com.vzome.core.exporters;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Locale;

import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.RealVector;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.Strut;
import com.vzome.core.render.RenderedManifestation;


public class DxfExporter extends GeometryExporter
{
    @Override
	public void doExport( File directory, Writer writer, int height, int width ) throws IOException
	{
        output = new PrintWriter( writer );
        output .println( "0" );
        output .println( "SECTION" );
        output .println( "2" );
        output .println( "ENTITIES" );
		
        NumberFormat format = NumberFormat .getNumberInstance( Locale .US );
        format .setMaximumFractionDigits( 6 );
        double inchScaling = mModel .getCmScaling() / 2.54d;

        for (RenderedManifestation rm : mModel) {
            Manifestation man = rm .getManifestation();
            if ( man instanceof Strut ) {
                output .println( "0" );
                output .println( "LINE" );
                output .println( "8" );
                output .println( "vZome" ); // this is the "layer" the line appears in; it need not be predefined
                AlgebraicVector start = ((Strut) man) .getLocation();
                AlgebraicVector end = ((Strut) man) .getEnd();
                RealVector rv = mModel .renderVector( start );
                rv = rv .scale( inchScaling );
                output .println( "10" );
                output .println( format.format( rv .x ) );
                output .println( "20" );
                output .println( format.format( rv .y ) );
                output .println( "30" );
                output .println( format.format( rv .z ) );
                rv = mModel .renderVector( end );
                rv = rv .scale( inchScaling );
                output .println( "11" );
                output .println( format.format( rv .x ) );
                output .println( "21" );
                output .println( format.format( rv .y ) );
                output .println( "31" );
                output .println( format.format( rv .z ) );
            }
        }
        
        output .println( "0" );
        output .println( "ENDSEC" );
        output .println( "0" );
        output .println( "EOF" );

		output .flush();
	}


    @Override
    public String getFileExtension()
    {
        return "dxf";
    }

}


