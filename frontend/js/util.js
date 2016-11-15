exports.memoBind = memoBind;

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
