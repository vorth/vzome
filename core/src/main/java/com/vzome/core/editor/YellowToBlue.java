
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package com.vzome.core.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.commands.Command;
import com.vzome.core.commands.Command.Failure;
import com.vzome.core.construction.CentroidPoint;
import com.vzome.core.construction.FreePoint;
import com.vzome.core.construction.Point;
import com.vzome.core.model.Connector;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.RealizedModel;
import com.vzome.core.model.Strut;

public class YellowToBlue extends ChangeManifestations
{
    @Override
    public void perform() throws Command.Failure
    {
    	Queue<Connector> workList = new LinkedList<Connector>();
    	List<Strut> struts = new ArrayList<Strut>();
    	List<Connector> balls = new ArrayList<Connector>();
    	Map<Connector,AlgebraicVector> pointMap = new HashMap<Connector,AlgebraicVector>();
    	
        for (Manifestation man : mSelection) {
			if ( man instanceof Connector ) {
				if ( workList .isEmpty() ) // just once, seed the workList
				{
					Connector ball = (Connector) man ;
					workList .add( ball );
					pointMap .put( ball, ball .getLocation() ); // the only ball coincident with its mapped point, probably
				}
				else
					balls .add( (Connector) man );
			}
            else if ( man instanceof Strut )
            	struts .add( (Strut) man );  // speed the neighbor detection later
        }
        
        while ( ! workList .isEmpty() )
        {
        	Connector ball = (Connector)  workList .remove();
            unselect( ball );
            Point point = new FreePoint( pointMap .get( ball ) );
            manifestConstruction( point );
            AlgebraicVector loc = ball .getLocation();
            ListIterator<Strut> strutsIter = struts.listIterator();
            while( strutsIter.hasNext() )
            {
            	Strut strut = strutsIter .next();
            	AlgebraicVector end;
                if ( loc .equals( strut .getLocation() ) ) {
                    end = strut .getEnd();
                }
                else if (loc .equals( strut .getEnd() ) ) {
                    end = strut .getLocation();
                }
                else
                	continue;

                // if we arrive here, we found a neighbor strut
                strutsIter .remove();
                unselect( strut );
                
                ListIterator<Connector> ballsIter = balls .listIterator();
                while( ballsIter .hasNext() )
                {
                	Connector endBall = ballsIter .next();
                    if ( end .equals( endBall .getLocation() ) ) {
                        // if we arrive here, we found the end ball
                        ballsIter .remove();
                        end = strut .getEnd();
                        break;
                    }
                }
                
                // now, map the strut, and compute a new "end"
                
                // finally, record the endBall->end mapping,
                // and add endBall to the workList
            }
		}

        //manifestConstruction( centroid );

        redo();
    }

    public YellowToBlue( Selection selection, RealizedModel realized, boolean groupInSelection )
    {
        super( selection, realized, groupInSelection );
    }
        
    @Override
    protected String getXmlElementName()
    {
        return "YellowToBlue";
    }
}
