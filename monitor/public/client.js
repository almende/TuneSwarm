var notes = new vis.DataSet();
var timeline;
var moment = vis.moment;

var DELAY = 100; // ms
var NOTES = {
  C4: 0,
  D4: 1,
  E4: 2,
  F4: 3,
  G4: 5,
  A4: 6,
  B4: 7,
  C5: 8,
  D5: 9
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
    data.height = NOTES[data.note] * 20; // TODO: calculate the exact height of the note-bulb in pixels
    notes.add(data);
  });
  socket.on('time', function (time) {
    // sync the visualization with the servers time
    timeline.setCurrentTime(time);
  });
});
