package com.vzome.desktop.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.vorthmann.ui.Controller;
import org.vorthmann.ui.DefaultController;
import org.vorthmann.zome.app.impl.LengthController;
import org.vorthmann.zome.app.impl.SymmetrySnapper;
import org.vorthmann.zome.app.impl.ToolFactoryController;

import com.vzome.api.Tool;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.editor.SymmetrySystem;
import com.vzome.core.math.symmetry.Axis;
import com.vzome.core.math.symmetry.Direction;
import com.vzome.core.math.symmetry.OrbitSet;
import com.vzome.core.math.symmetry.Symmetry;
import com.vzome.core.render.Color;
import com.vzome.core.render.RenderedModel;
import com.vzome.core.render.Shapes;

public class SymmetryController extends DefaultController
{
    protected SymmetrySystem symmetrySystem;
    public OrbitSet availableOrbits;
    public OrbitSet snapOrbits;
    public OrbitSet buildOrbits;
    public OrbitSet renderOrbits;
    protected final CameraController.Snapper snapper;
    public Map<Direction, LengthController> orbitLengths = new HashMap<>();
    protected final Map<String, Controller> symmetryToolFactories = new LinkedHashMap<>();
    protected final Map<String, Controller> transformToolFactories = new LinkedHashMap<>();
    protected final Map<String, Controller> linearMapToolFactories = new LinkedHashMap<>();
    protected final RenderedModel renderedModel;

    public SymmetryController( Controller parent, SymmetrySystem model, RenderedModel mRenderedModel )
    {
        this .symmetrySystem = model;
        renderedModel = mRenderedModel;
        Symmetry symmetry = model .getSymmetry();
        availableOrbits = new OrbitSet( symmetry );
        snapOrbits = new OrbitSet( symmetry );
        buildOrbits = new OrbitSet( symmetry );
        renderOrbits = new OrbitSet( symmetry );
        snapper = new SymmetrySnapper( snapOrbits );
        for (Direction dir : symmetry .getOrbitSet()) {
            if ( dir .isStandard() )
            {
                availableOrbits .add( dir );
                snapOrbits .add( dir );
                Axis zone = dir .getAxis( 0, 0 );
                if ( zone .getRotationPermutation() != null )
                {
                    buildOrbits .add( dir );
                }
            }
            renderOrbits .add( dir );
        }
        for ( Direction dir : this .symmetrySystem .getOrbits() ) {
            LengthController lengthModel = new LengthController( dir );
            orbitLengths .put( dir, lengthModel );
        }
        if ( parent .propertyIsTrue( "disable.known.directions" ) )
            this .symmetrySystem .disableKnownDirection();
    }

    @Override
    public Controller getSubController( String name )
    {
        if ( name .startsWith( "length." ) )
        {
            String dirName = name .substring( "length." .length() );
            Direction dir = this .symmetrySystem .getOrbits() .getDirection( dirName );
            return getLengthController( dir );
        }
        Controller result = this .symmetryToolFactories .get( name );
        if ( result != null )
            return result;
        result = this .transformToolFactories .get( name );
        if ( result != null )
            return result;
        result = this .linearMapToolFactories .get( name );
        if ( result != null )
            return result;
        return super .getSubController( name );
    }

    @Override
    public String getProperty( String string )
    {
        switch ( string ) {
        
        case "name":
            return this .symmetrySystem .getName();

        case "renderingStyle":
            return this .symmetrySystem .getStyle() .getName();

        case "modelResourcePath":
            return this .symmetrySystem .getModelResourcePath();

        default:
            if ( string .startsWith( "orbitColor." ) )
            {
                String name = string .substring( "orbitColor." .length() );
                Direction dir = buildOrbits .getDirection( name );
                Color color = getColor( dir );
                return color .toString();
            }
            return super.getProperty( string );
        }
    }

