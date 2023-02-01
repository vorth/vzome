

package com.vzome.core.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.construction.Color;
import com.vzome.core.construction.Construction;
import com.vzome.core.construction.Point;
import com.vzome.core.construction.Polygon;
import com.vzome.core.construction.Segment;
import com.vzome.core.math.Projection;

/**
 * @author Scott Vorthmann
 */
public class RealizedModelImpl implements RealizedModel
{
    private final List<ManifestationChanges> mListeners = new ArrayList<>( 1 );

    private final HashMap<String, Manifestation> mManifestations = new LinkedHashMap<>( 1000 );
    
    private Projection mProjection;

	private final AlgebraicField field;

    public RealizedModelImpl( AlgebraicField field, Projection projection )
    {
        super();
		this.field = field;
        mProjection = projection;
    }
    
    public Set<Manifestation> moreVisibleThan( RealizedModelImpl other )
    {
        Set<Manifestation> result = new HashSet<>();
        for (Manifestation man : mManifestations .values()) {
            if ( man .isHidden() )
                continue;
            Manifestation doppel = other .mManifestations .get( man .toConstruction() .getSignature() );
            if ( doppel == null || doppel .isHidden() )
                result .add( man );
        }
        return result;
    }

    public void addListener( ManifestationChanges l )
    {
        mListeners .add( l );
    }

    public void removeListener( ManifestationChanges l )
    {
        mListeners .remove( l );
    }

	@Override
	public Iterator<Manifestation> iterator()
	{
        return mManifestations .values() .iterator();
	}

    public Manifestation manifest( Construction c )
    {
        Manifestation m = null;
        if ( c instanceof Point )
        {
            Point p = (Point) c;
            m = new ConnectorImpl( mProjection .projectImage( p .getLocation(), true ) );
        }
        else if ( c instanceof Segment )
        {
            Segment s = (Segment) c;
            AlgebraicVector start = mProjection .projectImage( s .getStart(), true );
            AlgebraicVector end = mProjection .projectImage( s .getEnd(), true );
            if ( ! start .equals( end ) )
            {
                m = new StrutImpl( start, end );
            }
        }
        else if ( c instanceof Polygon )
        {
            Polygon p = (Polygon) c;
            List<AlgebraicVector> vertices = new ArrayList<>();
            for (int i = 0; i < p.getVertexCount(); i++) {
                vertices .add( mProjection .projectImage( p.getVertex( i ), true ) );
            }
            m = new PanelImpl( vertices );
        }
        return m;
    }
    
    private static final Logger logger = Logger .getLogger( "com.vzome.core.model" );
    
    @Override
    public void add( Manifestation m )
    {
        String key = m .toConstruction() .getSignature();
        mManifestations .put( key, m );
        if ( logger .isLoggable( Level .FINER ) )
            logger .finer( "add manifestation: " + m .toString() );
    }
    
    @Override
    public void remove( Manifestation m )
    {
        String key = m .toConstruction() .getSignature();
        mManifestations .remove( key );
        if ( logger .isLoggable( Level .FINER ) )
            logger .finer( "remove manifestation: " + m .toString() );
    }
    
    public void refresh( boolean on, RealizedModelImpl unused )
    {
        for (Manifestation man : mManifestations .values()) {
            if ( ! man .isHidden() )
            {
                if ( on )
                    show( man );
                else
                    hide( man );
            }
        }
    }
    
    /*
     * idempotent: show,show is the same as show
     */
    @Override
    public void show( Manifestation m )
    {
        if ( doingBatch )
        {
            if ( removals .contains( m ) )
                removals .remove( m );
            else
                additions .add( m );
        }
        else
            privateShow( m );
    }

    private void privateShow( Manifestation m )
    {
        if ( ! m .isRendered() ) {
            for (ManifestationChanges next : mListeners) {
                next .manifestationAdded( m );
                // one side-effect will be to set the rendered object
            }
        }
    }

    /*
     * idempotent: hide,hide is the same as hide
     */
    @Override
    public void hide( Manifestation m )
    {
        if ( doingBatch )
        {
            if ( additions .contains( m ) )
                additions .remove( m );
            else
                removals .add( m );
        }
        else
            privateHide( m );
    }
    
    private void privateHide( Manifestation m )
    {
        if ( m .isRendered() ) {
            for (ManifestationChanges next : mListeners) {
                next .manifestationRemoved( m );
            }
        }
    }
    
    @Override
    public void setColor( Manifestation m, Color color )
    {
        m .setColor( color );
        if ( m .isRendered() ) {
            for (ManifestationChanges next : mListeners) {
                next .manifestationColored( m, color );
            }
        }
    }
    
    
    @Override
    public Manifestation findConstruction( Construction c )
    {
        Manifestation actualMan = mManifestations .get( c .getSignature() );
        if ( actualMan == null )
            actualMan = manifest( c );
        
        return actualMan;
    }
    
    @Override
    public Manifestation removeConstruction( Construction c )
    {
        Manifestation actualMan = mManifestations .get( c .getSignature() );
        if ( actualMan == null )
            return null;
        // This is just bizarre, but it matches the old logic!
        return manifest( c );
    }

    /**
     * @param c
     * @return
     */
    @Override
    public Manifestation getManifestation( Construction c )
    {
        return mManifestations .get( c .getSignature() );
    }

	@Override
	public int size()
	{
		return mManifestations .size();
	}
    
    @Override
    public boolean equals( Object object )
    {
        if ( object == null ) {
            return false;
        }
        if ( object == this ) {
            return true;
        }
        if ( ! ( object instanceof RealizedModelImpl ) )
            return false;
        
        RealizedModelImpl that = (RealizedModelImpl) object;

        if ( this.size() != that.size() )
            return false;
        for (Manifestation man : mManifestations.values() ) {
            if ( ! that .mManifestations .values() .contains( man ) ) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode()
    {
        return size();
    }
    
    private boolean doingBatch = false;
    
    private final Set<Manifestation> additions = new HashSet<>();
    
    private final Set<Manifestation> removals = new HashSet<>();

    public void startBatch()
    {
        additions .clear();
        removals .clear();
        doingBatch = true;
    }

    public void endBatch()
    {
        for (Manifestation m : removals) {
            privateHide( m );
        }
        for (Manifestation m : additions) {
            privateShow( m );
        }
        additions .clear();
        removals .clear();
        this .doingBatch = false;
    }

	@Override
	public AlgebraicField getField()
	{
		return field;
	}

    /**
     * This records the NEW manifestations produced by manifestConstruction for this edit,
     * to avoid creating colliding manifestations.
     */
    private transient Map<String, Manifestation> mManifestedNow;  // used only while calling manifest

    @Override
    public Manifestation findPerEditManifestation( String signature )
    {
        return this .mManifestedNow .get( signature );
    }

    @Override
    public void addPerEditManifestation( String signature, Manifestation m )
    {
        this .mManifestedNow .put( signature, m );
    }

    @Override
    public void clearPerEditManifestations()
    {
        mManifestedNow = new HashMap<>();
    }
}
