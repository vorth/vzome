
import { createSignal, createContext, useContext } from "solid-js";
import { Quaternion } from 'three';
import { controllerProperty } from '../controllers-solid.js';
import { icosahedralScene } from '../icosahedral-vef.js';
import { octahedralScene } from '../octahedral-vef.js';
import { SceneCanvas } from './scenecanvas.jsx';

const scenes = {
  icosahedral: icosahedralScene,
  octahedral: octahedralScene,
};

const CameraControls = props =>
{
  // TODO: use symmetry to look up the scene to use, somehow, rather than hardcoding icosahedralScene
  const symmetry = () => controllerProperty( props.controller, 'symmetry' );

  // Why isn't this reactive when props.bkgdColor changes for a loaded model?
  const scene = () => {
    const symmScene = scenes[ symmetry() || 'icosahedral' ];
    return ({ ...symmScene, lighting: { ...symmScene.lighting, backgroundColor: props.bkgdColor } } );
  }

  // createEffect( () => {
  //   console.log( "backgroundColor is ", scene() .lighting .backgroundColor );
  // } );

  return (
    <div id='camera-controls' style={{ display: 'grid', 'grid-template-rows': 'min-content min-content' }}>
      <div id='camera-buttons' class='placeholder' style={{ 'min-height': '60px' }} >perspective | snap | outlines</div>
      <div id="ball-and-slider" style={{ display: 'grid', 'grid-template-columns': '3fr 1fr' }}>
        <div id="camera-trackball" style={{ border: '1px solid', minHeight: '200px' }}>
          <SceneCanvas scene={scene()} trackball={false} rotationOnly={true} />
        </div>
        <div id='zoom-slider' class='placeholder' style={{ 'min-height': '100px', 'min-width': '60px' }} >zoom</div>
      </div>
    </div>
  )
}

// We need to record the sourceCamera so we can make sure that trackball changes
//  don't try to drive the camera for the same scene

const defaultRotation = { quaternion: new Quaternion(), sourceCamera: null };

const RotationContext = createContext( {} );

const RotationProvider = (props) =>
{
  const [ lastRotation, setLastRotation ] = createSignal( defaultRotation );
  const publishRotation = ( quaternion, sourceCamera ) => setLastRotation( { quaternion, sourceCamera } );
  const value = [
    lastRotation, publishRotation
  ];

  return (
    <RotationContext.Provider value={value}>
      {props.children}
    </RotationContext.Provider>
  );
}

function useRotation() { return useContext( RotationContext ); }

export { CameraControls, useRotation, RotationProvider }