exports.debounce = debounce;

function debounce(fn, timeout) {
  let lastCall = null,
      lastResult;

  return function() {
    if (lastCall !== null && Date.now() - lastCall < timeout) return (console.log('debounced'), lastResult);

    lastCall = Date.now();
    return (lastResult = fn.apply(this, arguments));
  };
}

exports.mapGetOrSetDefault = function(map, key, def) {
  let value = map[key];

  if (value === undefined) value = map[key] = def;

  return value;
};

exports.forEachOwnProperty = function(object, fn) {
  let i = 0;
  for (let key in object) if (object.hasOwnProperty(key)) {
    fn.call(object, key, i++);
  }
};

exports.shuffleArrayInPlace = function(array) {
  for (let i = 0; i < array.length - 2; i++) {
    let j = i + (Math.random() * (array.length - i) | 0);

    if (i === j) continue;

    // swap
    let x = array[i];
    array[i] = array[j];
    array[j] = x;
  }

  return array;
};
