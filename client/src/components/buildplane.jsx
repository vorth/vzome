import React from 'react'
import * as THREE from 'three'
import { useResource } from 'react-three-fiber'

function BuildPlane( { config, startGridHover, stopGridHover } )
{
  const { position, quaternions, quatIndex, grid, color, size, field } = config
  const quaternion = quaternions[ quatIndex ]
  const [ materialRef, material ] = useResource()
  const rsize = field.embed( size )
  const planeSize = rsize * 8
  const dotSize = rsize / 24
  
  const makeAbsolute = ( gridPt ) =>
  {
    let vector3d = field.quatTransform( quaternion, [ ...gridPt, field.zero ] )
    return field.vectoradd( position, vector3d )
  }
  const handleHoverIn = ( e, gridPt ) =>
  {
    e.stopPropagation()
    startGridHover( makeAbsolute( gridPt ) )
  }
  const handleHoverOut = ( e, gridPt ) =>
  {
    e.stopPropagation()
    stopGridHover( makeAbsolute( gridPt ) )
  }
  const handleClick = ( e, gridPt ) =>
  {
    e.stopPropagation()
    console.log( "handle grid click: " + JSON.stringify( gridPt ) )
  }
  const wlast = q =>
  {
    const [ w, x, y, z ] = q
    return [ x, y, z, w ]
  }
  
  return (
    <group position={field.embedv( position )} quaternion={field.embedv( wlast( quaternion ) )}>
      <meshLambertMaterial ref={materialRef} transparent={true} opacity={0.7} color={color} side={THREE.DoubleSide} />
      <mesh material={material} >
        <planeGeometry attach="geometry" args={[ planeSize, planeSize ]} />
      </mesh>
      {grid.map( ( gv ) => {
        const [ x, y, z ] = field.embedv( gv ) 
        return (
          <mesh position={[x,y,z]} key={JSON.stringify( gv )} material={material}
              onPointerOver={ e => handleHoverIn( e, gv ) }
              onPointerOut={ e => handleHoverOut( e, gv ) }
              onClick={ e => handleClick( e, gv ) }>
            <icosahedronBufferGeometry attach="geometry" args={[dotSize]} />
          </mesh>
        )}) }
    </group>
  )
}

export default BuildPlane