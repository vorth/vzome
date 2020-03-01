
package org.vorthmann.j3d;

public interface CanvasTool
{    
    void attach( Object canvas ); // really Component, but we cannot use AWT in the interface
    
    void detach( Object canvas );
}
