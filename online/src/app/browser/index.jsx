
import { render } from 'solid-js/web';
import { ErrorBoundary } from "solid-js";

import Typography from '@suid/material/Typography'
import Link from '@suid/material/Link'

import { VZomeAppBar } from '../classic/components/appbar.jsx';
import { DesignBrowser } from './browser.jsx';
import { WorkerStateProvider } from '../../workerClient/context.jsx';

const Browser = () => (
  <ErrorBoundary fallback={err => <div>{err.toString()}</div>} >
    <WorkerStateProvider>
      <VZomeAppBar oneDesign={true} title='Browser'
        about={ <>
          <Typography gutterBottom>
            vZome Online Browser shows all of
            the <Link target="_blank" rel="noopener" href="https://vzome.com/home/">vZome</Link> designs
            uploaded and available in
            a <Link target="_blank" rel="noopener" href="https://vzome.github.io/vzome//sharing.html">vZome sharing</Link> GitHub repository.
            By default, my own designs are indexed.
            To see another user's designs, add a query parameter in your browser's address bar.
            (You must know their GitHub username.)
            Here are some examples:
          </Typography>
          <ul>
            <li>
              <Link target="_blank" rel="noopener" href="https://www.vzome.com/app/browser/?user=john-kostick">https://www.vzome.com/app/browser/?user=john-kostick</Link>
            </li>
            <li>
              <Link target="_blank" rel="noopener" href="https://www.vzome.com/app/browser/?user=david-hall">https://www.vzome.com/app/browser/?user=david-hall</Link>
            </li>
          </ul>
          <Typography gutterBottom>
            Click on any entry in the list to view the design interactively.
            Use your mouse to rotate, zoom, and pan the view.
            Note: some designs may take considerably longer than others to render.
          </Typography>
        </> } />
      <DesignBrowser />
    </WorkerStateProvider>
  </ErrorBoundary>
)

render( Browser, document.getElementById( 'root' ) );
