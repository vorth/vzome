
package com.vzome.core.edits;

import com.vzome.core.commands.Command.Failure;
import com.vzome.core.editor.api.ChangeSelection;
import com.vzome.core.editor.api.EditorModel;


public class ValidateSelection extends ChangeSelection
{
    @Override
    public void perform() throws Failure
    {
        if ( mSelection .size() == 0 )
            throw new Failure( "selection is empty" );
    }

    public ValidateSelection( EditorModel editor )
    {
        super( editor .getSelection() ); 
    }

    @Override
    protected String getXmlElementName()
    {
        return "ValidateSelection";
    }

}
