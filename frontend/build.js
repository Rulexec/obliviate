let fs = require('fs');

const STAT_COUNTERS = '%STAT_COUNTERS%';

let indexHtml = fs.readFileSync(__dirname + '/index.html'),
    statCountersPos = indexHtml.indexOf(STAT_COUNTERS);

var statCountersHtml;

if (!process.env.LOCAL) statCountersHtml = fs.readFileSync(__dirname + '/statCounters.html');
else statCountersHtml = '';

let rendered = [
  indexHtml.slice(0, statCountersPos), 
  statCountersHtml,
  indexHtml.slice(statCountersPos + STAT_COUNTERS.length)
].join('');

fs.writeFileSync(__dirname + '/static/index.html', rendered);
