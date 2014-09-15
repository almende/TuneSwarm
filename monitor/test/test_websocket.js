var WebSocket = require('ws');
var ws = new WebSocket('ws://localhost:8082/ws/conductor?id=monitor');

ws.on('open', function() {
  console.log('opened');
  ws.send(JSON.stringify({
    jsonrpc: 2,
    method: 'registerAgent',
    params: {}
  }));
});
ws.on('message', function(data, flags) {
  console.log('message received', data);
});
