
import React from 'react'
import { connect } from 'react-redux'
import Button from 'react-bootstrap/Button'
import OverlayTrigger from 'react-bootstrap/OverlayTrigger'
import Tooltip from 'react-bootstrap/Tooltip'
import * as planes from '../bundles/planes'


const Exporter = ({ enabled, switchPlane }) =>
{
  if ( enabled )
    return (
      <OverlayTrigger placement="bottom" overlay={<Tooltip>Switch build plane</Tooltip>} >
        <Button id="planeswitch" variant="link" onClick={switchPlane}
          style={{ cursor: enabled ? 'pointer' : 'default' }} >
          <img alt="switch plane" className="Icon" src="/app/dodecFaces.svg" />
        </Button>
      </OverlayTrigger>
    )
  else
    return null
} 

const select = (state) => ({
  enabled: true && state.workingPlane && state.workingPlane.enabled
})

const boundEventActions = {
  switchPlane: planes.doChangeOrientation
}

export default connect( select, boundEventActions )( Exporter )
