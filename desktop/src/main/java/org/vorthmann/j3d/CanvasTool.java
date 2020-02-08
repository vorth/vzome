
package org.vorthmann.j3d;

import java.awt.Component;

public interface CanvasTool
{
    void startHandlingMouseEvents( Component canvas );
    
    void stopHandlingMouseEvents( Component canvas );
}
