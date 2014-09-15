# TuneSwarm Monitor

## Start

To start the monitor:

- First install dependencies (once):

        npm install

- Start the server:

        node server.js
  
  Command line parameters:
  
  - `--conductor URL`
    Provide a custom url for the conductor agent:

        node server.js --conductor ws://localhost:8082/ws/conductor
  
  - `--random`
    The server will generate a random note once a second for simulation and 
    testing purposes.
        
- Open the interface in the browser:

        http://localhost:3000


## Events

A note:

    {
      "note": "C4", 
      "duration": 400, 
      "start": "2014-09-11T15:39:33.827Z"
    }
