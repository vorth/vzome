package com.vzome.core.editor;

import static com.vzome.core.editor.ChangeSelection.ActionEnum.DESELECT;
import static com.vzome.core.editor.ChangeSelection.ActionEnum.IGNORE;
import static com.vzome.core.editor.ChangeSelection.ActionEnum.SELECT;

import java.util.HashSet;
import java.util.Set;

import com.vzome.core.commands.Command;
import com.vzome.core.construction.Construction;
import com.vzome.core.construction.Point;
import com.vzome.core.construction.Segment;
import com.vzome.core.model.Connector;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.RealizedModel;
import com.vzome.core.model.Strut;

public class EditorModel
{
    public EditorModel( RealizedModel realized, Selection selection, boolean oldGroups, Point originPoint, SymmetrySystem symmetrySystem )
	{
        mRealized = realized;
        mSelection = selection;
        this.oldGroups = oldGroups;
		this.symmetrySystem = symmetrySystem;
        
		this .selectionSummary = new SelectionSummary( this .mSelection );

        Manifestation m = realized .manifest( originPoint );
        m .addConstruction( originPoint );
        realized .add( m );
        realized .show( m );
        mCenterPoint = originPoint;
    }
    
    public void addSelectionSummaryListener( SelectionSummary.Listener listener )
    {
    	this .selectionSummary .addListener( listener );
    }
    
    public RealizedModel getRealizedModel()
    {
        return mRealized;
    }
	
	public Point getCenterPoint()
	{
	    return mCenterPoint;
	}
    
    public void setCenterPoint( Construction point )
    {
        mCenterPoint = (Point) point;
    }
    
    public Segment getSymmetrySegment()
    {
        return mSymmetryAxis;
    }
    
    public void setSymmetrySegment( Segment segment )
    {
        mSymmetryAxis = segment;
    }

    public UndoableEdit selectManifestation( Manifestation m, boolean replace )
	{
        ChangeSelection edit = new SelectManifestation( m, replace, mSelection, mRealized, false );
        if ( edit .selectionChanged() )
            return edit;
        else
            return new NoOp();
    }

    public UndoableEdit selectAll()
    {
        ChangeSelection edit = new SelectAll( mSelection, mRealized, false );
        if ( edit .selectionChanged() )
            return edit;
        else
            return new NoOp();
    }

    public UndoableEdit unselectAll()
    {
        ChangeSelection edit = new DeselectAll( mSelection, false );
        if ( edit .selectionChanged() )
            return edit;
        else
            return new NoOp();
    }

	public UndoableEdit unselectConnectors()
	{
        return new AdjustSelectionByClass(mSelection, mRealized, DESELECT, IGNORE, IGNORE);
    }

    public UndoableEdit unselectStrutsAndPanels() {
        return new AdjustSelectionByClass(mSelection, mRealized, IGNORE, DESELECT, DESELECT);
    }

    /**
     * This legacy method name is misleading because it actually deselects both struts and panels.
     * @deprecated As of 8/23/2017: Use {@link #unselectStrutsAndPanels()} instead.
     */
    @Deprecated
	public UndoableEdit unselectStruts()
	{
        return unselectStrutsAndPanels();
    }

	public UndoableEdit deselectConnectors()
	{
        return new AdjustSelectionByClass( mSelection, mRealized, DESELECT, IGNORE, IGNORE );
    }

	public UndoableEdit deselectStruts()
	{
        return new AdjustSelectionByClass( mSelection, mRealized, IGNORE, DESELECT, IGNORE );
    }

	public UndoableEdit deselectPanels()
	{
        return new AdjustSelectionByClass( mSelection, mRealized, IGNORE, IGNORE, DESELECT );
    }

	public UndoableEdit selectConnectors()
	{
        return new AdjustSelectionByClass( mSelection, mRealized, SELECT, IGNORE, IGNORE );
    }

	public UndoableEdit selectStruts()
	{
        return new AdjustSelectionByClass( mSelection, mRealized, IGNORE, SELECT, IGNORE );
    }

	public UndoableEdit selectPanels()
	{
        return new AdjustSelectionByClass( mSelection, mRealized, IGNORE, IGNORE, SELECT );
    }

    public UndoableEdit selectNeighbors()
    {
        ChangeSelection edit = new SelectNeighbors( mSelection, mRealized, false );
        if ( edit .selectionChanged() )
            return edit;
        else
            return new NoOp();
    }

