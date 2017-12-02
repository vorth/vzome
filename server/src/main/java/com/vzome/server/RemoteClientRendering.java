package com.vzome.server;

import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;

import org.eclipse.jetty.websocket.api.Session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vzome.core.math.RealVector;
import com.vzome.core.model.Manifestation;
import com.vzome.core.model.Strut;
import com.vzome.core.render.Color;
import com.vzome.core.render.RenderedManifestation;
import com.vzome.core.render.RenderingChanges;
import com.vzome.desktop.controller.RenderingViewer;

class RemoteClientRendering implements RenderingChanges, RenderingViewer, PropertyChangeListener
{
	private final Session session;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final ThrottledQueue queue = new ThrottledQueue( 20, 1000 ); // 25 gets/second

	public RemoteClientRendering( Session session, String bkgdColor )
	{
		this .session = session;
		if ( bkgdColor != null ) {
			int rgb =  Integer .parseInt( bkgdColor, 16 );
			String color = new Color( rgb ) .toWebString();
			this .session .getRemote() .sendString( "{ \"render\": \"background\", \"color\": \"" + color + "\" }", null );
		}
		
		Thread consumer = new Thread( new Runnable() {
			@Override
			public void run() {
				while (true)
					if ( ! queue .isEmpty()) {
						session .getRemote() .sendString( (String) queue .get(), null );
					}
			}
		} );

        consumer.start();
	}

	@Override
	public void setEye( int eye ) {}

	@Override
	public void setViewTransformation( Matrix4d trans, int eye ) {}

	@Override
	public void setPerspective( double fov, double aspectRatio, double near, double far ) {}

	@Override
	public void setOrthographic( double halfEdge, double near, double far ) {}

	@Override
	public RenderedManifestation pickManifestation( MouseEvent e )
	{
		return null;
	}

	@Override
	public Collection<RenderedManifestation> pickCube()
	{
		return null;
	}

	@Override
	public void pickPoint(MouseEvent e, Point3d imagePt, Point3d eyePt) {}

	@Override
	public RenderingChanges getRenderingChanges()
	{
		return this;
	}

	@Override
	public void captureImage(int maxSize, ImageCapture capture) {}

	@Override
	public void reset() {}

	@Override
	public void manifestationAdded( RenderedManifestation rm )
	{
		Manifestation man = rm .getManifestation();
		if ( man instanceof Strut ) {
			Strut strut = (Strut) man;
			RealVector start = strut .getLocation() .toRealVector();
			RealVector end = strut .getEnd() .toRealVector();
			try {
				String startJson = this .objectMapper .writeValueAsString( start );
				String endJson = this .objectMapper .writeValueAsString( end );
				String color = rm .getColor() .toWebString();
				this .queue .add( "{ \"render\": \"segment\", \"start\": " + startJson + ", \"end\": " + endJson  + ", \"color\": \"" + color + "\" }" );
			} catch ( JsonProcessingException e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public void manifestationRemoved(RenderedManifestation manifestation) {}

	@Override
	public void manifestationSwitched(RenderedManifestation from, RenderedManifestation to) {}

	@Override
	public void glowChanged(RenderedManifestation manifestation) {}

	@Override
	public void colorChanged(RenderedManifestation manifestation) {}

	@Override
	public void locationChanged(RenderedManifestation manifestation) {}

	@Override
	public void orientationChanged(RenderedManifestation manifestation) {}

	@Override
	public void shapeChanged(RenderedManifestation manifestation) {}

	@Override
	public void propertyChange( PropertyChangeEvent chg ) {}
}