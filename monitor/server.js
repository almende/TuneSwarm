var argv = require('yargs').argv;
var WebSocket = require('ws');
var eve = require('evejs');
var MonitorAgent = require('./MonitorAgent');

var RECONNECT_DELAY = 10000; // ms
var CONDUCTOR_AGENT_URL = argv.conductor || 'ws://localhost:8082/ws/conductor';
var MONITOR_AGENT_URL = 'monitor';

console.log('conductor agent url:', CONDUCTOR_AGENT_URL);
console.log('monitor agent url:  ', MONITOR_AGENT_URL);

var monitorAgent = new MonitorAgent(MONITOR_AGENT_URL, {port: 3000});

// create a sort of a proxy agent for the conductorAgent
// TODO: replace this with a websocket transport as soon as this is implemented in evejs
var conductorProxyAgent = new eve.Agent(CONDUCTOR_AGENT_URL);
conductorProxyAgent.extend('rpc', []);
conductorProxyAgent.connect(eve.system.transports.getAll());

// open a web socket to the conductor agent now
connect();

function connect() {
// open a websocket pass incoming messages via the conductorAgent to the monitorAgent
  var ws = new WebSocket(CONDUCTOR_AGENT_URL + '?id=' + MONITOR_AGENT_URL);
  ws.on('open', function () {
    console.log('Connected to the conductor agent');
  });
  ws.on('message', function (data, flags) {
    var rpc = JSON.parse(data);
    console.log('received', data);
    conductorProxyAgent.request('monitor', rpc)
        .then(function (result) {
          ws.send(JSON.stringify({id: rpc.id, result: result, error: null}));
        })
        .catch(function (err) {
          ws.send(JSON.stringify({id: rpc.id, result: null, error: err.toString()}));
        });
  });
  ws.on('close', function (err) {
    console.log('Error: Connection to the conductor agent closed');
    reconnect();
  });
  ws.on('error', function (err) {
    console.log('Error: Failed to connect to the conductor agent');
    reconnect();
  });
}

function reconnect() {
  console.log('Reconnecting in ' + (RECONNECT_DELAY / 1000) + ' seconds...');
  setTimeout(connect, RECONNECT_DELAY);
}

if (argv.random) {
// emit a random note once a second
  var NOTES = {
    C4: 0,
    D4: 1,
    E4: 2,
    F4: 3,
    G4: 4,
    A4: 5,
    B4: 6,
    C5: 7,
    D5: 8
  };

  var nr = 0;
  var arr = Object.keys(NOTES);
  setInterval(function () {
    var now = new Date().toISOString();
    var duration = Math.pow(2, Math.round(Math.random() * 4)) * 100;
    //var note = arr[Math.floor(Math.random() * arr.length)];
    var note = arr[nr];
    nr = (nr + 1) % 9;

    monitorAgent.onNote({'note': note, duration: duration, start: now});
  }, 1000);
}
