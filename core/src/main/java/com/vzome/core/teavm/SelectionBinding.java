package com.vzome.core.teavm;

import java.util.Iterator;
import java.util.List;

import com.vzome.core.editor.Selection;
import com.vzome.core.model.Manifestation;

public class SelectionBinding implements Selection
{
    @JSBody( params = { "message" }, script = "console.log(message)")
    public static native void log( String message );
    
    @Override
    public Iterator<Manifestation> iterator()
    {
        log( "SelectionBinding.iterator()" );
        return null;
    }

    @Override
    public void clear()
    {
        log( "SelectionBinding.iterator()" );
    }

    @Override
    public boolean manifestationSelected( Manifestation man )
    {
        log( "SelectionBinding.manifestationSelected()" );
        return false;
    }

    @Override
    public void selectWithGrouping( Manifestation mMan )
    {
        log( "SelectionBinding.selectWithGrouping()" );
    }

    @Override
    public void unselectWithGrouping(Manifestation mMan)
    {
        log( "SelectionBinding.unselectWithGrouping()" );
    }

    @Override
    public void select(Manifestation mMan)
    {
        log( "SelectionBinding.select()" );
    }

    @Override
    public void unselect(Manifestation mMan)
    {
        log( "SelectionBinding.unselect()" );
    }

    @Override
    public void gatherGroup()
    {
        log( "SelectionBinding.gatherGroup()" );
    }

    @Override
    public void gatherGroup211()
    {
        log( "SelectionBinding.gatherGroup211()" );
    }

    @Override
    public void scatterGroup()
    {
        log( "SelectionBinding.scatterGroup()" );
    }

    @Override
    public void scatterGroup211()
    {
        log( "SelectionBinding.scatterGroup211()" );
    }

    @Override
    public boolean isSelectionAGroup()
    {
        log( "SelectionBinding.isSelectionAGroup()" );
        return false;
    }

    @Override
    public int size()
    {
        log( "SelectionBinding.size()" );
        return 0;
    }

    @Override
    public void copy(List<Manifestation> bookmarkedSelection)
    {
        log( "SelectionBinding.copy()" );
    }

}
