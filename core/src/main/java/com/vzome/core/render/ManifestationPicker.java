
package com.vzome.core.render;

import java.awt.event.MouseEvent;
import java.util.Collection;

import com.vzome.core.model.Manifestation;

public interface ManifestationPicker
{
    public Manifestation pickManifestation( MouseEvent e );
    
    public Collection<Manifestation> pickCube();
}
