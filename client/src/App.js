import React from 'react';
import 'bootstrap/dist/css/bootstrap.min.css';
import './App.css';

import ModelCanvas from './components/modelcanvas-three.js';
import Models from './components/models.js';
import FileOpener from './components/fileopener.js';
import Exporter from './components/exporter.js';
import Spinner from './components/spinner.js';

function App() {
  return (
    <div>
      <ModelCanvas/>
      <Models/>
      <FileOpener/>
      <Exporter/>
      <Spinner/>
      {/* <div>Export icon made from <a href="http://www.onlinewebfonts.com/icon">Icon Fonts</a> is licensed by CC BY 3.0</div> */}
      {/* <a href="https://github.com/mhnpd/react-loader-spinner">react-loader-spinner</a> */}
      {/* <a href="http://simpleicon.com/folder-2.html">folder icon</a> */}
    </div>
  );
}

export default App;
