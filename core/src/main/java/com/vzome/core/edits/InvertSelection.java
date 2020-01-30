
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package com.vzome.core.edits;


import com.vzome.core.editor.ChangeSelection;
import com.vzome.core.editor.Selection;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.RealizedModel;

public class InvertSelection extends ChangeSelection
{
    protected final RealizedModel mManifestations;

    public InvertSelection( Selection selection, RealizedModel model )
    {
        super( selection );
        mManifestations = model;
    }
    
    @Override
    public void perform()
    {
        for (Manifestation m : mManifestations) {
            if ( m .getRenderedObject() != null )
            {
                if ( mSelection .manifestationSelected( m ) )
                    unselect( m );
                else
                    select( m );
            }
        }
        redo();
    }

    @Override
    protected String getXmlElementName()
    {
        return "InvertSelection";
    }

}
