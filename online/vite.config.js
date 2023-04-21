
/*
I'm trying a switch from esbuild to Vite for several reasons:

1. SolidJS community and tools is centered on Vite
2. esbuild as a bundler is not a common choice, and there's not much info nor ecosystem
3. esbuild is maintained only by Evan Wallace

Status:

The build command (here as "yarn ship") is finishing without errors, but it is not clear
that I'll have sufficient control over chunk (module) names.  (See final conclusion.)

The dev server (here as "yarn dev") runs without initial stdout errors, but both
the main 'online' app and the 'classic' app produce errors.  The latter is due just
to the missing SolidJS compiling, but the former is more concerning.  Here is the console log:

Warning: Invalid hook call. Hooks can only be called inside of the body of a function component. This could happen for one of the following reasons:
1. You might have mismatching versions of React and the renderer (such as React DOM)
2. You might be breaking the Rules of Hooks
3. You might have more than one copy of React in the same app
See https://reactjs.org/link/invalid-hook-call for tips about how to debug and fix this problem.
printWarning @ react.development.js:209
react.development.js:1622 Uncaught TypeError: Cannot read properties of null (reading 'useState')
    at useState (react.development.js:1622:21)
    at _$$component.location (index.jsx:248:21)
    at @solid-refresh:25:42
    at untrack (dev.js:421:12)
    at HMRComp.createMemo.name [as fn] (@solid-refresh:25:28)
    at runComputation (dev.js:691:22)
    at updateComputation (dev.js:676:3)
    at createMemo (dev.js:238:10)
    at [solid-refresh]WorkerContext (@solid-refresh:22:20)
    at dev.js:515:12

Also, I have to navigate to "/public/" to view the online app.  Looks like I'd have to
setup the proxy configuration to avoid that, or reorg public and src.

I'm currently using the multi-page app approach to bundling multiple endpoints.
If I pick this up again, I'll try to bundle as multiple libraries ("lib" mode), to be closer
to what esbuild is doing, without any massaging of HTML at all, but it is unclear whether
the dev server will even work that way, since there is no bundling in that case.

For now, I'm abandoning this approach, mostly due to the wildly different output for the dev server
and build, but also because it feels like too much complexity to achieve an equivalent to what
I have with esbuild, building multiple apps and bundling and splitting all in one step, really quickly,
with very high dev server fidelity.

If I come back, it will be because of difficulty integrating some dev tools like solid-debugger, I suspect.

*/

import { resolve } from 'path';
import { defineConfig } from 'vite';
import solidPlugin from 'vite-plugin-solid';

export default defineConfig({
  publicDir: '/public',
  assetsInclude: ['**/*.vef'],
  plugins: [solidPlugin()],
  server: {
    port: 3001,
  },
  build: {
    target: 'esnext',
    rollupOptions: {
      input: {
        // These are good enough for bundling entry points (for build), but don't help with serving
        online: resolve(__dirname, 'public/index.html'),
        classic: resolve(__dirname, 'public/classic/index.html'),
      },
    },
  },
});
