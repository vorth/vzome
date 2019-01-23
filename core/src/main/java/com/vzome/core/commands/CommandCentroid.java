

package com.vzome.core.commands;

import java.util.ArrayList;
import java.util.List;

import com.vzome.core.construction.CentroidPoint;
import com.vzome.core.construction.Construction;
import com.vzome.core.construction.ConstructionChanges;
import com.vzome.core.construction.ConstructionList;
import com.vzome.core.construction.Point;

/**
 * @author Scott Vorthmann
 */
public class CommandCentroid extends AbstractCommand
{
    private static final Object[][] PARAM_SIGNATURE =
        new Object[][]{ { GENERIC_PARAM_NAME, Point.class } };

    private static final Object[][] ATTR_SIGNATURE = new Object[][]{};

    @Override
    public Object[][] getParameterSignature()
    {
        return PARAM_SIGNATURE;
    }

    @Override
    public Object[][] getAttributeSignature()
    {
        return ATTR_SIGNATURE;
    }
    
    @Override
    public ConstructionList apply( ConstructionList parameters, AttributeMap attrs, ConstructionChanges effects ) throws Failure
    {
        ConstructionList result = new ConstructionList();
        if ( parameters == null || parameters .size() == 0 )
            throw new Failure( "Select two or more balls to compute their centroid." );
        final Construction[] params = parameters .getConstructions();
        
        List<Point> verticesList = new ArrayList<>();
        for (Construction param : params) {
            if (param instanceof Point) {
                verticesList.add((Point) param);
            }
        }
        // Checking if verticesList .size() < 2 causes old files to fail to load
        // but failing to check if verticesList .isEmpty() throws ArrayIndexOutOfBoundsException.
        // Allowing a single ball to be its own centroid will let older files load,
        // but will generate an appropriate warning if any struts or panels are selected, but no balls.
        if ( verticesList .isEmpty() )
            throw new Failure( "Select two or more balls to compute their centroid." );
        Point[] points = new Point[0];
        CentroidPoint centroid = new CentroidPoint( verticesList .toArray( points ) );
        effects .constructionAdded( centroid );
        result .addConstruction( centroid );
        return result;
    }
}
