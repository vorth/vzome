
import React from 'react'
import { forwardRef, useImperativeHandle } from 'react';
import { useEmbedding, useRotation, useGeometry } from './hooks.js'
import { useThree } from '@react-three/fiber'
import { GLTFExporter } from 'https://cdn.skypack.dev/three@0.129.0/examples/jsm/exporters/GLTFExporter.js';

const Instance = ( { id, vectors, position, rotation, geometry, color, selected, highlightBall=()=>{}, onClick, onHover } ) =>
{
  const ref = useRotation( rotation );
  
  const handleHoverIn = ( e ) =>
  {
    e.stopPropagation()
    onHover && onHover( vectors, true )
  }
  const handleHoverOut = ( e ) =>
  {
    e.stopPropagation()
    onHover && onHover( vectors, false )
  }
  const handleClick = ( e ) =>
  {
    if ( onClick ) { // may be undefined when the model is not editable, or when the object is not clickable in the current mode
      e.stopPropagation()
      onClick && onClick( id, vectors, selected )
    }
  }
  const emissive = selected? "#f6f6f6" : highlightBall( id )? "#884444" : "black"
  // TODO: cache materials
  return (
    <group position={ position } >
      <mesh matrixAutoUpdate={false} ref={ ref } geometry={geometry} onPointerOver={handleHoverIn} onPointerOut={handleHoverOut} onClick={handleClick}>
        <meshLambertMaterial attach="material" color={color} emissive={emissive} />
      </mesh>
    </group>
  )
}

const InstancedShape = ( { shape, onClick, onHover, highlightBall } ) =>
{
  const geometry = useGeometry( shape );
  if ( shape.instances.length === 0 )
    return null;
  return (
    <>
      { shape.instances.map( instance =>
        <Instance key={instance.id} {...instance} geometry={geometry} highlightBall={highlightBall} onClick={onClick} onHover={onHover} /> ) }
    </>
  )
}

export const ShapedGeometry = forwardRef(( { shapes, embedding, highlightBall, handleClick, onHover }, exporterRef ) =>
{
  const { scene } = useThree();

  const gltfExporter = {
    exportGltfJson: writeFile =>
    {
      const exporter = new GLTFExporter();
      // Parse the input and generate the glTF output
      exporter.parse( scene, gltf => {
        writeFile( JSON.stringify( gltf, null, 2 ) );
      }, {} );
    }
  };
  // This lets the download menu make the generateGltfJson call
  useImperativeHandle( exporterRef, () => gltfExporter );

  const bkgdClick = () =>
  {
    // no-op for now
  }

  const ref = useEmbedding( embedding );
  return ( shapes &&
    <group matrixAutoUpdate={false} ref={ref} onPointerMissed={bkgdClick}>
      { Object.values( shapes ).map( shape =>
        <InstancedShape key={shape.id} {...{ shape, highlightBall, handleClick, onHover }} />
      ) }
    </group>
  ) || null
});
