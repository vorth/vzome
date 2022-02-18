// babel workaround
import "regenerator-runtime/runtime";

import { vZomeViewerCSS } from "./vzome-viewer.css";

import { muiCSS } from "./mui-styles.html";

import { createWorkerStore } from '../ui/viewer/store.js';

export class VZomeViewer extends HTMLElement {
  #root;
  #container;
  #store;
  #url;
  constructor() {
    super();
    this.#root = this.attachShadow({ mode: "open" });

    this.#root.innerHTML = muiCSS; // This is a hack to work around JSS behavior from MUI.
    // Every time I need more styles from MUI, I have to copy them from the debugger element
    //  view of the <head/> and put them in mui-styles.html.js.

    const viewerStyle = document.createElement( "style" );
    viewerStyle.textContent = vZomeViewerCSS;
    this.#root.prepend( viewerStyle );
    this.#container = document.createElement("div");
    this.#root.appendChild( this.#container );

    this.#store = createWorkerStore( this );

    if ( this.hasAttribute( 'src' ) ) {
      const url = this.getAttribute( 'src' );
      if ( ! url.endsWith( ".vZome" ) ) {
        // This is the only case in which we don't resolve the promise with text,
        //  since there is no point in allowing download of non-vZome text.
        alert( `Unrecognized file name: ${url}` );
      }
      else
        this.#url = new URL( url, window.location ) .toString();
        // Get the fetch started by the worker before we load the dynamic module below,
        //  which is pretty big.  I really should encapsulate the message in a function!
        this.#store.dispatch( { type: 'URL_PROVIDED', payload: { url: this.#url, viewOnly: true } } );
    }
  }

  connectedCallback() {
    import( '../ui/viewer/index.jsx' )
      .then( module => {
        this.#reactElement = module.renderViewer( this.#store, this.#container, this.#url );
      })
  }

  #reactElement = null;
  get reactElement() {
    return this.#reactElement;
  }

  static get observedAttributes() {
    return ["src"];
  }

  attributeChangedCallback(
    attributeName,
    _oldValue,
    _newValue
  ) {
    switch (attributeName) {
      case "src":
      this.#url = new URL( _newValue, window.location ) .toString();
      this.#store.dispatch( { type: 'URL_PROVIDED', payload: { url: this.#url, viewOnly: true } } );
    }
  }

  // Reflect the attribute in a JS property.
  set src(newSrc) {
    if (newSrc === null) {
      this.removeAttribute("src");
    } else {
      this.setAttribute("src", newSrc);
    }
  }

  get src() {
    return this.getAttribute("src");
  }
}

customElements.define("vzome-viewer", VZomeViewer);
