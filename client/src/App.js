import React from 'react'
import 'bootstrap/dist/css/bootstrap.min.css'
import './App.css'

import ModelCanvas from './components/modelcanvas-three.js'
import WebLoader from './components/webloader.js'
import Models from './components/models.js'
import FileOpener from './components/fileopener.js'
import Exporter from './components/exporter.js'
import ErrorAlert from './components/alert.js'
import EditMenu from './components/editmenu.js'
import Spinner from './components/spinner.js'
import PlaneSwitch from './components/planeswitch.js'

function App() {
  return (
    <>
      <ModelCanvas/>
      <PlaneSwitch/>
      <WebLoader/>
      <Models/>
      <FileOpener/>
      <Exporter/>
      <ErrorAlert/>
      <EditMenu/>
      <Spinner/>
      {/* <div>Export icon made from <a href="http://www.onlinewebfonts.com/icon">Icon Fonts</a> is licensed by CC BY 3.0</div> */}
      {/* <a href="https://github.com/mhnpd/react-loader-spinner">react-loader-spinner</a> */}
      {/* <a href="http://simpleicon.com/folder-2.html">folder icon</a> */}
    </>
  );
}

export default App;
