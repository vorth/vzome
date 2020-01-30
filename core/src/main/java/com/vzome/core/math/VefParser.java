
//(c) Copyright 2005, Scott Vorthmann.  All rights reserved.

package com.vzome.core.math;

import java.math.BigInteger;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.StringTokenizer;

import com.vzome.core.algebra.AlgebraicField;
import com.vzome.core.algebra.AlgebraicFields;
import com.vzome.core.algebra.AlgebraicNumber;
import com.vzome.core.algebra.AlgebraicVector;
import com.vzome.core.algebra.BigRational;

public abstract class VefParser
{
    
    // Version 10 supports:
    // An offset vector may be specified which will be added to each vertex after it is scaled.
    // If not specified, nothing is added.
    public static final int VERSION_EXPLICIT_OFFSET = 10;
    
    // Version 9 supports:
    // The number of dimensions may be specified for parsing AlgebraicVectors. 
    // If not specified, use 4D for backward compatibility. 
    public static final int VERSION_EXPLICIT_DIMENSION = 9;
    
    // Version 8 supports:
    // Scale can optionally be represented as an AlgebraicVector with separate scale per coordinate
    // or as an AlgebraicNumber with the same scale for all coordinates (original behavior)
    public static final int VERSION_SCALE_VECTOR = 8;

    // Version 7 supports:
    // New "rational" field which requires only rational numbers as input and matches any actual field
    // Mix of rational and/or irrational numeric format for parsing scale and vertices
    // Factors not specified in the VEF string will be set to 0 so they don't all have to be listed.
    // Flag telling subclasses to use the specified scale and not introduce additional scaling after parsing.
    public static final int VERSION_RATIONAL_ACTUAL_SCALE = 7;

    public static final int VERSION_EXPLICIT_BALLS = 6;
    
    public static final int VERSION_ANY_FIELD = 5;

    public static final int VERSION_W_FIRST = 4;
    
    private int mVersion = 0;
    
    private int dimension = 4;
    
    private boolean isRational = false;

    private transient AlgebraicField field;
    
    protected AlgebraicVector parsedOffset;

    protected abstract void startVertices( int numVertices );
    
    protected abstract void addVertex( int index, AlgebraicVector location );
    
    protected void endVertices() {};

    protected abstract void startEdges( int numEdges );
    
    protected abstract void addEdge( int index, int v1, int v2 );
    
    protected void endEdges() {};

    protected abstract void startFaces( int numFaces );
    
    protected abstract void addFace( int index, int[] verts );
    
    protected void endFaces() {};
    
    protected abstract void startBalls( int numVertices );
    
    protected abstract void addBall( int index, int vertex );
    
    protected void endBalls() {};

    protected int getVersion()
    {
        return mVersion;
    }
    
    protected AlgebraicField getField()
    {
        return this .field;
    }
    
    protected boolean isRational()
    {
        return isRational;
    }

    protected boolean wFirst()
    {
        return mVersion >= VERSION_W_FIRST;
    }

