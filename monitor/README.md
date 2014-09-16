# TuneSwarm Monitor

## Start

Open the application in your browser:

    ./index.html

To connect to a custom conductor agent (default is `ws://localhost:8082/ws/conductor`):

    ./index.html?conductor=ws://localhost:8082/ws/conductor

To let the application generate random notes:

    ./index.html?random


## Build

The web application uses commonjs modules. When changes are made in the code,
the code must be bundled again before it can be used in the browser.

- First install dependencies (once):

        npm install

- Bundle the code using browserify

        npm run build

  This will generate the file `app-bundle.js`, which is loaded by `app.html`.


## Events

A note:

    {
      "note": "C4", 
      "duration": 400, 
      "start": "2014-09-11T15:39:33.827Z"
    }
