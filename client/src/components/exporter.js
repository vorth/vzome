
import React, { useState } from 'react'
import { connect } from 'react-redux'
import { exportTriggered } from '../bundles/vzomejava'
import Modal from 'react-bootstrap/Modal'
import Button from 'react-bootstrap/Button'
import Tab from 'react-bootstrap/Tab'
import Row from 'react-bootstrap/Row'
import Col from 'react-bootstrap/Col'
import Nav from 'react-bootstrap/Nav'

const formats = [
  {
    key: "dae",
    label: "COLLADA (dae)",
    description: "COLLADA (COLLAborative Design Activity) " +
      "is an interchange file format for interactive 3D applications. " +
      "COLLADA documents are XML files, usually identified with a .dae (digital asset exchange) filename extension."
  },
  {
    key: "pov",
    label: "POV-Ray",
    description: ""
  },
  {
    key: "StL",
    label: "STL",
    description: "Stereolithography format is commonly used for 3D printing.  vZome STL exports are in millimeters, the typical default for slicers."
  },
  {
    key: "dxf",
    label: "AutoCAD DXF",
    description: ""
  },
  // {
  //   key: "mesh",
  //   label: "Simple Mesh JSON",
  //   description: "This is custom vZome format for basic geometry interchange."
  // },
  // {
  //   key: "cmesh",
  //   label: "Color Mesh JSON",
  //   description: "This is custom vZome format for basic geometry interchange, including colors."
  // },
  {
    key: "shapes",
    label: "vZome Shapes JSON",
    description: "This is custom vZome format for complete capture, including ball and strut shapes and colors."
  }
]

const Exporter = ({ enabled, doExport }) =>
{
  const [show, setShow] = useState( false );
  const [key, setFormat] = useState( formats[0].key );

  const handleCancel = () =>{
    setShow( false )
  }
  const handleExport = () =>{
    setShow( false )
    const format = formats.find( f => f.key === key )
    doExport( key, `Exporting ${format.label}...` )
  }
  const handleShow = () => setShow(true);

  return (
    <>
      <Button id="exporter" variant="link" onClick={handleShow}
          style={{ cursor: enabled ? 'pointer' : 'default' }} >
          <img alt="export" className="Icon" src="/app/export.svg" />
      </Button>

      <Modal centered show={show} size='lg' onHide={handleCancel}>
        <Modal.Header closeButton>
          <Modal.Title>Export a Model File</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Tab.Container id="model-tabs" defaultActiveKey={key} onSelect={setFormat}>
            <Row>
              <Col sm={4}>
                <Nav variant="pills" className="flex-column">
                  { formats.map( (format) => (
                    <Nav.Item>
                      <Nav.Link key={format.key} eventKey={format.key}>{format.label}</Nav.Link>
                    </Nav.Item>
                  ) ) }
                </Nav>
              </Col>
              <Col sm={8}>
                <Tab.Content>
                  { formats.map( (format) => (
                    <Tab.Pane key={format.key} eventKey={format.key}>{format.description}</Tab.Pane>
                  ) ) }
                </Tab.Content>
              </Col>
            </Row>
          </Tab.Container>
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={handleCancel}>
            Cancel
          </Button>
          <Button variant="primary" onClick={handleExport}>
            Export
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  )
} 

const select = (state) => ({
  enabled: state.jre.javaReady
})

const boundEventActions = {
  doExport : exportTriggered
}

export default connect( select, boundEventActions )( Exporter )
