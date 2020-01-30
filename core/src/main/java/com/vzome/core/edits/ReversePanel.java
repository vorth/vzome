
//(c) Copyright 2008, Scott Vorthmann.  All rights reserved.

package com.vzome.core.edits;


import com.vzome.core.commands.Command.Failure;
import com.vzome.core.construction.Polygon;
import com.vzome.core.editor.ChangeManifestations;
import com.vzome.core.editor.Selection;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.Panel;
import com.vzome.core.model.RealizedModel;


/**
 * Work in progress, to help someone create correctly oriented surfaces for vZome part export,
 * or for StL 3D printing export.
 * 
 * @author Scott Vorthmann
 *
 */
public class ReversePanel extends ChangeManifestations
{
    @Override
    public void perform() throws Failure
    {
        if ( panel != null )
        {
            if ( this .mSelection .manifestationSelected( panel ) )
                unselect( panel );
            Polygon polygon = (Polygon) panel .getConstructions() .next();
            unmanifestConstruction( polygon );
            // TODO complete this
        }
        redo();
    }

    private final Panel panel;
        
    public ReversePanel( Manifestation singlePanel, Selection selection, RealizedModel realized )
    {
        super( selection, realized );
        if ( singlePanel != null )
            this .panel = (Panel) singlePanel;
        else
            this .panel = null;
    }

    @Override
    protected String getXmlElementName()
    {
        return "ReversePanel";
    }

}