    public UndoableEdit selectAutomaticStruts()
    {
        return new SelectAutomaticStruts( symmetrySystem, mSelection, mRealized );
    }

    public UndoableEdit selectCollinear()
    {
        return new SelectCollinear(mSelection, mRealized );
    }

	public UndoableEdit selectParallelStruts() {
		return new SelectParallelStruts( symmetrySystem, mSelection, mRealized );
	}

    public UndoableEdit selectByDiameter()
    {
        return new SelectByDiameter(mSelection, mRealized );
    }

    public UndoableEdit selectByRadius()
    {
        return new SelectByRadius(mSelection, mRealized );
    }

    public UndoableEdit invertSelection()
    {
        return new InvertSelection( mSelection, mRealized, false );
        // always a change, by definition
    }
    
    public UndoableEdit joinSkewLines()
    {
        return new JoinSkewLines( mSelection, mRealized );
    }
    
    public UndoableEdit convexHull2d()
    {
        return new ConvexHull2d( mSelection, mRealized );
    }
    
    public UndoableEdit convexHull3d()
    {
        return new ConvexHull3d( mSelection, mRealized );
    }
    
    public UndoableEdit validate2Manifold()
    {
        return new Validate2Manifold( mSelection, mRealized );
    }
    
    private final RealizedModel mRealized;

    protected Selection mSelection;

	private SelectionSummary selectionSummary;

    private Point mCenterPoint;
    
    private Segment mSymmetryAxis;

	private SymmetrySystem symmetrySystem;

    private final boolean oldGroups;

    public Construction getSelectedConstruction( Class<? extends Construction > kind )
    {
        Class<? extends Manifestation> manifestationClass;
        if ( kind == Point .class )
            manifestationClass = Connector.class;
        else if ( kind == Segment .class )
            manifestationClass = Strut .class;
        else
            return null;
        Manifestation focus = mSelection .getSingleSelection( manifestationClass );
        if ( focus != null )
            return focus .getConstructions() .next();
        return null;
    }

    
    public UndoableEdit setSymmetryCenter( Construction target ) throws Command.Failure
    {
        Point newCenter = null;
        if ( target instanceof Point )
            newCenter = (Point) target;
        else if ( target != null )
            throw new Command.Failure( "Target is not a single ball." );
        if ( newCenter == null ) {
            newCenter = (Point) getSelectedConstruction( Point.class );
            if ( newCenter == null )
                throw new Command.Failure( "Selection is not a single ball." );
        }
        if ( newCenter .getLocation() .equals( mCenterPoint .getLocation() ) )
            return null;
        return new SymmetryCenterChange( this, newCenter );
    }
    
    public UndoableEdit setSymmetryAxis( Construction target ) throws Command.Failure
    {
        Segment newAxis = null;
        if ( target instanceof Segment )
            newAxis = (Segment) target;
        else if ( target != null )
            throw new Command.Failure( "Target is not a single strut." );
        if ( newAxis == null ) {
            newAxis = (Segment) getSelectedConstruction( Segment.class );
            if ( newAxis == null )
                throw new Command.Failure( "Selection is not a single strut." );
        }
        if ( ( mSymmetryAxis != null )
           && newAxis .getStart() .equals( mSymmetryAxis .getStart() )
           && newAxis .getEnd() .equals( mSymmetryAxis .getEnd() ) )
                return null;
        return new SymmetryAxisChange( this, newAxis );
    }

    public UndoableEdit groupSelection()
    {
        if ( !oldGroups && mSelection .isSelectionAGroup() )
            return new NoOp();
        else
            return new GroupSelection( mSelection, true );
    }

    public UndoableEdit ungroupSelection()
    {
        if ( oldGroups || mSelection .isSelectionAGroup() )
            return new GroupSelection( mSelection, false );
        else
            return new NoOp();
    }
    
    private final Set<Manifestation> failedConstructions = new HashSet<>();

    public void addFailedConstruction( Construction cons )
    {
        failedConstructions .add( mRealized .manifest( cons ) );
    }

    public boolean hasFailedConstruction( Construction cons )
    {
        return failedConstructions .contains( mRealized .manifest( cons ) );
    }

	public Selection getSelection()
	{
		return this .mSelection;
	}

	public void notifyListeners()
	{
		this .selectionSummary .notifyListeners();
	}

	public SymmetrySystem getSymmetrySystem()
	{
		return this .symmetrySystem;
	}

	public void setSymmetrySystem( SymmetrySystem system )
	{
		this .symmetrySystem = system;
	}
}
