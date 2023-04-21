// babel workaround
import "regenerator-runtime/runtime";

import { render } from 'solid-js/web';
import { enableRootsAutoattach } from '@solid-devtools/debugger'
import { attachDevtoolsOverlay } from '@solid-devtools/overlay'

enableRootsAutoattach();
attachDevtoolsOverlay();

import { VZomeAppBar } from './components/appbar.jsx';
import { createWorkerStore } from './controllers-solid.js';
import { ClassicEditor } from './classic.jsx';
import { createWorker } from "../../workerClient/client.js";

const worker = createWorker();

const Classic = () =>
{
  const { rootController, getScene, setState } = createWorkerStore( worker );

  return (
  <>
    <VZomeAppBar getScene={getScene} controller={rootController()} />
    <ClassicEditor getScene={getScene} setState={setState} controller={rootController()} />
  </>
);
}

render( Classic, document.getElementById('root'))

