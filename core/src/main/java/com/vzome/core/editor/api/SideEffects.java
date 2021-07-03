
package com.vzome.core.editor.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.vzome.core.commands.Command;

public abstract class SideEffects extends UndoableEdit
{
    @Override
    public Element getDetailXml( Document doc )
    {
        Element result = this .getXml( doc );
        Element effects = doc .createElement( "effects" );
        
        for (SideEffect se : mItems) {
            if ( se != null )
            {
                Element effect = se .getXml( doc );
                if ( effect != null ) {
//                    if ( BUG_ACCOMMODATION_LOGGER .isLoggable( Level.FINEST ) )
//                        BUG_ACCOMMODATION_LOGGER .finest( "side-effect: " + DomSerializer .getXmlString( effect ) );
                    effects .appendChild( effect );
                }
                // else effect was ChangeConstructions.AttachConstruction, which we don't need to serialize
            }
        }
        result .appendChild( effects );
        return result;
    }

    @Override
    public boolean isSticky()
    {
        return false;
    }

    private List<SideEffect> mItems = new ArrayList<>();
    
    /**
     * This lets us use this pattern:
     *      plan plan plan plan
     *      redo
     *      plan plan plan
     *      redo
     *      
     * ... so we can sync the state with a redo
     * 
     */
    private int redone = 0;

    private static final Logger BUG_ACCOMMODATION_LOGGER = Logger.getLogger( "com.vzome.core.bug.accommodations" );

    public static void logBugAccommodation( String accommodation )
    {
        if ( BUG_ACCOMMODATION_LOGGER .isLoggable( Level.WARNING ) )
            BUG_ACCOMMODATION_LOGGER .warning( "ACCOMMODATION: " + accommodation );
    }
    
    @Override
    public boolean isVisible()
    {
    	return true;
    }

    @Override
    public boolean isDestructive()
    {
        return true;
    }

    protected void plan( SideEffect se )
    {
        mItems .add( se );
    }
    
    @Override
    public void perform() throws Command.Failure
    {
        // this default assumes that the constructor has already planned the SideEffects.
        // The more correct pattern is to OVERRIDE perform(), and plan side-effects there,
        //  then either redo() (perhaps several times) or call this inherited perform().
        redo();
    }
    
    protected void fail( String message ) throws Command.Failure
    {
        undo(); // in case there has been any redo() already
        throw new Command.Failure( message );
    }

    @Override
    public void configure( Map<String,Object> props ) {}

    @Override
    public boolean isNoOp()
    {
        return this.mItems.size() == 0;
    }

    @Override
    public void redo()
    {
        for ( int i = redone; i < mItems .size(); i++ )
        {
            SideEffect se = mItems .get( i );
            if ( se != null )
                se .redo();
        }
        redone = mItems .size();
    }

    @Override
    public void undo()
    {
        for ( int i = mItems .size(); i > 0; i-- )
        {
            SideEffect se = mItems .get( i-1 );
            if ( se != null )
                se .undo();
        }
        redone = 0;
    }

	protected Iterator<SideEffect> getEffects()
	{
		return this .mItems .iterator();
	}
}
