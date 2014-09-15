var open = require('open');
var express = require('express');
var app = express();
var server = require('http').Server(app);
var io = require('socket.io')(server);

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

// start a server
var PORT = 3000;
server.listen(PORT);
console.log('Server listening on http://localhost:' + PORT);
open('http://localhost:' + PORT); // automatically open the web app in the browser

app.use('/', express.static(__dirname + '/public'));
app.use('/node_modules', express.static(__dirname + '/node_modules'));

// history with all notes
var notes = [];

io.on('connection', function (socket) {
  // emit the servers time, so the client can sync with that
  socket.emit('time', new Date().toISOString());

  // emit all logs from history
  notes.forEach(function(note) {
    socket.emit('note', note);
  });
});

function logNote(note) {
  notes.push(note);
  console.log(JSON.stringify(note));

  // emit to all connected clients
  var connections = io.sockets.connected;
  for (var id in connections) {
    if (connections.hasOwnProperty(id)) {
      connections[id].emit('note', note);
    }
  }
}

// emit a random note once a second
// TODO: replace this with connecting via the websocket to the conductor agent
//       to listen for real events
var nr = 0;
var arr = Object.keys(NOTES);
setInterval(function () {
  var now = new Date().toISOString();
  var duration = Math.pow(2, Math.round(Math.random() * 4)) * 100;
  //var note = arr[Math.floor(Math.random() * arr.length)];
  var note = arr[nr];
  nr = (nr + 1) % 9;

  logNote({'note': note, duration: duration, start: now});
}, 1000);