    public void parseVEF( String vefData, final AlgebraicField field )
    {
        this.field = field;
        StringTokenizer tokens = new StringTokenizer( vefData );
        String token = null;
        try {
            token = tokens .nextToken();
        } catch ( NoSuchElementException e1 ) {
            throw new IllegalStateException( "VEF format error: no tokens in file data: \"" + vefData + "\"" );
        }
        mVersion = 0;
        isRational = false;

        if ( token .equals( "vZome" ) ) {
            // format 4, with version number
            try {
                token = tokens .nextToken(); // skip "VEF"
            } catch ( NoSuchElementException e1 ) {
                throw new IllegalStateException( "VEF format error: no tokens after \"vZome\"" );
            }
            if ( ! "VEF" .equals( token ) )
                throw new IllegalStateException( "VEF format error: token after \"vZome\" (\"" + token + "\" should be \"VEF\"" );
            try {
                token = tokens .nextToken();
            } catch ( NoSuchElementException e1 ) {
                throw new IllegalStateException( "VEF format error: no tokens after \"VEF\"" );
            }
            try {
                mVersion = Integer .parseInt( token );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: VEF version number (\"" + token + "\") must be an integer", e );
            }
            token = tokens .nextToken();
        }
        if ( token .equals( "field" ) ) {
            // format > 5, with field name
            try {
                token = tokens .nextToken();
            } catch ( NoSuchElementException e1 ) {
                throw new IllegalStateException( "VEF format error: no tokens after \"field\"" );
            }
            // format >= 7 allows "rational" to match any field since it is a subset of every other field
            if ( token .equals( "rational" ) ) {
                isRational = true;
                token = field .getName();
            }
            if(! token .equals(field .getName()) ) {
                if (! AlgebraicFields.haveSameInitialCoefficients(field, token) ) {
                    throw new IllegalStateException( "VEF field mismatch error: VEF field name (\"" + token + "\") does not match current model field name (\"" + field .getName() + "\")." );
                }
            }
            token = tokens .nextToken();
        }

        // format >= 7 allowed disabling subsequent scaling with keyword "actual",
        //  but this is now disabled to allow the user to have control.
        if ( token .equals( "actual" ) ) {
            try {
                token = tokens .nextToken();
            } catch ( NoSuchElementException e1 ) {
                throw new IllegalStateException( "VEF format error: no tokens after \"actual\"" );
            }
        }
        
        // format >= 9 allows the number of dimensions to be specified for parsing AlgebraicVectors 
        if ( token .equals( "dimension" ) ) {
            try {
                token = tokens .nextToken();
            } catch ( NoSuchElementException e1 ) {
                throw new IllegalStateException( "VEF format error: no tokens after \"dimension\"" );
            }
            try {
                this.dimension = Integer .parseInt( token );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: dimension number (\"" + token + "\") must be an integer", e );
            }
            try {
                token = tokens.nextToken();
            } catch (NoSuchElementException e) {
                throw new IllegalStateException( "VEF format error: no tokens after \"dimension\"" );
            }
        }
        
        AlgebraicVector scaleVector = new AlgebraicVector( field, this.dimension );
        if ( token .equals( "scale" ) ) {
            try {
                token = tokens .nextToken();
                // format >= 8 allows discreet scaling per coordinate with keyword "vector"
                if ( token.equals("vector") ) {
                    try {
                        for (int tokNum = 0; tokNum < this.dimension; tokNum++) {
                            token = tokens.nextToken();
                            AlgebraicNumber coord = parseIntegralNumber(token);
                            scaleVector.setComponent(tokNum, coord); // format is W X Y Z
                        }
                    } catch (NoSuchElementException e) {
                        throw new IllegalStateException("VEF format error: scale vector requires " + this.dimension + " coordinates");
                    }
                }
                else {
                    AlgebraicNumber scale = parseIntegralNumber( token );
                    for (int i = 0; i < this.dimension; i++) {
                        scaleVector.setComponent(i, scale);
                    }
                }
                token = tokens.nextToken();
            } catch (NoSuchElementException e) {
                throw new IllegalStateException( "VEF format error: no tokens after \"scale\"" );
            }
        }
        else {
            for (int i = 0; i < this.dimension; i++) {
                scaleVector.setComponent(i, field.one());
            }
        }
        
        parsedOffset = new AlgebraicVector( field, this.dimension );
        if ( token .equals( "offset" ) ) {
            // format >= 10 allows an offset to be specified which will be added to each vertex after it is scaled
            try {
                for (int tokNum = 0; tokNum < this.dimension; tokNum++) {
                    token = tokens.nextToken();
                    AlgebraicNumber coord = parseIntegralNumber(token);
                    parsedOffset.setComponent(tokNum, coord); // format is W X Y Z
                }
            } catch (NoSuchElementException e) {
                throw new IllegalStateException("VEF format error: offset vector requires " + this.dimension + " coordinates");
            }
            try {
                token = tokens.nextToken();
            } catch (NoSuchElementException e) {
                throw new IllegalStateException( "VEF format error: no tokens after \"offset\"" );
            }
        }
        
        int numVertices;
        try {
            numVertices = Integer .parseInt( token );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "VEF format error: number of vertices (\"" + token + "\") must be an integer", e );
        }
        startVertices( numVertices );
        // testing hasOffset is more efficent in a loop 
        // than using !parsedOffset.isOrigin() which re-tests all coordinates in each iteration
        final boolean hasOffset = !parsedOffset.isOrigin();
        for ( int i = 0 ; i < numVertices; i++ )
        {
            AlgebraicVector v = field .origin( this.dimension );
            for ( int tokNum = 0; tokNum < this.dimension; tokNum ++ ) {
                try {
                    token = tokens .nextToken();
                } catch ( NoSuchElementException e1 ) {
                    throw new IllegalStateException( "VEF format error: not enough vertices in list" );
                }
                AlgebraicNumber coord = parseIntegralNumber( token ) .times( scaleVector.getComponent(tokNum) );
                
                // I think this is the right way to deal with VERSION_W_FIRST and the concomitant
                //  "incorrect" quaternion multiplication used for projection.  All that is necessary
                //  is to remember that it is VERSION_W_FIRST, and project by dropping Z instead of W.
                //
//                if ( mVersion < VERSION_W_FIRST )
//                    field .setVectorComponent( v, (tokNum+1)%4, coord ); // format is X Y Z W
//                else
                    v .setComponent( tokNum, coord ); // format is W X Y Z
            }
            if(hasOffset) {
                v = v.plus(parsedOffset);
            }
            addVertex( i, v );
        }
        endVertices();

        if ( tokens .hasMoreTokens() )
        {
            token = tokens .nextToken();
            int numEdges;
            try {
                numEdges = Integer .parseInt( token );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: number of edges (\"" + token + "\") must be an integer", e );
            }
            startEdges( numEdges );
            for ( int i = 0; i < numEdges; i++ ){
                try {
                    token = tokens .nextToken();
                } catch ( NoSuchElementException e1 ) {
                    throw new IllegalStateException( "VEF format error: not enough edges in list" );
                }
                int v1 = Integer .parseInt( token );
                try {
                    token = tokens .nextToken();
                } catch ( NoSuchElementException e1 ) {
                    throw new IllegalStateException( "VEF format error: 2nd vertex index of last edge is missing" );
                }
                int v2 = Integer .parseInt( token );
                addEdge( i, v1, v2 );
            }
            endEdges();
        }
        
