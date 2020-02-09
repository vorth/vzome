
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package org.vorthmann.ui;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.vorthmann.j3d.MouseTool;
import org.vorthmann.j3d.MouseToolFilter;

/**
 * A MouseToolFilter that separates press-release sequences into pure clicks and
 * press-drag-release sequences with at least three drags.
 * Press-release-click sequences are never generated.
 * 
 * @author Scott Vorthmann
 *
 */
public class MouseDragAdapter extends MouseToolFilter
{
    private transient MouseEvent pressEvent, lastDragEvent;
        
    private transient boolean dragged;
    
    public MouseDragAdapter( MouseTool delegate )
    {
        super( delegate );
    }

    @Override
    public void mousePressed( MouseEvent e )
    {
        pressEvent = e; // will be emitted in mouseDragged, eventually, if at all
        this .dragged = false;
    }

    @Override
    public void mouseDragged( MouseEvent mouseEvt )
    {
        if ( this .nearMousePress( mouseEvt ) ) {
            // Yes, overwrite... we only need the most recent one below
            this .lastDragEvent = mouseEvt;
            return;
        }

        this .dragged = true;
        
        if ( this .lastDragEvent != null ) {
            super .mousePressed( lastDragEvent );
            this .lastDragEvent = null;
            // We know this can't be too far away, so we won't get surprising jumps
            super .mouseDragged( mouseEvt );
        }
        else
            super .mouseDragged( mouseEvt );
    }

    @Override
    public void mouseReleased( MouseEvent mouseEvt )
    {
        if ( this .dragged )
            super .mouseReleased( mouseEvt );
    }
    
    private boolean nearMousePress( MouseEvent mouseEvt )
    {
        if ( pressEvent == null )
            // already dragged
            return false;

        if ( mouseEvt .getWhen() - pressEvent .getWhen() >= 100 )
            return false;

        if ( mouseEvt .getX() - pressEvent .getX() >= 2 )
            return false;

        if ( mouseEvt .getY() - pressEvent .getY() >= 2 )
            return false;
        
        return true;
    }

    @Override
    public void mouseClicked( MouseEvent mouseEvt )
    {
        // only generate a click if the pressEvent is near
        //  enough in both time and space
        if ( this .nearMousePress( mouseEvt ) )
            super .mouseClicked( mouseEvt );
    }

    @Override
    public void mouseWheelMoved( MouseWheelEvent arg0 )
    {}
}
