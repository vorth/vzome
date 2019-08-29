package com.vzome.core.teavm;

import org.python.antlr.PythonParser.parameters_return;

import com.vzome.core.edits.Polytope4d;

public class Polytope4dRunner {

    public static void main( String[] args )
    {
        SelectionBinding selection = new SelectionBinding();
        RealizedModelBinding model = new RealizedModelBinding();
        Polytope4d edit = new Polytope4d( selection, model, fieldApp, null, 8, "H4" );
        edit .configure( params );
        edit .perform();
    }

}
