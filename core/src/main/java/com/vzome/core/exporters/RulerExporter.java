
package com.vzome.core.exporters;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;

import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.RealVector;
import com.vzome.core.model.Connector;
import com.vzome.core.model.Manifestation;
import com.vzome.core.render.Colors;
import com.vzome.core.render.RenderedManifestation;
import com.vzome.core.render.RenderedModel;
import com.vzome.core.viewing.SceneLighting;
import com.vzome.core.viewing.Camera;

public class RulerExporter extends Exporter3d
{
    public RulerExporter( Camera scene, Colors colors,
            SceneLighting lights, RenderedModel model )
    {
        super( scene, colors, lights, model );
    }

    @Override
    public void doExport( File directory, Writer writer, int height, int width )
            throws Exception
    {
        double maxX = 0, maxY = 0, maxZ = 0;

        for (RenderedManifestation rm : this .mModel) {
            Manifestation man = rm .getManifestation();
            if ( man instanceof Connector )
            {
                AlgebraicVector loc = ((Connector) man) .getLocation();
                RealVector rv = mModel .renderVector( loc );
                double x = rv .x;
                if ( x > maxX )
                    maxX = x;
                double y = rv .y;
                if ( y > maxY )
                    maxY = y;
                double z = rv .z;
                if ( z > maxZ )
                    maxZ = z;
            }
        }

        PrintWriter pw = new PrintWriter( writer );
        pw .println( "max X = " + maxX );
        pw .println( "max Y = " + maxY );
        pw .println( "max Z = " + maxZ );
    }

    @Override
    public String getFileExtension()
    {
        return "txt";
    }

}
