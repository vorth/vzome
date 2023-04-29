

import { createEffect } from 'solid-js';
import { LightedTrackballCanvas } from './ltcanvas.jsx';
import { ShapedGeometry } from './geometry.jsx';
import { Canvas3 } from './canvas.jsx';

const SceneCanvas = ( props ) =>
{
  return (
    <Canvas3 rotationOnly={props.rotationOnly}>
      <ShapedGeometry embedding={props.scene.embedding} shapes={props.scene?.shapes} toolActions={props.toolActions} />
      {props.children3d}
    </Canvas3>
  );
    // <Show when={ () => props.scene?.shapes }>
      // <LightedTrackballCanvas toolActions={props.toolActions} height={props.height} width={props.width} >
      //   <ShapedGeometry embedding={props.scene.embedding} shapes={props.scene?.shapes} toolActions={props.toolActions} />
      //   {props.children3d}
      // </LightedTrackballCanvas>
    // </Show> );
}

export { SceneCanvas };
