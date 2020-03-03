package com.vzome.desktop.awt;

import java.awt.Dimension;
import java.awt.Graphics;

import org.vorthmann.ui.Controller;

public interface GraphicsController extends Controller
{
    void repaintGraphics( String panelName, Graphics graphics, Dimension size );
}
