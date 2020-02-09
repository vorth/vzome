
package org.vorthmann.ui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.vorthmann.j3d.MouseTool;
import org.vorthmann.j3d.MouseToolFilter;

/**
 * A MouseToolFilter that excludes "popup trigger" events and wheel events.
 * 
 * @author Scott Vorthmann
 *
 */
public class LeftMouseFilter extends MouseToolFilter
{
    private transient boolean mPopupTrigger;
    
    public LeftMouseFilter( MouseTool delegate )
    {
        super( delegate );
    }

    @Override
    public void mousePressed( MouseEvent e )
    {
        if ( e .isPopupTrigger() ) {
            mPopupTrigger = true;
            return;
        }
        super .mousePressed( e );
    }

    @Override
    public void mouseDragged( MouseEvent mouseEvt )
    {
        if ( mouseEvt .isPopupTrigger() )
            return;

        super .mouseDragged( mouseEvt );
    }

    @Override
    public void mouseReleased( MouseEvent mouseEvt )
    {
        if ( mouseEvt .isPopupTrigger() )
            return;
        
        super .mouseReleased( mouseEvt );
    }

    @Override
    public void mouseClicked( MouseEvent mouseEvt )
    {
        if ( mouseEvt .isPopupTrigger() )
            return;
        
        super .mouseClicked( mouseEvt );
    }

    @Override
    public void mouseWheelMoved( MouseWheelEvent arg0 )
    {}
}
