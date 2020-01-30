package org.vorthmann.zome.render.jogl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.math.FloatUtil;
import com.vzome.opengl.OpenGlShim;

import java.nio.FloatBuffer;

/**
 * Created by vorth on 2019-12-26, initially copying from
 * vzome-cardboard AndroidOpenGlShim.  That class, and everything
 * in com.vzome.opengl, is clearly set up for modern OpenGL,
 * requiring VBOs and custom shaders.  JOGL may not be well-prepared
 * for this, being a bit outdated now.
 * 
 * See https://www.khronos.org/opengl/wiki/Legacy_OpenGL.
 */
public class JoglOpenGlShim implements OpenGlShim
{
    private final GL2 gl2;
    
    public boolean isSameContext( GL2 gl2 )
    {
        return this.gl2 == gl2;
    }

    public JoglOpenGlShim( GL2 gl2 )
    {
        this.gl2 = gl2;
    }

    public void glViewport( int x, int y, int width, int height ) {
        gl2.glViewport( x, y, width, height );
    }

    @Override
    public int glCreateProgram() {
        return gl2.glCreateProgram();
    }

    @Override
    public void glAttachShader(int i, int i2) {
        gl2.glAttachShader( i, i2 );
    }

    @Override
    public void glLinkProgram(int i) {
        gl2.glLinkProgram(i);
    }

    @Override
    public int glCreateVertexShader() {
        return gl2.glCreateShader( GL2ES2.GL_VERTEX_SHADER );
    }

    @Override
    public int glCreateFragmentShader() {
        return gl2.glCreateShader( GL2ES2.GL_FRAGMENT_SHADER );
    }

    @Override
    public void glShaderSource( int i, String s ) {
        String[] vlines = new String[] { s };
        int[] vlengths = new int[] { vlines[0].length() };
        gl2 .glShaderSource( i, vlines.length, vlines, vlengths, 0 );
    }

    @Override
    public void glCompileShader(int i) {
        gl2.glCompileShader(i);
    }

    @Override
    public void glGetShaderStatus(int i, int[] ints, int i2) {
        gl2.glGetShaderiv(i, GL2ES2.GL_COMPILE_STATUS, ints, i2);
    }

    @Override
    public String glGetShaderInfoLog(int i)
    {
        int[] logLength = new int[1];
        gl2 .glGetShaderiv( i, GL2ES2.GL_INFO_LOG_LENGTH, logLength, 0 );

        byte[] log = new byte[logLength[0]];
        gl2 .glGetShaderInfoLog( i, logLength[0], (int[])null, 0, log, 0 );
        return new String( log );
    }

    @Override
    public void glDeleteShader(int i) {
        gl2.glDeleteShader( i );
    }

    @Override
    public int glGetError() {
        return gl2.glGetError();
    }

    @Override
    public int glGetUniformLocation(int i, String s) {
        return gl2.glGetUniformLocation( i, s );
    }

    @Override
    public int glGetAttribLocation(int i, String s) {
        return gl2.glGetAttribLocation( i, s );
    }

    @Override
    public void glUseProgram(int i) {
        gl2.glUseProgram( i );
    }

    @Override
    public void glUniformMatrix4fv(int i, int i2, boolean b, float[] floats, int i3) {
        gl2.glUniformMatrix4fv( i, i2, b, floats, i3 );
    }

    @Override
    public void glUniform3f(int i, float v, float v2, float v3) {
        gl2.glUniform3f( i, v, v2, v3 );
    }

    @Override
    public void glUniform1i( int i, int v ) {
        gl2.glUniform1i( i, v );
    }

    @Override
    public void glUniform4f(int i, float v, float v2, float v3, float v4) {
        gl2.glUniform4f( i, v, v2, v3, v4 );
    }

    @Override
    public void glEnableVertexAttribArray(int i) {
        gl2.glEnableVertexAttribArray( i );
    }

    @Override
    public void glBindBuffer( int i) {
        gl2.glBindBuffer( GL.GL_ARRAY_BUFFER, i );
    }

    @Override
    public void glVertexAttribDivisor(int i, int i2) {
        gl2.glVertexAttribDivisor( i, i2 );
    }

    @Override
    public void glVertexAttribPointer(int i, int i2, boolean b, int i3, int i4) {
        gl2.glVertexAttribPointer( i, i2, GL.GL_FLOAT, b, i3, i4 );
    }

    @Override
    public void glVertexAttribPointer(int i, int i2, boolean b, int i3, FloatBuffer floatBuffer) {
        gl2.glVertexAttribPointer( i, i2, GL.GL_FLOAT, b, i3, floatBuffer );
    }

    @Override
    public void glDrawTrianglesInstanced( int i, int i2, int i3) {
        gl2.glDrawArraysInstanced( GL.GL_TRIANGLES, i, i2, i3 );
    }

    @Override
    public void glLineWidth( float width ) {
        gl2.glLineWidth( width );
    }

    @Override
    public void glDrawLinesInstanced( int i, int i2, int i3) {
        gl2.glDrawArraysInstanced( GL.GL_LINES, i, i2, i3 );
    }

    @Override
    public void glDrawTriangles( int i, int i2) {
        gl2.glDrawArrays( GL.GL_TRIANGLES, i, i2 );
    }

    @Override
    public void glDrawLines( int i, int i2) {
        gl2.glDrawArrays( GL.GL_LINES, i, i2 );
    }

    @Override
    public void glGenBuffers(int i, int[] ints, int i2) {
        gl2.glGenBuffers( i, ints, i2 );
    }

    @Override
    public void glBufferData( int i, FloatBuffer floatBuffer ) {
        gl2.glBufferData( GL.GL_ARRAY_BUFFER, i, floatBuffer, GL.GL_STATIC_DRAW );
    }

    @Override
    public void multiplyMM(float[] floats, float[] floats2, float[] floats3) {
        FloatUtil.multMatrix( floats, floats2, floats3 );
    }

    @Override
    public void invertM(float[] floats, float[] floats2) {
//        Matrix.invertM( floats, 0, floats2, 0 );
    }

    @Override
    public void transposeM(float[] floats, float[] floats2) {
//        Matrix.transposeM( floats, 0, floats2, 0 );
    }

    @Override
    public void multiplyMV(float[] floats, float[] floats2, float[] floats3 ) {
        FloatUtil.multMatrixVec( floats, floats2, floats3 );
    }

    @Override
    public String getGLSLVersionString()
    {
        return gl2 .getContext() .getGLSLVersionString();
    }

    @Override
    public void glEnableDepth()
    {
        gl2 .glEnable( GL2.GL_DEPTH_TEST );
    }

    @Override
    public void glClear( float r, float g, float b, float alpha )
    {
        gl2 .glClearColor( r, g, b, alpha );
        gl2 .glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT );
    }

    @Override
    public void glPolygonOffset( float f, float g )
    {
        gl2 .glEnable( GL2 .GL_POLYGON_OFFSET_FILL );
        gl2 .glPolygonOffset( f, g );
    }

    @Override
    public void glDeleteBuffer( int oldId )
    {
        gl2 .glDeleteBuffers( 1, new int[] { oldId }, 0 );
    }

    @Override
    public void glUniform1f( int param, float f )
    {
        gl2 .glUniform1f( param, f );
    }
}
