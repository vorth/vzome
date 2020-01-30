package com.vzome.core.editor;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.algebra.PentagonField;
import com.vzome.core.commands.Command.Failure;
import com.vzome.core.edits.Delete;
import com.vzome.core.math.Projection;
import com.vzome.core.model.Connector;
import com.vzome.core.model.RealizedModel;

public class DeleteTest {

	@Test
	public void testPerform()
	{
		AlgebraicField field = new PentagonField();
		Selection selection = new Selection();
		RealizedModel realized = new RealizedModel( field, new Projection .Default( field ) );
		assertEquals( 0, realized .size() );

		AlgebraicVector loc = field .basisVector( 3, 2 );
		Connector ball = new Connector( loc );
		realized .add( ball );
		selection .select( ball );
		assertEquals( 1, realized .size() );
		assertFalse( selection .isEmpty() );
		
		Delete cmd = new Delete( selection, realized );
		try {
			cmd .perform();
		} catch ( Failure e ) {
			fail( "Delete perform failed" );
		}
		assertEquals( 0, realized .size() );
		assertTrue( selection .isEmpty() );
	}
	
	@Test
	public void testEmpty()
	{
		AlgebraicField field = new PentagonField();
		Selection selection = new Selection();
		RealizedModel realized = new RealizedModel( field, new Projection .Default( field ) );
		assertEquals( 0, realized .size() );
		assertTrue( selection .isEmpty() );

		AlgebraicVector loc = field .basisVector( 3, 2 );
		Connector ball = new Connector( loc );
		realized .add( ball );
		assertEquals( 1, realized .size() );
		assertTrue( selection .isEmpty() );
		
		Delete cmd = new Delete( selection, realized );
		try {
			cmd .perform();
		} catch ( Failure e ) {
			fail( "Delete perform failed" );
		}
		assertEquals( 1, realized .size() );
		assertTrue( selection .isEmpty() );
	}
}
