let fs = require('fs');

let indexHtml = fs.readFileSync(__dirname + '/index.html');

var statCountersHtml, forceHttpsHtml;

if (!process.env.LOCAL) {
  statCountersHtml = fs.readFileSync(__dirname + '/statCounters.html');
  forceHttpsHtml = "<script>if (document.location.protocol === 'http:') document.location.protocol = 'https:';</script>";
} else {
  statCountersHtml = '';
  forceHttpsHtml = '';
}

let replaces = {
  STAT_COUNTERS: statCountersHtml,
  FORCE_HTTPS: forceHttpsHtml
};

let chunks = [];

for (let key of Object.keys(replaces)) {
  let value = replaces[key];

  let pos = indexHtml.indexOf(`%${key}%`);
  if (pos === -1) continue;

  chunks.push({
    pos: pos,
    keyLength: key.length + 2,
    value: value
  });
}

chunks.sort(function(a, b) { return a.pos - b.pos; });

let substituted = chunks.reduce(function({result, lastPos}, {pos, keyLength, value}) {
  result.push(indexHtml.slice(lastPos, pos));
  lastPos = pos + keyLength;

  result.push(value);

  return {result, lastPos};
}, {result: [], lastPos: 0});

chunks = substituted.result;
chunks.push(indexHtml.slice(substituted.lastPos));

let rendered = chunks.join('');

fs.writeFileSync(__dirname + '/static/index.html', rendered);
