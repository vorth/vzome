
package com.vzome.core.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.editor.Application;
import com.vzome.core.math.Projection;

public class VsonDeserializer extends StdDeserializer<RealizedModel>
{
    private final Application app;

    public VsonDeserializer( Application app )
    { 
        super( RealizedModel.class ); 
        this.app = app;
    } 

    @Override
    public RealizedModel deserialize( JsonParser jp, DeserializationContext ctxt ) 
            throws IOException
    {
        JsonNode node = jp .getCodec() .readTree(jp);
        
        JsonNode fieldNode = node .get( "field" );
        if ( fieldNode == null )
            throw new IOException( "No field name in JSON object." );
        AlgebraicField field = this .app .getField( fieldNode .asText() );
                
        //        int userId = (Integer) ((IntNode) node.get("createdBy")).numberValue();
        
        return new RealizedModel( field, new Projection.Default( field ) );
    }
}