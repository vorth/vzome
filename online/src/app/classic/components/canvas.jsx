
import { createEffect } from 'solid-js';
import * as THREE from 'three';
import { TrackballControls } from 'three-stdlib/controls/TrackballControls.js';

// // Adapted from https://github.com/mrdoob/three.js/blob/master/examples/webgl_multiple_elements.html

// // Reusable renderer with an offscreen GL context!  Now we don't care how many times this component is used on the page!
// const renderer = new THREE.WebGLRenderer({
//   antialias: true,
//   alpha: true,
// });
// // renderer.setClearColor( 0x070707, 1 );
// renderer.setPixelRatio( window.devicePixelRatio );

// function render( scene, camera, canvas )
// {
//   // so something moves
//   scene.children[ 0 ].rotation.y = Date.now() * 0.001;

//   // get its position relative to the page's viewport
//   const rect = canvas.getBoundingClientRect();

//   // TODO: check if it's offscreen. If so skip it

//   // set the viewport
//   const width = rect.right - rect.left;
//   const height = rect.bottom - rect.top;

//   camera.aspect = width / height;
//   camera.updateProjectionMatrix();

//   //scene.userData.controls.update();

//   renderer.setSize( width, height ); // TODO: is it faster if we cache values and only call this when necessary?
//   renderer.render( scene, camera ); // render onto the offscreen canvas

//   const context = canvas .getContext("2d");
//   context .clearRect( 0, 0, canvas.width, canvas.height );
//   context .drawImage( renderer.domElement, 0, 0 );
// }

const Canvas3 = props =>
{
  let canvasRef;
  createEffect( () => {

    const renderer = new THREE.WebGLRenderer( {
      canvas: canvasRef,
      antialias: true,
      alpha: true,
    });
    // renderer.setClearColor( 0x070707, 1 );
    renderer.setPixelRatio( window.devicePixelRatio );
    
    const scene = new THREE.Scene();
    scene.background = new THREE.Color( 0x005080 );

    // Objects
    const geometry = props.box? new THREE.BoxGeometry( 5, 8, 13 ) : new THREE.DodecahedronGeometry( 4 );

    // Materials

    const material = new THREE.MeshLambertMaterial()
    material.color = new THREE.Color(0xff0099)

    // Mesh
    const mesh = new THREE.Mesh( geometry, material )
    scene.add(mesh)

    // Camera
    const camera = new THREE.PerspectiveCamera( 50, 1, 0.1, 1000 )
    camera.position.x = 0
    camera.position.y = 0
    camera.position.z = 20
    scene.add( camera )

    // Lights
    const pointLight = new THREE.PointLight(0xffffff, 0.7)
    pointLight.position.x = 2
    pointLight.position.y = 3
    pointLight.position.z = 14
    camera.add(pointLight)

    const pointLight2 = new THREE.PointLight(0xffffff, 0.4)
    pointLight2.position.x = -2
    pointLight2.position.y = -1
    pointLight2.position.z = 14
    camera.add(pointLight2)

    // Controls
    const controls = new TrackballControls( camera, canvasRef );
    controls.staticMoving = true;
    controls.enableDamping = true;
    controls.rotateSpeed = 4.5;
    controls.zoomSpeed = 3;
    controls.panSpeed = 1;
    
    function render()
    {
      // so something moves
      // scene.children[ 0 ].rotation.y = Date.now() * 0.001;
    
      // get its position relative to the page's viewport
      const rect = canvasRef.getBoundingClientRect();
    
      // TODO: check if it's offscreen. If so skip it
    
      // set the viewport
      const width = rect.right - rect.left;
      const height = rect.bottom - rect.top;
    
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
    
      controls.update();
    
      renderer.setSize( width, height ); // TODO: is it faster if we cache values and only call this when necessary?
      renderer.render( scene, camera ); // render onto the offscreen canvas
    }

    const animate = () =>
    {
      render();
      requestAnimationFrame( animate );
    }
    animate();
  });

  return <canvas ref={canvasRef} style={{ height: '100%', width: '100%' }}/>;
}

export { Canvas3 }