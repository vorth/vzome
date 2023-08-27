package com.vzome.core.kinds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.vzome.api.Tool;
import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.commands.Command;
import com.vzome.core.commands.CommandAxialSymmetry;
import com.vzome.core.editor.SymmetryPerspective;
import com.vzome.core.editor.ToolsModel;
import com.vzome.core.tools.AxialSymmetryToolFactory;
import com.vzome.core.tools.LinearMapToolFactory;
import com.vzome.core.tools.MirrorToolFactory;
import com.vzome.core.tools.RotationToolFactory;
import com.vzome.core.tools.ScalingToolFactory;
import com.vzome.core.tools.SymmetryToolFactory;
import com.vzome.core.tools.TranslationToolFactory;
import com.vzome.core.viewing.AbstractShapes;
import com.vzome.core.viewing.ExportedVEFShapes;
import com.vzome.core.viewing.OctahedralShapes;
import com.vzome.fields.heptagon.HeptagonalAntiprismSymmetry;

/**
 * Everything here is stateless, or at worst, a cache (like Shapes).
 * An instance of this can be shared by many DocumentModels.
 * This is why it does not have tool factories, though it does
 * dictate what tool factories will be present.
 * 
 * @author Scott Vorthmann
 *
 */
public class HeptagonFieldApplication extends DefaultFieldApplication
{
	public HeptagonFieldApplication( AlgebraicField field )
	{
		super( field );
	}

    @Override
    public String getLabel()
    {
        return "Heptagon";
    }

    private final SymmetryPerspective correctedAntiprismPerspective = new HeptagonalSymmetryPerspective(true);
    private final SymmetryPerspective originalAntiprismPerspective = new HeptagonalSymmetryPerspective(false);

//    TriangularAntiprismSymmetry is not yet implemented correctly, but when it is, it will go here 
//    Symmetry taSymmetry = new TriangularAntiprismSymmetry( kind, "blue", "triangular antiprism" );
//    defaultShapes = new OctahedralShapes( "octahedral", "triangular antiprism", taSymmetry );

    private class HeptagonalSymmetryPerspective extends AbstractSymmetryPerspective
	{
        private boolean corrected;

        HeptagonalSymmetryPerspective(boolean corrected) {
	        super(new HeptagonalAntiprismSymmetry(getField(), "blue", corrected).createStandardOrbits( "blue" ));
            this.corrected = corrected;
	        AbstractShapes octahedralShapes = new OctahedralShapes( "octahedral", "triangular antiprism", symmetry );
	        AbstractShapes antiprismShapes = new ExportedVEFShapes( null, "heptagon/antiprism", "heptagonal antiprism", symmetry, octahedralShapes );
	        
	        // this is the order they will be shown on the dialog
            setDefaultGeometry(antiprismShapes);
	        addShapes(octahedralShapes);
	    }
	    
        @Override
        public String getLabel()
        {
            return this.corrected? "heptagonal antiprism" : null; // Hide the "corrected" part for the UI
            //  ... and the null return makes that variant invisible in the UI
        }

        @Override
        public List<Tool.Factory> createToolFactories( Tool.Kind kind, ToolsModel tools )
        {
            List<Tool.Factory> result = new ArrayList<>();
            switch ( kind ) {

            case SYMMETRY:
                result .add( new SymmetryToolFactory( tools, this .symmetry ) );
                result .add( new MirrorToolFactory( tools ) );
                result .add( new AxialSymmetryToolFactory( tools, this .symmetry ) );
                break;

            case TRANSFORM:
                result .add( new ScalingToolFactory( tools, this .symmetry ) );
                result .add( new RotationToolFactory( tools, this .symmetry ) );
                result .add( new TranslationToolFactory( tools ) );
                break;

            case LINEAR_MAP:
                result .add( new LinearMapToolFactory( tools, this .symmetry, false ) );
                break;

            default:
                break;
            }
            return result;
        }

        @Override
        public List<Tool> predefineTools( Tool.Kind kind, ToolsModel tools )
        {
            List<Tool> result = new ArrayList<>();
            switch ( kind ) {

            case SYMMETRY:
                result .add( new SymmetryToolFactory( tools, this .symmetry ) .createPredefinedTool( "heptagonal antiprism around origin" ) );
                result .add( new MirrorToolFactory( tools ) .createPredefinedTool( "reflection through XY plane" ) );
                result .add( new AxialSymmetryToolFactory( tools, this .symmetry ) .createPredefinedTool( "symmetry around red through origin" ) );
                break;

            case TRANSFORM:
                result .add( new ScalingToolFactory( tools, this .symmetry ) .createPredefinedTool( "scale down" ) );
                result .add( new ScalingToolFactory( tools, this .symmetry ) .createPredefinedTool( "scale up" ) );
                result .add( new RotationToolFactory(tools, this.symmetry, true) .createPredefinedTool( "rotate around red through origin" ) );
                result .add( new TranslationToolFactory( tools ) .createPredefinedTool( "b1 move along +X" ) );
                break;

            default:
                break;
            }
            return result;
        }

        private final Command axialsymm = new CommandAxialSymmetry( symmetry );

        @Override
        public Command getLegacyCommand( String action )
        {
            switch ( action ) {
            case "axialsymm":
                return axialsymm;
                
            default:
                return super.getLegacyCommand(action);
            }
        }

        @Override
        public String getModelResourcePath()
        {
            return "org/vorthmann/zome/app/heptagonal antiprism.vZome";
        }
	}

	@Override
	public Collection<SymmetryPerspective> getSymmetryPerspectives()
	{
		return Arrays.asList( this .correctedAntiprismPerspective, super .getDefaultSymmetryPerspective(), this .originalAntiprismPerspective );
	}

	@Override
	public SymmetryPerspective getDefaultSymmetryPerspective()
	{
		return this .correctedAntiprismPerspective;
	}

	@Override
	public SymmetryPerspective getSymmetryPerspective( String symmName )
	{
		switch ( symmName ) {

		case "heptagonal antiprism corrected":
			return this .correctedAntiprismPerspective;

		case "heptagonal antiprism":
			return this .originalAntiprismPerspective;

		default:
			return super .getSymmetryPerspective( symmName );
		}
	}
}
