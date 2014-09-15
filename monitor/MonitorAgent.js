var eve = require('evejs');
var open = require('open');
var express = require('express');

var MAX_HISTORY = 10000;

/**
 * A MonitorAgent, logging events from the ConductorAgent and visualizes them
 * in a web application.
 * @param {string} id
 * @param {{port: number}} params
 * @private
 */
function MonitorAgent(id, params) {
  eve.Agent.call(this, id);

  // extend the agent with rpc functionality
  this.extend('rpc', ['onNote']);

  // connect to all transports provided by the system
  this.connect(eve.system.transports.getAll());

  this._initServer(params);
}

MonitorAgent.prototype = Object.create(eve.Agent.prototype);
MonitorAgent.prototype.constructor = MonitorAgent;

/**
 * Initialize a web application server
 * @param {{port: number}} params
 * @private
 */
MonitorAgent.prototype._initServer = function (params) {
  // create an express application
  this.app = express();
  this.app.use('/', express.static(__dirname + '/public'));
  this.app.use('/node_modules', express.static(__dirname + '/node_modules'));
  this.server = require('http').Server(this.app);
  this.io = require('socket.io')(this.server);

  // start a server
  this.port = params && params.port || 3000;
  this.server.listen(this.port);
  console.log('Server listening on http://localhost:' + this.port);
  //open('http://localhost:' + this.port); // automatically open the web app in the browser

  // history with all notes
  this.notes = [];

  var me = this;
  this.io.on('connection', function (socket) {
    // emit the servers time, so the client can sync with that
    socket.emit('time', new Date().toISOString());

    // emit all logs from history
    me.notes.forEach(function(note) {
      socket.emit('note', note);
    });
  });
};

/**
 * Log a music note
 * @param {note: string, duration: number, start: string} note
 */
MonitorAgent.prototype.onNote = function(note) {
  this.notes.push(note);
  console.log(JSON.stringify(note));

  // remove notes when history gets too large
  while (this.notes.length > MAX_HISTORY) this.notes.shift();

  // emit to all connected clients
  var connections = this.io.sockets.connected;
  for (var id in connections) {
    if (connections.hasOwnProperty(id)) {
      connections[id].emit('note', note);
    }
  }

  return 'roger';
};

module.exports = MonitorAgent;
