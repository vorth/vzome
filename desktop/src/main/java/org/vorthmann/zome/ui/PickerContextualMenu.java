package org.vorthmann.zome.ui;

import javax.swing.JMenuItem;

import org.vorthmann.ui.Controller;

class PickerContextualMenu extends ContextualMenu
{
    private final Controller controller;

    public PickerContextualMenu( Controller controller, ControlActions enabler, String key )
    {
        super();
        this .controller = controller;
        boolean oldTools = true;// controller .propertyIsTrue( "original.tools" );
        this .setLightWeightPopupEnabled( false );

        this .add( setMenuAction( "copyThisView", new JMenuItem( "Copy This View" ) ) );
        this .add( setMenuAction( "useCopiedView", new JMenuItem( "Use Copied View" ) ) );

        this .addSeparator();

        this .add( setMenuAction( "lookAtThis", new JMenuItem( "Look At This" ) ) );
        this .add( setMenuAction( "lookAtOrigin", new JMenuItem( "Look At Origin" ) ) );

        if ( oldTools ) {
            this .add( setMenuAction( "lookAtSymmetryCenter", new JMenuItem( "Look At Symmetry Center" ) ) );
            this .addSeparator();
            this .add( setMenuAction( "SymmetryCenterChange", new JMenuItem( "Set Symmetry Center" ) ) );
            this .add( setMenuAction( "SymmetryAxisChange", new JMenuItem( "Set Symmetry Axis" ) ) );
        }

        this .addSeparator();

        this .add( setMenuAction( "setWorkingPlane", new JMenuItem( "Set Working Plane" ) ) );
        this .add( setMenuAction( "setWorkingPlaneAxis", new JMenuItem( "Set Working Plane Axis" ) ) );

        this .addSeparator();

        this .add( setMenuAction( "SelectCollinear", new JMenuItem( "Select Collinear" ) ) );
        this .add( setMenuAction( "SelectParallelStruts", new JMenuItem( "Select Parallel Struts" ) ) );
        this .add( setMenuAction( "AdjustSelectionByOrbitLength/selectSimilarStruts", new JMenuItem( "Select Similar Struts" ) ) );

        this .add( setMenuAction( "undoToManifestation", new JMenuItem( "Undo Including This" ) ) );

        this .addSeparator();

        this .add( enabler .setMenuAction( "setBackgroundColor", this .controller, new JMenuItem( "Set Background Color..." ) ) );

        this .addSeparator();

        this .add( setMenuAction( "ReplaceWithShape", new JMenuItem( "Replace With Panels" ) ) );

        this .addSeparator();

        this .add( setMenuAction( "setBuildOrbitAndLength", new JMenuItem( "Build With This" ) ) );
        this .add( enabler .setMenuAction( "showProperties-"+key, this .controller, new JMenuItem( "Show Properties" ) ) );
    }

    private JMenuItem setMenuAction( String action, JMenuItem control )
    {
        control .setEnabled( true );
        control .setActionCommand( action );
        control .addActionListener( this .controller );
        return control;
    }
}