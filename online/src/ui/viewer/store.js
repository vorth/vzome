
import { configureStore } from '@reduxjs/toolkit'

export const initialState = {};

const reducer = ( state = initialState, event ) =>
{
  switch ( event.type ) {

    case 'ALERT_RAISED':
      console.log( `Alert from the worker: ${event.payload}` );
      return { ...state, problem: event.payload, waiting: false };

    case 'ALERT_DISMISSED':
      return { ...state, problem: '' };

    case 'FETCH_STARTED': {
      if ( state.waiting )
        return state; // the fetch was started already
      const { url, viewOnly } = event.payload;
      return { ...state, waiting: true, editing: !viewOnly };
    }

    case 'TEXT_FETCHED':
      return { ...state, source: event.payload };

    case 'DESIGN_RENDERED': {
      let { scene, xmlTree } = event.payload;
      const attributes = {};
      const indexAttributes = node => node.children && node.children.map( child => {
        attributes[ child.id ] = child.attributes;
        indexAttributes( child );
      })
      if ( xmlTree ) {
        indexAttributes( xmlTree );
        xmlTree = branchSelectionBlocks( xmlTree );
      }
      // may need to merge scene.shapes here, for incremental case
      return { ...state, scene: { ...state.scene, ...scene }, waiting: false, xmlTree, attributes };
    }

    case 'EDIT_RENDERED': {
      const { scene } = event.payload;
      // may need to merge scene.shapes here, for incremental case
      return { ...state, scene: { ...state.scene, ...scene }, waiting: false };
    }

    default:
      return state;
  }
};

const branchSelectionBlocks = node =>
{
  if ( node.children && node.children.length > 1 ) {
    const newChildren = [];
    let block = null;
    for (const child of node.children) {
      if ( child.tagName === 'BeginBlock' ) {
        block = [];
        // discard this BeginBlock node (don't push it)
      } else if ( child.tagName === 'EndBlock' ) {
        const newNode = { tagName: 'ChangeSelection', id: child.id, children: block, attributes: {} };
        newChildren.push( newNode );
        block = null;
      } else if ( block ) {
        block.push( child ); // child can't be a branch inside a block
      } else {
        newChildren.push( branchSelectionBlocks( child ) );
      }
    }
    node.children = newChildren;
    return node;
  }
  else
    return node;
}

export const createWorkerStore = customElement =>
{
  // trampolining to work around worker CORS issue
  // see https://github.com/evanw/esbuild/issues/312#issuecomment-1025066671
  const workerPromise = import( "../../worker/vzome-worker-static.js" )
    .then( module => {
      const blob = new Blob( [ `import "${module.WORKER_ENTRY_FILE_URL}";` ], { type: "text/javascript" } );
      const worker = new Worker( URL.createObjectURL( blob ), { type: "module" } );
      worker.onmessage = onWorkerMessage;
      return worker;
    } );

  const workerSender = store => report => event =>
  {
    switch ( event.type ) {

      // These are all coming back from the worker
      case 'ALERT_RAISED':
      case 'ALERT_DISMISSED':
      case 'FETCH_STARTED':
      case 'TEXT_FETCHED':
      case 'DESIGN_RENDERED':
      case 'EDIT_RENDERED':
        report( event );
        break;

      // Anything else is to send to the worker
      default:
        if ( navigator.userAgent.indexOf( "Firefox" ) > -1 ) {
            console.log( "The worker is not available in Firefox" );
            report( { type: 'ALERT_RAISED', payload: 'Module workers are not yet supported in Firefox.  Please try another browser.' } );
        }
        else {
          workerPromise.then( worker => {
            // console.log( `Message sending to worker: ${JSON.stringify( event, null, 2 )}` );
            worker.postMessage( event );  // send them all, let the worker filter them out
          } )
          .catch( error => {
            console.log( "The worker is not available" );
            report( { type: 'ALERT_RAISED', payload: 'The worker is not available.  Module workers are not supported in older versions of other browsers.  Please update your browser.' } );
          } );
        }
        break;    
    }
  }

  const preloadedState = {}

  const store = configureStore( {
    reducer,
    preloadedState,
    middleware: getDefaultMiddleware => getDefaultMiddleware().concat( workerSender ),
    devTools: true,
  });

  const onWorkerMessage = ({ data }) => {
    console.log( `Message received from worker: ${JSON.stringify( data.type, null, 2 )}` );
    store .dispatch( data );

    // Useful for supporting regression testing of the vzome-viewer web component
    if ( customElement ) {
      switch (data.type) {

        case 'DESIGN_RENDERED':
          customElement.dispatchEvent( new Event( 'vzome-design-rendered' ) );
          break;
      
        case 'ALERT_RAISED':
          customElement.dispatchEvent( new Event( 'vzome-design-failed' ) );
          break;
      
        default:
          break;
      }
    }
  }

  return store;
}
