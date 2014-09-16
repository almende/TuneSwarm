var eve = require('evejs');
var DataSet = require('vis/lib/DataSet');

var MAX_NOTE_COUNT = 1000;

/**
 * A MonitorAgent, logging events from the ConductorAgent and visualizes them
 * in a web application.
 * @param {string} id
 * @private
 */
function MonitorAgent(id) {
  eve.Agent.call(this, id);

  this.notes = new DataSet();

  // extend the agent with rpc functionality
  this.extend('rpc', ['onNote']);

  // connect to all transports provided by the system
  this.connect(eve.system.transports.getAll());
}

// Extend eve.Agent
MonitorAgent.prototype = Object.create(eve.Agent.prototype);
MonitorAgent.prototype.constructor = MonitorAgent;

/**
 * Log a music note
 * @param {{note: string, duration: number, start: string}} data
 */
MonitorAgent.prototype.onNote = function(data) {
  console.log(JSON.stringify(data));

  // add the new note to the dataset
  data.height = this.NOTES[data.note] * 33 / 2 - 19; // at scale 50%
  data.img = data.duration > 1500 ? 'note1.png' : data.duration > 500 ? 'note2.png' : 'note4.png';
  this.notes.add(data);

  // remove notes when too many
  var all = this.notes.get({order: 'start'});
  if (all.length > MAX_NOTE_COUNT) {
    var old = all.splice(0, all.length - MAX_NOTE_COUNT);
    this.notes.remove(old);
  }
};

// enum all used notes
MonitorAgent.prototype.NOTES = {
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

module.exports = MonitorAgent;
