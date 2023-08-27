package com.vzome.core.kinds;

import java.util.ArrayList;
import java.util.List;

import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.commands.Command;
import com.vzome.core.commands.CommandSymmetry;
import com.vzome.core.editor.SymmetryPerspective;
import com.vzome.core.editor.api.Shapes;
import com.vzome.core.math.symmetry.Axis;
import com.vzome.core.math.symmetry.Direction;
import com.vzome.core.math.symmetry.OctahedralSymmetry;
import com.vzome.core.math.symmetry.Symmetry;

public abstract class AbstractSymmetryPerspective implements SymmetryPerspective
{
    protected final Symmetry symmetry;

    private final List<Shapes> geometries = new ArrayList<>();

    private Shapes defaultShapes = null;

    public AbstractSymmetryPerspective(Symmetry symmetry) {
        this.symmetry = symmetry;
    }

    @Override
    public Symmetry getSymmetry() {
        return this.symmetry;
    }

    @Override
    public String getName() {
        return getSymmetry().getName();
    }

    @Override
    public String getLabel() {
        return getSymmetry().getName();
    }

    protected void addShapes(Shapes shapes) {
        Shapes old = getGeometry(shapes.getName());
        if(old != null) {
            this.geometries.remove(old);
        }
        this.geometries.add(shapes);
    }

    protected void clearShapes() {
        this.geometries.clear();
        defaultShapes= null;
    }

    @Override
    public List<Shapes> getGeometries() {
        return this.geometries;
    }

    private Shapes getGeometry(String name) {
        for(Shapes shapes : geometries) {
            if(shapes.getName().equals(name)) {
                return shapes;
            }
        }
        return null;
    }

    public void setDefaultGeometry(Shapes shapes) {
        this.defaultShapes = shapes;
        addShapes(shapes);
    }

    @Override
    public Shapes getDefaultGeometry() {
        return this.defaultShapes;
    }

    @Override
    public Command getLegacyCommand( String action )
    {
        switch ( action ) {
        case "octasymm":
        {
            Symmetry octaSymm = getSymmetry();
            if(! (octaSymm instanceof OctahedralSymmetry) ) {
                // only make a new OctahedralSymmetry if necessary
                octaSymm = new OctahedralSymmetry(octaSymm.getField());
            }
            // This command will be availble to all SymmetryPerspectives even if they are not Octahedral
            // TODO: This legacy command should probably eventually be removed
            // after we ensure that doing so is backward compatible. 
            return new CommandSymmetry( octaSymm );
        }
        
        default:
            return null;
        }
    }

    @Override
    public boolean orbitIsStandard( Direction orbit )
    {
        return orbit .isStandard();
    }

    @Override
    public boolean orbitIsBuildDefault( Direction orbit )
    {
        Axis zone0 = orbit .getAxis( 0, 0 );
        return zone0 .getRotationPermutation() != null;
    }
    
    @Override
    public AlgebraicNumber getOrbitUnitLength( Direction orbit )
    {
        return orbit .getUnitLength();
    }
}
