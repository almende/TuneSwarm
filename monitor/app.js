var eve = require('evejs');
var vis = require('vis');
var Handlebars = require('handlebars');
var MonitorAgent = require('./MonitorAgent');
var QueryParams = require('./asset/QueryParams');
var moment = vis.moment;
var params = new QueryParams();

var CONDUCTOR_AGENT_URL = params.getValue('conductor') || 'ws://localhost:8082/ws/conductor';
var MONITOR_AGENT_URL = 'monitor';
var RECONNECT_DELAY = 10000; // ms
var DELAY = 1000; // ms

window.addEventListener('load', function () {
  console.log('conductor agent url:', CONDUCTOR_AGENT_URL);
  console.log('monitor agent url:  ', MONITOR_AGENT_URL);

  var monitorAgent = new MonitorAgent(MONITOR_AGENT_URL);
  var conductorProxyAgent = createConductorAgentProxy();
  // create a timeline
  var container = document.getElementById('timeline-container');
  var options = {
    start: moment().add(-10, 'seconds'),
    end: moment().add(10, 'seconds'),
    template: Handlebars.compile(document.getElementById('note-template').innerHTML),
    type: 'box',
    height: '100%',
    stack: false,
    showMinorLabels: false,
    showMajorLabels: false
  };
  var timeline = new vis.Timeline(container, monitorAgent.notes, options);

  var running = false;
  function renderStep() {
    // move the window (you can think of different strategies).
    var now = timeline.getCurrentTime();
    var range = timeline.getWindow();
    var interval = range.end - range.start;
    switch (document.getElementById('time').value) {
      case 'continuous':
        // continuously move the window
        timeline.setWindow(now - interval, now, {animate: false});
        requestAnimationFrame(renderStep);
        running = true;
        break;

      case 'discrete':
        timeline.setWindow(now - interval, now, {animate: false});
        setTimeout(renderStep, DELAY);
        running = true;
        break;

      case 'oscilloscope':
        // move the window 90% to the left when now is larger than the end of the window
        if (now > range.end) {
          timeline.setWindow(now.valueOf() - 0.1 * interval, now.valueOf() + 0.9 * interval);
        }
        setTimeout(renderStep, DELAY);
        running = true;
        break;

      default: // 'off'
        running = false;
        // do nothing...
        break;
    }
  }
  renderStep();

  // bind events to buttons
  document.getElementById('fit').onclick = function () {
    if (document.getElementById('time').value == 'off') {
      timeline.fit()
    }
    else {
      alert('Please set time following "Off"');
    }
  };
  document.getElementById('now').onclick = function () {
    if (document.getElementById('time').value == 'off') {
      timeline.moveTo(timeline.getCurrentTime());
    }
    else {
      alert('Please set time following "Off"');
    }
  };
  document.getElementById('time').onchange = function () {
    if (!running) {
      renderStep();
    }
  };

  // emit a random note once a second
  if (params.getValue('random') != undefined) {
    var nr = 0;
    var arr = Object.keys(monitorAgent.NOTES);
    setInterval(function () {
      var now = new Date().toISOString();
      var duration = Math.pow(2, Math.round(Math.random() * 4)) * 100;
      //var note = arr[Math.floor(Math.random() * arr.length)];
      var note = arr[nr];
      nr = (nr + 1) % 9;

      monitorAgent.onNote({'note': note, duration: duration, start: now});
    }, 1000);
  }

  // expose properties on window for debugging purposes
  window.monitorAgent = monitorAgent;
  window.conductorProxyAgent = conductorProxyAgent;
  window.timeline = timeline;
  window.vis = vis;
  window.params = params;
});

/**
 * Create a proxy agent and open a websocket for communication with the conductorAgent.
 * TODO: this is redundant as soon as evejs supports websockets
 * @returns {eve.Agent}
 */
function createConductorAgentProxy() {
  // create a sort of a proxy agent for the conductorAgent
  // TODO: replace this with a websocket transport as soon as this is implemented in evejs
  var conductorProxyAgent = new eve.Agent(CONDUCTOR_AGENT_URL);
  conductorProxyAgent.extend('rpc', []);
  conductorProxyAgent.connect(eve.system.transports.getAll());

  // open a websocket pass incoming messages via the conductorAgent to the monitorAgent
  function connect() {
    var ws = new WebSocket(CONDUCTOR_AGENT_URL + '?id=' + MONITOR_AGENT_URL);
    ws.onopen = function () {
      console.log('Connected to the conductor agent');
    };
    ws.onmessage = function (event) {
      console.log('received', event.data);
      var rpc = JSON.parse(event.data);
      conductorProxyAgent.request('monitor', rpc)
          .then(function (result) {
            ws.send(JSON.stringify({id: rpc.id, result: result, error: null}));
          })
          .catch(function (err) {
            ws.send(JSON.stringify({id: rpc.id, result: null, error: err.toString()}));
          });
    };
    ws.onclose = function (err) {
      console.log('Error: Connection to the conductor agent closed');
      reconnect();
    };
    ws.onerror = function (err) {
      console.log('Error: Failed to connect to the conductor agent');
      reconnect();
    };

    // expose properties on window for debugging purposes
    window.ws = ws;
  }

  var reconnectTimer = null;
  function reconnect() {
    if (reconnectTimer == null) {
      console.log('Reconnecting in ' + (RECONNECT_DELAY / 1000) + ' seconds...');
      reconnectTimer = setTimeout(function () {
        reconnectTimer = null;
        connect();
      }, RECONNECT_DELAY);
    }
  }

  // open a web socket to the conductor agent now
  connect();

  return conductorProxyAgent;
}
