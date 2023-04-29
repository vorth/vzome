
import { createEffect } from 'solid-js';
import * as THREE from 'three';
import { TrackballControls } from 'three-stdlib/controls/TrackballControls.js';
import { useRotation } from './camera.jsx';

// // Adapted from https://github.com/mrdoob/three.js/blob/master/examples/webgl_multiple_elements.html
// // Reusable renderer with an offscreen GL context!  Now we don't care how many times this component is used on the page!
// const renderer = new THREE.WebGLRenderer({
//   antialias: true,
//   alpha: true,
// });
// // renderer.setClearColor( 0x070707, 1 );
// renderer.setPixelRatio( window.devicePixelRatio );

const Canvas3 = props =>
{
  const [ lastRotation, publishRotation ] = useRotation();

  const renderer = new THREE.WebGLRenderer( {
    antialias: true,
    alpha: true,
  });
  renderer.setPixelRatio( window.devicePixelRatio );
  const canvas = renderer.domElement;
  
  const scene = new THREE.Scene();
  scene.background = new THREE.Color( 0x005080 );

  // Camera
  const camera = new THREE.PerspectiveCamera( 50, 1, 0.1, 1000 );
  camera.position.x = 0
  camera.position.y = 0
  camera.position.z = 20
  const cameraPosition = new THREE.Vector3() .copy( camera.position );
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
  let controls;
  createEffect( () => {
    // Why is this part inside an effect?  Well, apparently the mouse event handling doesn't work
    //  if the canvas is not actually attached to the document.  Without the effect, I could
    //  zoom with the scroll wheel, but not rotate or pan.
    // Also, now, this will be reactive if props.rotationOnly changes
    if ( ! props.rotationOnly ) {
      controls = new TrackballControls( camera, canvas );
      controls.staticMoving = true;
      controls.rotateSpeed = props.rotationOnly? 2 : 4.5;
      controls.zoomSpeed = 3;
      controls.panSpeed = 1;
      controls.addEventListener( 'change', (evt) => {
        // filter out pan and zoom changes
        // HACK! Assumes knowledge of TrackballControls internals
        if ( controls._state !== 0 && controls._state !== 3 ) // ROTATE, TOUCH_ROTATE
          return;
        // TODO: publish absolute values rather than changes
        const { _lastAxis, _lastAngle } = controls; // the rotation change
        const quaternion = new THREE.Quaternion() .setFromAxisAngle( _lastAxis, _lastAngle );
        publishRotation( quaternion, camera );
      } );
    }
  } );
  const _eye = new THREE.Vector3(), target = new THREE.Vector3();
  createEffect( () => {
    let { quaternion, sourceCamera } = lastRotation();
    // This sourceCamera test is equivalent to just checking to see if controls is undefined, for now,
    //  but it will continue to work fine if I ever restore the secondary trackball with a nicer implementation.
    if ( sourceCamera && sourceCamera !== camera ) {
      // Mimic the logic of TrackballControls.update() with rotate()
      _eye.copy( camera.position ).sub( target );
      _eye.applyQuaternion( quaternion );
      camera.up.applyQuaternion( quaternion );
      camera.position.addVectors( target, _eye );
      camera.lookAt( target );
    }
  });

  function render()
  {
    // so something moves
    // scene.children[ 0 ].rotation.y = Date.now() * 0.001;
  
    // get its position relative to the page's viewport
    const rect = canvas.getBoundingClientRect();
  
    // TODO: check if it's offscreen. If so skip it
  
    // set the viewport
    const width = rect.right - rect.left;
    const height = rect.bottom - rect.top;
  
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
  
    // controls are absent for the secondary scene
    controls && controls.update(); // TODO: separate driver loop for controls, so unnecessary renders are avoided
    //  (We must have a loop for TrackballControls; it has no internal call to update(),
    //    and the change event we need is only fired within update().)
  
    renderer.setSize( width, height ); // TODO: is it faster if we cache values and only call this when necessary?
    renderer.render( scene, camera ); // render onto the offscreen canvas
  }

  const animate = () =>
  {
    render();
    requestAnimationFrame( animate );
  }
  animate();

  canvas.style = 'height: 100%; width: 100%';

  props.children .forEach( child => scene .add( child ) );
  return canvas;
}

export { Canvas3 }