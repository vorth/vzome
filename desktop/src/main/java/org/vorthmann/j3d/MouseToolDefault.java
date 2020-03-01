

package org.vorthmann.j3d;

import java.awt.Component;
import java.awt.event.MouseWheelEvent;

import javax.swing.event.MouseInputAdapter;

/**
 * @author Scott Vorthmann
 *
 */
public class MouseToolDefault extends MouseInputAdapter implements MouseTool
{
    @Override
    public void attach( Object canvas )
    {
        Component component = (Component) canvas;
        component .addMouseListener( this );
        component .addMouseMotionListener( this );
        component .addMouseWheelListener( this );
    }
    
    @Override
    public void detach( Object canvas )
    {
        Component component = (Component) canvas;
        component .removeMouseListener( this );
        component .removeMouseMotionListener( this );
        component .removeMouseWheelListener( this );
    }

    @Override
    public void mouseWheelMoved( MouseWheelEvent arg0 )
    {}

}