        if ( tokens .hasMoreTokens() ) {
            token = tokens .nextToken();
            int numFaces;
            try {
                numFaces = Integer .parseInt( token );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: number of faces (\"" + token + "\") must be an integer", e );
            }
            startFaces( numFaces );
            for ( int i = 0; i < numFaces; i++ ){
                try {
                    token = tokens .nextToken();
                } catch ( NoSuchElementException e1 ) {
                    throw new IllegalStateException( "VEF format error: not enough faces in list" );
                }
                int order = Integer .parseInt( token );
                int[] verts = new int[order];
                for ( int j = 0; j < order; j++ ) {
                    try {
                        token = tokens .nextToken();
                    } catch ( NoSuchElementException e1 ) {
                        throw new IllegalStateException( "VEF format error: not enough vertices in last face" );
                    }
                    verts[j] = Integer .parseInt( token );
                }
                addFace( i, verts );
            }
            endFaces();
        }

        if ( tokens .hasMoreTokens() ) {
            token = tokens .nextToken();
            int numBalls;
            try {
                numBalls = Integer .parseInt( token );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: number of balls (\"" + token + "\") must be an integer", e );
            }
            startBalls( numBalls );
            for ( int i = 0; i < numBalls; i++ ){
                try {
                    token = tokens .nextToken();
                } catch ( NoSuchElementException e1 ) {
                    throw new IllegalStateException( "VEF format error: not enough balls in list" );
                }
                int v1 = Integer .parseInt( token );
                addBall( i, v1 );
            }
            endBalls();
        }

        endFile( tokens );
    }
    
    protected void endFile( StringTokenizer tokens )
    {
    }
    
    private AlgebraicNumber parseIntegralNumber( String string )
    {
        BigRational[] factors = new BigRational[this.field.getOrder()];
        // if the field is declared as rational, then we won't allow the irrational syntax using parenthesis
        // if the field is NOT declared as rational, then we will still allow the rational format as shorthand with no parenthesis
        // or we will allow any order N string representation where N <= field.getOrder().
        if( (!isRational) &&  string.startsWith("(") && string.endsWith(")") ) {
            // strip "(" and ")", tokenize on ","
            StringTokenizer tokens = new StringTokenizer(string.substring(1, string.length() - 1), ",");
            // The tokens get pushed into the factors array in reverse order 
            // from the string representation so the last token becomes the 0th factor.
            // For example, with an order 2 field, the factors for "(3,-2)" are parsed to a 2 element array as {-2, 3}
            // With an order 6 field such as the snubDodec, the factors for "(0,0,0,0,3,-2)" are parsed to a 6 element array: {-2, 3, 0, 0, 0, 0}
            // When a field needs more factors than are supplied, the factors that are provided must still be parsed into the begining of the array:
            // With an order 6 field, if only 2 factors are provided, "(3,-2)" must still be parsed into a 6 element array as {-2, 3, 0, 0, 0, 0}
            // Since VEF version 7 no longer requires that all factors be provided, we need to push the factors onto a stack
            // and pop them off to reverse the order as they are inserted into the begining of the factors array.
            Stack<BigRational> stack = new Stack<>();
            while(tokens.hasMoreElements()) {
                if(stack.size() >= field.getOrder()) {
                    throw new RuntimeException( "VEF format error: \"" + string + "\" has too many factors for " + field.getName() + " field" );
                }
                stack.push( parseRationalNumber( tokens.nextToken() ) );
            }
            int i = 0;
            while(! stack.empty() ) {
               factors[i++] = stack.pop();
            }
            return this.field.createAlgebraicNumber(factors);
        } else {
            // format >= 7 supports the rational numeric format which expects no irrational factors,
            // so there are no parentheses or commas, but still allows the optional "/" if a denominator is specified.
            factors[0] = parseRationalNumber( string );
            // count on createAlgebraicNumber to set all of the null irrational factors to zero
        }
        return this.field.createAlgebraicNumber(factors);
    }

    private BigRational parseRationalNumber( String coord )
    {
        BigInteger num = BigInteger.ZERO;
        BigInteger denom = BigInteger.ONE;
        int slash = coord .indexOf( '/' );
        if ( slash > 0 ) {
            try {
                num = new BigInteger( coord .substring( 0, slash ) );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: rational numerator (\"" + coord + "\") must be an integer", e );
            }
            try {
                denom = new BigInteger( coord .substring( slash+1 ) );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: rational denominator (\"" + coord + "\") must be an integer", e );
            }
        } else {
            try {
                num = new BigInteger( coord );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "VEF format error: coordinate value (\"" + coord + "\") must be an integer or rational", e );
            }
        }
        return new BigRational( num, denom );
    }
}
