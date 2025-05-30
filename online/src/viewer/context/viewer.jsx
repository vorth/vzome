
import { createContext, createSignal, useContext } from "solid-js";
import { createStore, reconcile, unwrap } from "solid-js/store";

import { useWorkerClient } from "./worker.jsx";
import { useCamera } from "./camera.jsx";
import { decodeEntities, fetchDesign, selectSnapshot, openTextContent, encodeEntities } from "../util/actions.js";

const ViewerContext = createContext( { scene: ()=> { console.log( 'NO ViewerProvider' ); }, problem: ()=>null } );

const ViewerProvider = ( props ) =>
{
  const { url, labels: showLabels } = props.config || {};
  const [ scene, setScene ] = createStore( {} );
  const [ scenes, setScenes ] = createStore( [] );
  const [ source, setSource ] = createStore( {} );
  const [ problem, setProblem ] = createSignal( '' ); // cooperatively managed by both worker and client
  const [ waiting, setWaiting ] = createSignal( false );
  const [ labels, setLabels ] = createSignal( showLabels );
  const { postMessage, subscribeFor } = useWorkerClient();
  const { state, tweenCamera, setLighting } = useCamera();

  const requestDesign = ( url, config ) =>
  {
    setWaiting( true );
    postMessage( fetchDesign( url, config ) );
  }

  const openText = ( name, contents ) =>
  {
    setWaiting( true );
    postMessage( openTextContent( name, contents ) );
  }

  url && postMessage( fetchDesign( url, props.config ) );

  const addShape = ( shape ) =>
  {
    if ( ! scene .shapes ) {
      setScene( 'shapes', {} );
    }
    if ( ! scene ?.shapes[ shape.id ] ) {
      setScene( 'shapes', shape.id, shape );
      return true;
    }
    return false;
  }

  const updateShapes = ( shapes ) =>
  {
    for (const [id, shape] of Object.entries(shapes)) {
      if ( ! addShape( shape ) ) {
        // shape is not new, so just replace its instances
        setScene( 'shapes', id, 'instances', shape.instances );
      }
    }
    // clean up preview strut, which may be a shape otherwise not in the scene
    for ( const id of Object.keys( scene ?.shapes || {} ) ) {
      if ( ! (id in shapes) )
      setScene( 'shapes', id, 'instances', [] );
    }
  }

  subscribeFor( 'SYMMETRY_CHANGED', ( { orientations } ) => {
    setScene( 'orientations', orientations );
  });

  subscribeFor( 'SCENES_DISCOVERED', ( { lighting, scenes } ) => {
    const newScenes = scenes .map( scene => ({ ...scene, title: decodeEntities( scene.title ), content: decodeEntities( scene.content || ' ' ) }));
    setScenes( newScenes );
    if ( !! lighting ) { // lighting only present in loaded designs
      const { backgroundColor } = lighting;
      setLighting( { ...state.lighting, backgroundColor } ); // no per-scene lighting yet
    }
    const camera = scenes[ 0 ].camera;
    if ( !! camera ) // camera only present in loaded designs
      tweenCamera( scenes[ 0 ].camera );
  });

  subscribeFor( 'SCENE_RENDERED', ( { scene } ) => {
    setWaiting( false );
    setScene( 'embedding', reconcile( scene.embedding ) );
    setScene( 'polygons', scene.polygons );
    updateShapes( scene.shapes );
    // logShapes();
  } );

  subscribeFor( 'SHAPE_DEFINED', ( shape ) => {
    addShape( { ...shape, instances: [] } );
    // logShapes();
  } );

  subscribeFor( 'INSTANCE_ADDED', ( instance ) => {
    const { shapeId, orientation } = instance;
    const shape = scene.shapes[ shapeId ];
    const rotation = unwrap( scene.orientations[ (orientation < 0)? 0 : orientation ] );
    setScene( 'shapes', shape.id, 'instances', [ ...shape.instances, { ...instance, rotation } ] );
    // logShapes();
  } );
  
  subscribeFor( 'INSTANCE_REMOVED', ( { shapeId, id } ) => {
    const shape = scene.shapes[ shapeId ];
    const instances = shape.instances .filter( instance => instance.id != id );
    setScene( 'shapes', shape.id, 'instances', instances );
    // logShapes();
  } );
  
  subscribeFor( 'SELECTION_TOGGLED', ( { shapeId, id, selected } ) => {
    // TODO use nested signal
    const shape = scene.shapes[ shapeId ];
    const instances = shape .instances.map( inst => (
      inst.id !== id ? inst : { ...inst, selected }
    ));
    const shapes = { ...scene.shapes, [ shapeId ]: { ...shape, instances } };
    setScene( { ...scene, shapes } );
    // TODO lower ambient light if anything is selected
  } );
  
  subscribeFor( 'TEXT_FETCHED', ( source ) => {
    setSource( source );
  } );
  
  subscribeFor( 'ALERT_RAISED', ( problem ) => {
    setProblem( problem );
    setWaiting( false );
  } );

  const getSceneIndex = ( title ) =>
    {
      if ( !title )
        return 0;
      title = encodeEntities( title ); // was happening in actions.js... still necessary?
      let index;
      if ( title.startsWith( '#' ) ) {
        const indexStr = title.substring( 1 );
        index = parseInt( indexStr );
        if ( isNaN( index ) || index < 0 || index > scenes.length ) {
          console.log( `WARNING: ${index} is not a scene index` );
          index = 0;
        }
      } else {
        index = scenes .map( s => s.title ) .indexOf( title );
        if ( index < 0 ) {
          console.log( `WARNING: no scene titled "${title}"` );
          index = 0;
        }
      }
      return index;
    }
    
  const requestScene = ( name, config ) =>
  {
    const index = getSceneIndex( name );
    if ( index < scenes.length ) {
      setTimeout( () => tweenCamera( scenes[ index ] .camera ) ); // This causes an effect loop if not deferred
      postMessage( selectSnapshot( scenes[ index ] .snapshot, config ) );
    }
  }
  
  const providerValue = {
    scene, setScene, requestDesign, openText, scenes, source, problem, waiting, labels,
    subscribeFor,
    setScenes,
    resetScenes: () => setScenes( [] ),
    setProblem,
    clearProblem: () => setProblem( '' ),
    requestScene,
    requestBOM: () => postMessage( { type: 'BOM_REQUESTED' } ),
  };
  // For the web component, this gives it access to the viewer context
  props.setClient && props.setClient( providerValue );

  return (
    <ViewerContext.Provider value={ providerValue }>
      {props.children}
    </ViewerContext.Provider>
  );
}

const useViewer = () => { return useContext( ViewerContext ); };

export { ViewerProvider, useViewer };
