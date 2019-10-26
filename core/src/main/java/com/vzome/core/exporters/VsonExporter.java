package com.vzome.core.exporters;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.math.symmetry.Axis;
import com.vzome.core.model.Connector;
import com.vzome.core.model.IndexedModel;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.Panel;
import com.vzome.core.model.Strut;
import com.vzome.core.render.Color;
import com.vzome.core.render.Colors;
import com.vzome.core.render.RenderedManifestation;
import com.vzome.core.render.RenderedModel;
import com.vzome.core.viewing.Camera;
import com.vzome.core.viewing.Lights;


public class VsonExporter extends Exporter3d
{			
    public VsonExporter( Camera scene, Colors colors, Lights lights, RenderedModel model )
    {
        super( scene, colors, lights, model );
    }

    @Override
    public void doExport( File directory, Writer writer, int height, int width ) throws IOException
    {
        SortedSet<AlgebraicVector> vertices = new TreeSet<>();
        ArrayList<Integer> ballIndices = new ArrayList<>();
        ArrayList<IndexedModel.Strut> struts = new ArrayList<>();
        ArrayList<IndexedModel.Panel> panels = new ArrayList<>();

        // phase one: find and index all vertices
        for (RenderedManifestation rm : mModel) {
            Manifestation man = rm .getManifestation();
            if ( man instanceof Connector )
            {
                vertices .add( man .getLocation() );
            }
            else if ( man instanceof Strut )
            {
                vertices .add( man .getLocation() );
                vertices .add( ((Strut) man) .getEnd() );
            }
            else if ( man instanceof Panel )
            {
                for ( AlgebraicVector vertex : (Panel) man ) {
                    vertices .add( vertex );
                }
            }
        }

        // Up to this point, the vertices TreeSet has collected and sorted every unique vertex of every manifestation.
        // From now on we'll need their index, so we copy them into an ArrayList, preserving their sorted order.
        // so we can get their index into that array.
        ArrayList<AlgebraicVector> sortedVertexList = new ArrayList<>(vertices);
        // we no longer need the vertices collection, 
        // so set it to null to free the memory and to ensure we don't use it later by mistake.
        vertices = null;

        // phase three: generate the JSON
        IndexedModel indexedModel = new IndexedModel();
        ObjectMapper mapper = new ObjectMapper();
        for (RenderedManifestation rm : mModel) {
            Manifestation man = rm .getManifestation();
            if ( man instanceof Connector )
            {
                ballIndices .add( sortedVertexList .indexOf( man .getLocation() ) );
            }
            else if ( man instanceof Strut )
            {
                IndexedModel.Strut strut = new IndexedModel.Strut();
                int start = sortedVertexList .indexOf( man .getLocation() );
                int end = sortedVertexList .indexOf( ((Strut) man) .getEnd() );
                boolean reverse = rm .getStrutSense() == Axis.MINUS;
                strut .start = reverse? end : start;
                strut .end = reverse? start : end;
                strut .length = rm .getStrutLength();
                strut .orbit = rm .getStrutOrbit() .getName();
                strut .orientation = rm .getStrutZone();
                Color color = man .getColor();
                if ( color != null )
                    strut .color = color .toWebString();
                struts .add( strut );
            }
            else if ( man instanceof Panel )
            {
                IndexedModel.Panel panel = new IndexedModel.Panel();
                @SuppressWarnings("unchecked")
                Stream<AlgebraicVector> vertexStream = StreamSupport.stream( ( (Iterable<AlgebraicVector>) man ) .spliterator(), false );
                panel .vertices = vertexStream.map( v -> sortedVertexList .indexOf( v ) ). collect( Collectors.toList() )
                                        .stream() .mapToInt(i->i) .toArray() ;
                Color color = man .getColor();
                if ( color != null )
                    panel .color = color .toWebString();
                panels .add( panel );
            }
        }
        indexedModel .struts = struts .toArray( new IndexedModel.Strut[] {} );
        indexedModel .panels = panels .toArray( new IndexedModel.Panel[] {} );
        indexedModel .field = mModel .getField() .getName();
        indexedModel .symmetry = mModel .getOrbitSource() .getSymmetry() .getName();

        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory .createGenerator( writer );
        generator .useDefaultPrettyPrinter();
        generator .setCodec( mapper );

        generator .writeStartObject();
        generator .writeStringField( "field", mModel .getField() .getName() );
        generator .writeStringField( "symmetry", mModel .getOrbitSource() .getSymmetry() .getName() );
        generator .writeObjectField( "vertices", sortedVertexList );
        generator .writeObjectField( "balls", ballIndices );
        generator .writeObjectField( "struts", struts );
        generator .writeObjectField( "panels", panels );
        generator .writeEndObject();
        generator.close();
    }

    @Override
    public String getFileExtension()
    {
        return "vson";
    }

    @Override
    public String getContentType()
    {
        return "application/json";
    }
}


