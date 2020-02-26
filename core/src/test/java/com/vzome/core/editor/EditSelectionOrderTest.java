package com.vzome.core.editor;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.PentagonField;
import com.vzome.core.construction.FreePoint;
import com.vzome.core.construction.Point;
import com.vzome.core.math.Projection;
import com.vzome.core.model.RealizedModel;

public class EditSelectionOrderTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test()
    {
        AlgebraicField field = new PentagonField();
        RealizedModel model = new RealizedModel( field, new Projection.Default( field ) );
        Point origin = new FreePoint( field .origin( 3 ) );
        EditorModel editorModel = new EditorModel( model, origin, null, null, new HashMap<String, SymmetrySystem>() );
        try {
            String packageName = "com.vzome.core.edits";
            List<String> classNamesFromPackage = getClassNamesFromPackage( packageName )
                    .stream() .filter( s8 -> ! s8.contains( "$" ) ) .collect( Collectors .toList() );
            for ( String className : classNamesFromPackage ) {
                UndoableEdit edit = editorModel .createEdit( className );
                if ( edit == null )
                    continue;
                if ( edit instanceof ChangeSelection ) {
                    ChangeSelection change = (ChangeSelection) edit;
                    if ( change .hasOrderedSelection() ) {
                        System.out.println( className );
                        // Nothing prints out, since we never setOrderedSelection() except
                        //   from within perform().
                        //   I'm abandoning this testing effort, due to the complexity of getting it right.
                        //   The behavior can vary even for a single class, depending on how the edit
                        //   is configured.
                    }
                    else {
                        
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            fail( e .getMessage() );
        }
    }

    public static ArrayList<String> getClassNamesFromPackage( String packageName )
            throws IOException, URISyntaxException
    {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL packageURL;
        ArrayList<String> names = new ArrayList<String>();;

        packageName = packageName.replace(".", "/");
        packageURL = classLoader.getResource(packageName);

        URI uri = new URI(packageURL.toString());
        File folder = new File(uri.getPath());
            // won't work with path which contains blank (%20)
            // File folder = new File(packageURL.getFile()); 
            File[] contenuti = folder.listFiles();
            String entryName;
            for(File actual: contenuti){
                entryName = actual.getName();
                entryName = entryName.substring(0, entryName.lastIndexOf('.'));
                names.add(entryName);
            }
        
        return names;
    }

}
