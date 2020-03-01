
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package com.vzome.desktop.controller;

import org.vorthmann.ui.Controller;

import com.vzome.core.render.RenderingChanges;
import com.vzome.core.viewing.Lights;

public interface Controller3d extends Controller
{
    void attachViewer( RenderingViewer viewer, RenderingChanges scene, Object canvas );

    Lights getSceneLighting();
}
