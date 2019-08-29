package com.vzome.core.teavm;

import java.util.Iterator;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.construction.Construction;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.RealizedModel;
import com.vzome.core.render.Color;

public class RealizedModelBinding implements RealizedModel
{
    @JSBody( params = { "message" }, script = "console.log(message)")
    public static native void log( String message );
    
    @Override
    public Iterator<Manifestation> iterator()
    {
        log( "RealizedModelBinding.iterator()" );
        return null;
    }

    @Override
    public AlgebraicField getField()
    {
        log( "RealizedModelBinding.getField()" );
        return null;
    }

    @Override
    public Manifestation findConstruction(Construction c)
    {
        log( "RealizedModelBinding.findConstruction()" );
        return null;
    }

    @Override
    public Manifestation removeConstruction(Construction c)
    {
        log( "RealizedModelBinding.removeConstruction()" );
        return null;
    }

    @Override
    public Manifestation getManifestation(Construction c)
    {
        log( "RealizedModelBinding.getManifestation()" );
        return null;
    }

    @Override
    public int size()
    {
        log( "RealizedModelBinding.size()" );
        return 0;
    }

    @Override
    public void show(Manifestation mManifestation)
    {
        log( "RealizedModelBinding.show()" );
    }

    @Override
    public void hide(Manifestation mManifestation)
    {
        log( "RealizedModelBinding.hide()" );
    }

    @Override
    public void add(Manifestation m)
    {
        log( "RealizedModelBinding.add()" );
    }

    @Override
    public void remove(Manifestation mManifestation)
    {
        log( "RealizedModelBinding.remove()" );
    }

    @Override
    public void setColor(Manifestation manifestation, Color color)
    {
        log( "RealizedModelBinding.setColor()" );
    }

}
