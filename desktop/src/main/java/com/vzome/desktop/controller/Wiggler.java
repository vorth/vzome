package com.vzome.desktop.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Function;

import javax.swing.Timer;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

public class Wiggler implements ActionListener
{    
    private Vector3d staticLookDir, staticUpDir;
    private Vector3f newUp, xAxis, yAxis;
    private Timer timer;
    private Runnable nextStep;
    private Function<Float, Float> interpolation;
    private final int iterations = 30;
    private int iteration = 0;
    private CameraController cameraController;
    
    public void start( CameraController cameraController, float radius )
    {
        this .cameraController = cameraController;
        this .staticLookDir = new Vector3d();
        this .staticUpDir = new Vector3d();
        cameraController .getViewOrientation( staticLookDir, staticUpDir );
        
        this .newUp = new Vector3f( staticUpDir );

        this .xAxis = new Vector3f();
        this .xAxis .cross( new Vector3f( staticLookDir ), new Vector3f( staticUpDir ) );
        
        this .yAxis = new Vector3f( staticUpDir );
        
        // We will build a circle in the plane spanned by crossDir and upDir
        this .xAxis .scale( radius );
        this .yAxis .scale( radius );
        
        timer = new Timer( 20, this );        
        spinUp(); 
        timer .start();
    }
    
    private void spinUp()
    {
        this .timer .setDelay( 20 );
        this .interpolation = parameter -> parameter * parameter; // simple parabola acceleration
        this .nextStep = () -> keepSpinning();
    }
    
    private void keepSpinning()
    {
        this .interpolation = parameter -> parameter; // linear for the spin
        this .nextStep = null; // let it spin
    }

    private void spinDown( Runnable onStopped )
    {
        this .interpolation = parameter -> 1 - (1-parameter) * (1-parameter); // parabolic deceleration
        this .nextStep = onStopped;
    }
    
    public void defer()
    {
        if ( this .timer .isRunning() )
            spinDown( () -> this .timer .setDelay( 3000 ) );
    }

    public void stop( Runnable onStopped )
    {
        if ( this .timer .isRunning() ) {
            this .nextStep = () -> {
                spinDown( () -> {
                    this .timer .stop();
                    if ( onStopped != null )
                        onStopped .run();
                } );
            };
        }
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        if ( this .timer .getDelay() == 3000 ) {
            // The 3 seconds expired, so we can start up the spin again
            spinUp();
            return;
        }

        float parameter = iteration / (float) iterations;
        double radians = this .interpolation .apply( parameter ) * 2 * Math.PI;

        double x = Math .cos( radians ) - 1d; // We want this to range [-2,0], so the transition is smooth
        double y = Math .sin( radians );

        Vector3f newLook = new Vector3f( staticLookDir );

        Vector3f xComponent = new Vector3f( xAxis );
        xComponent .scale( (float) x * 2 );
        newLook .add( xComponent );

        Vector3f yComponent = new Vector3f( yAxis );
        yComponent .scale( (float) y );
        newLook .add( yComponent );

        this .cameraController .setViewDirection( newLook, newUp );

        ++ this .iteration;
        if ( this .iteration == iterations )
        {
            this .iteration = 0; // came full circle, iterate again, unless...
            if ( this .nextStep != null )
                this .nextStep .run();
        }
    }

    /**
     * Straight out of https://en.wikipedia.org/wiki/Smoothstep
     */
    private static float smootherstep( float edge0, float edge1, float x )
    {
        // Scale, and clamp x to 0..1 range
        x = clamp( (x - edge0) / (edge1 - edge0), 0.0f, 1.0f );
        // Evaluate polynomial
        return x * x * x * (x * (x * 6 - 15) + 10);
    }

    private static float clamp( float x, float lowerlimit, float upperlimit )
    {
        if (x < lowerlimit)
            x = lowerlimit;
        if (x > upperlimit)
            x = upperlimit;
        return x;
    }
}