    @Override
    public String[] getCommandList( String listName )
    {
        switch ( listName ) {

        case "styles":

            return this .symmetrySystem .getStyleNames();

        case "orbits":

            String[] result = new String[ this .symmetrySystem .getOrbits() .size() ];
            int i = 0;
            for (Direction orbit : this .symmetrySystem .getOrbits()) {
                result[ i ] = orbit .getName();
                i++;
            }
            return result;

        case "symmetryToolFactories":

            // This will be called only once, before any relevant getSubController, so it is OK to do creations
            for ( Tool.Factory factory : this .symmetrySystem .getToolFactories( Tool.Kind.SYMMETRY ) )
                this .symmetryToolFactories .put( factory .getId(), new ToolFactoryController( factory ) );
            return this .symmetryToolFactories .keySet() .toArray( new String[]{} );

        case "transformToolFactories":

            // This will be called only once, before any relevant getSubController, so it is OK to do creations
            for ( Tool.Factory factory : this .symmetrySystem .getToolFactories( Tool.Kind.TRANSFORM ) )
                this .transformToolFactories .put( factory .getId(), new ToolFactoryController( factory ) );
            return this .transformToolFactories .keySet() .toArray( new String[]{} );

        case "linearMapToolFactories":

            // This will be called only once, before any relevant getSubController, so it is OK to do creations
            for ( Tool.Factory factory : this .symmetrySystem .getToolFactories( Tool.Kind.LINEAR_MAP ) )
                this .linearMapToolFactories .put( factory .getId(), new ToolFactoryController( factory ) );
            return this .linearMapToolFactories .keySet() .toArray( new String[]{} );

        case "builtInSymmetryTools":

            // This will be called only once, before any relevant getSubController, so it is OK to do creations
            List<String> toolNames = new ArrayList<>();
            for ( Tool tool : this .symmetrySystem .getPredefinedTools( Tool.Kind.SYMMETRY ) )
                toolNames .add( tool .getId() );
            return toolNames .toArray( new String[]{} );


        case "builtInTransformTools":

            // This will be called only once, before any relevant getSubController, so it is OK to do creations
            List<String> transformToolNames = new ArrayList<>();
            for ( Tool tool : this .symmetrySystem .getPredefinedTools( Tool.Kind.TRANSFORM ) )
                transformToolNames .add( tool .getId() );
            return transformToolNames .toArray( new String[]{} );

        default:
            return super .getCommandList( listName );
        }
    }

    @Override
    public void doAction( String action ) throws Exception
    {
        switch (action) {

        case "ReplaceWithShape":
            action += "/" + this .symmetrySystem .getName() + ":" + this .symmetrySystem .getStyle() .getName();
            super .doAction( action );
            break;

        default:
            if ( action .startsWith( "setStyle." ) )
            {
                String styleName =  action .substring( "setStyle." .length() );
                this .symmetrySystem .setStyle( styleName );
                this .renderedModel .setShapes( this .symmetrySystem .getShapes() );
            }
            else {
                boolean handled = this .symmetrySystem .doAction( action );
                if ( ! handled )
                    super .doAction( action );
            }
            break;
        }
    }

    // TODO this should take over all functions of symmetry.getAxis()

    public Symmetry getSymmetry()
    {
        return this .symmetrySystem .getSymmetry();
    }

    public CameraController.Snapper getSnapper()
    {
        return snapper;
    }

    public LengthController getLengthController( Direction dir )
    {
        LengthController result = orbitLengths .get( dir );
        if ( result == null && dir != null )
        {
            result = new LengthController( dir );
            orbitLengths .put( dir, result );
            renderOrbits .add( dir );
            availableOrbits .add( dir );
        }
        return result;
    }

    public OrbitSet getOrbits()
    {
        return this .symmetrySystem .getOrbits();
    }

    public RenderedModel.OrbitSource getOrbitSource()
    {
        return this .symmetrySystem;
    }

    public Axis getZone( AlgebraicVector offset )
    {
        return this .symmetrySystem .getAxis( offset );
    }

    public Color getColor( Direction orbit )
    {
        return this .symmetrySystem .getColor( orbit );
    }

    public OrbitSet getBuildOrbits()
    {
        return this .buildOrbits;
    }

    public Shapes getRenderingStyle()
    {
        return this .symmetrySystem .getRenderingStyle();
    }
}