var notes = new vis.DataSet();
var timeline;
var moment = vis.moment;

var MAX_NOTE_COUNT = 1000;
var DELAY = 1000; // ms
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

window.addEventListener('load', function () {
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
  timeline = new vis.Timeline(container, notes, options);

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

      default: // 'not'
        running = false;
        // do nothing...
        break;
    }
  }
  renderStep();

  // bind events to buttons
  document.getElementById('fit').onclick = function () {
    timeline.fit()
  };
  document.getElementById('now').onclick = function () {
    timeline.moveTo(timeline.getCurrentTime());
  };
  document.getElementById('time').onchange = function () {
    if (!running) {
      renderStep();
    }
  };

  // open a websocket, listen for events emitted by the server
  var socket = io.connect(location.href);
  socket.on('note', function (data) {
    console.log(data);

    // add the new note to the dataset
    data.height = NOTES[data.note] * 33 / 2 - 19; // at scale 50%
    data.img = data.duration > 500 ? 'note2.png' : 'note4.png';
    notes.add(data);

    // remove notes when too many
    var all = notes.get({order: 'start'});
    if (all.length > MAX_NOTE_COUNT) {
      var old = all.splice(0, all.length - MAX_NOTE_COUNT);
      notes.remove(old);
    }
  });
  socket.on('time', function (time) {
    // sync the visualization with the servers time
    console.log('Server time:', time);
    timeline.setCurrentTime(time);
  });
});
