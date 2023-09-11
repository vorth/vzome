
import { useFrame, Canvas } from "solid-three";
import { Color } from "three";
import { createEffect, createMemo, createRenderEffect, onMount } from "solid-js";
import { createElementSize } from "@solid-primitives/resize-observer";

import { PerspectiveCamera } from "./perspectivecamera.jsx";
import { TrackballControls } from "./trackballcontrols.jsx";
import { useWorkerClient } from "../../workerClient/index.js";
import { useInteractionTool } from "./interaction.jsx";

const Lighting = props =>
{
  const color = createMemo( () => new Color( props.backgroundColor ) );
  useFrame( ({scene}) => { scene.background = color() } )
  // const { scene } = useThree();
  // const centerObject = () => scene.getObjectByName('Center');
  let centerObject;
  return (
    <>
      <group ref={centerObject} position={[0,0,0]} visible={false} />
      <ambientLight color={props.ambientColor} intensity={1.5} />
      <For each={props.directionalLights}>{ ( { color, direction } ) =>
        <directionalLight target={centerObject} intensity={1.7} color={color} position={direction.map( x => -x )} />
      }</For>
    </>
  )
}

const defaultLighting = {
  // backgroundColor: '#8CC2E7',
  ambientColor: '#333333',
  directionalLights: [ // These are the vZome defaults, for consistency
    { direction: [ 1, -1, -0.3 ], color: '#FDFDFD' },
    { direction: [ -1, 0, -0.2 ], color: '#B5B5B5' },
    { direction: [ 0, 0, -1 ], color: '#303030' },
  ]
}

const toVector = vector3 =>
{
  const { x, y, z } = vector3;
  return [ x, y, z ];
}

// Thanks to Paul Henschel for this, to fix the camera.lookAt by adjusting the Controls target
//   https://github.com/react-spring/react-three-fiber/discussions/609

const LightedCameraControls = (props) =>
{
  const { setState } = useWorkerClient();
  // Here we can useThree, etc., which we could not in LightedTrackballCanvas

  const trackballEnd = evt =>
  {
    if ( ! setState ) return;
    
    const trackball = evt.target;
    const camera = trackball.object;
    const up = toVector( camera.up );
    const position = toVector( camera.position );
    const lookAt = toVector( trackball.target );
    const [ x, y, z ] = lookAt.map( (e,i) => e - position[ i ] );
    const distance = Math.sqrt( x*x + y*y + z*z );
    const lookDir = [ x/distance, y/distance, z/distance ];

    // This was missing, and vZome reads width to set FOV
    const fovX = camera.fov * (Math.PI/180) * camera.aspect; // Switch from Y-based FOV degrees to X-based radians
    const width = 2 * distance * Math.tan( fovX / 2 );
    // This is needed to keep the fog depth correct in desktop.
    //  See the reducer, where the width/distance ratio is maintained.
    const far = camera.far;
    const near = camera.near;

    // console.log( 'trackballEnd setState liveCamera' );
    setState( 'liveCamera', { lookAt, up, lookDir, distance, width, far, near } );

    // setNeedsRender( 20 );
  }

  const position = createMemo( () => {
    const dist = props.sceneCamera?.distance;
    const lookDir = props.sceneCamera?.lookDir;
    const result = props.sceneCamera?.lookAt.map( (e,i) => e - dist * lookDir[ i ] );
    return result;
  } );
  const fov = createMemo( () => {
    const halfX = props.sceneCamera?.width / 2;
    const halfY = halfX / props.aspect;
    return 360 * Math.atan( halfY / props.sceneCamera?.distance ) / Math.PI;
  } );

  const lights = createMemo( () => {
    const backgroundColor = props.lighting?.backgroundColor || defaultLighting.backgroundColor;
    return { ...defaultLighting, backgroundColor };
  });

  const result = ( !!props.sceneCamera &&
    <>
      <PerspectiveCamera fov={fov()} aspect={props.aspect} position={position()} up={props.sceneCamera?.up} target={props.sceneCamera?.lookAt} >
        <Lighting {...(lights())} />
      </PerspectiveCamera>
      <TrackballControls onEnd={props.rotationOnly? undefined : trackballEnd} rotationOnly={props.rotationOnly}
          staticMoving='true' rotateSpeed={4.5} zoomSpeed={3} panSpeed={1} target={props.sceneCamera?.lookAt} />
    </>
  );

  return result;
}

const isLeftMouseButton = e =>
{
  e = e || window.event;
  if ( "which" in e )  // Gecko (Firefox), WebKit (Safari/Chrome) & Opera
    return e.which === 1
  else if ( "button" in e )  // IE, Opera 
    return e.button === 0
  return false
}

export const LightedTrackballCanvas = ( props ) =>
{
  let size;
  const aspect = () => ( size && size.height )? size.width / size.height : 1;

  const [ tool ] = useInteractionTool();

  const handlePointerMove = ( e ) =>
  {
    const handler = tool && tool() ?.onDrag;
    if ( isLeftMouseButton( e ) && handler ) {
      e.stopPropagation()
      handler();
    }
  }
  const handlePointerUp = ( e ) =>
  {
    const handler = tool && tool() ?.onDragEnd;
    if ( isLeftMouseButton( e ) && handler ) {
      // e.stopPropagation()
      handler();
    }
  }
  const handlePointerMissed = ( e ) =>
  {
    const handler = tool && tool() ?.bkgdClick;
    if ( isLeftMouseButton( e ) && handler ) {
      e.stopPropagation()
      handler();
    }
  }

  const canvas =
    <Canvas id='lighted-canvas' dpr={ window.devicePixelRatio } gl={{ antialias: true, alpha: false }}
        height={props.height ?? "100vh"} width={props.width ?? "100vw"}
        frameloop="always" onPointerMissed={handlePointerMissed} >
      <LightedCameraControls lighting={props.lighting} aspect={aspect()} rotationOnly={props.rotationOnly}
        sceneCamera={props.sceneCamera} />
      {props.children}
    </Canvas>;
  
  canvas.style.display = 'flex';
  size = createElementSize( canvas );

  createRenderEffect( () => {
    canvas.style.cursor = (tool && tool().cursor) || 'auto';
  });
  
  onMount( () => {
    // canvas .addEventListener( 'pointermove', handlePointerMove );
    canvas .addEventListener( 'pointerup', handlePointerUp );
  });

  return canvas;
}
