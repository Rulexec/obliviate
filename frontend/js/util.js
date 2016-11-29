exports.memoBind = memoBind;
exports.debounce = debounce;

let MEMOBINDSYMBOL = Symbol('memoBind');
function memoBind(mem, id, func, ...bindArgs) {
  let cache = mem[MEMOBINDSYMBOL];

  if (typeof cache === 'undefined') {
    mem[MEMOBINDSYMBOL] = cache = Object.create(null);

    let bound = func.bind.apply(func, bindArgs);
    cache[id] = bound;
    
    return bound;
  }

  let cached = cache[id];

  if (typeof cached === 'undefined') {
    cache[id] = cached = func.bind.apply(func, bindArgs);
  }

  return cached;
}

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
