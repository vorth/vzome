
package com.vzome.core.render;

import java.util.Collection;

public interface ManifestationPicker
{
    public RenderedManifestation pickManifestation( Object mouseEvent );
    
    public Collection<RenderedManifestation> pickCube();
}
