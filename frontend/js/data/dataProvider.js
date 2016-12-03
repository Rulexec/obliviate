exports.DataProvider = DataProvider;

let util = require('../util'),
    Verbs = require('./verbs').Verbs;

function DataProvider(options) {
  let session = options.session,
      onSessionBroken = options.onSessionBroken;

  this.verbs = new Verbs();

  this.getRandomWord = function() {
    /*{"wordId":108,
       "word":"lol",
       "choices":[
         {"id":"74f38106-c19b-4970-9d7f-7a602f269004","value":"asd"},
         {"id":"0746dc9e-b67c-46fe-8ae7-b241941f11c5","value":"123"},
         {"id":"5c5ce4a4-24ab-4db7-be27-d636eb54cff6","value":"123231"},
         {"id":"9e2bd871-b402-4fd1-9774-3c4ee946aa0f","value":"hgfh"}
       ]
      } */
    return fetchJSON('words/random');
  };

  this.checkWordAndGetNextRandomWord = function(wordId, choiceId) {
    /*{"correct": "<choiceId>",
       "word": <word>}
     */
    return fetchJSON('words/check/' + wordId, {
      method: 'POST',
      body: choiceId
    });
  };

  this.getAllWordsWithTranslations = function() {
    return fetchJSON('words/').then(data => {
      let words = data.map(([id, word, translation]) => {return {id, word, translation}});

      words.sort(function(a, b) {
        if (a.word < b.word)   return -1;
        if (a.word === b.word) return 0;
                               return 1;
      });

      return words;
    });
  };

  this.deleteWord = function(id) {
    return fetchJSON('words/' + id, { method: 'DELETE' });
  };

  this.updateWord = function(id, word, translation) {
    return fetchJSON('words/' + id, { method: 'POST', body: JSON.stringify([word, translation]) });
  };

  this.createWord = function(word, translation) {
    return fetchJSON('words/', { method: 'POST', body: JSON.stringify([word, translation]) });
  };

  this.loginVk = function(data) {
    return fetchJSON('log/in/vk', { method: 'POST', body: JSON.stringify(data) });
  };

  this.logout = function(token) {
    return fetchJSON('log/out', { method: 'POST', body: token });
  };

  this.sendVerbsEmail = function(email) {
    return fetchJSON('email/', {method: 'POST', body: email});
  };

  let TIMEOUT = 5000;

  this.getTranslations = function(word) {
    let uri = 'https://dictionary.yandex.net/api/v1/dicservice.json/lookup?key=dict.1.1.20161128T194919Z.e29f78b0008f5e8d.78bb20ee265923dc722cc8e4728fc3a81e41aa82&lang=en-ru&ui=ru&flags=10&text=' + word;

    return promiseWithTimeout(fetch(uri).then(response => {
      if (response.status === 200) {
        return response.json().then(data => {
          if (data.code && data.code !== 200) {
            return Promise.reject(data);
          } else {
            return Promise.resolve(transform(data));
          }
        });
      } else {
        return Promise.reject(response);
      }
    }), TIMEOUT);

    function transform(yandex) {
      let defs = yandex.def;
      let figures = {};

      defs.forEach(x => {
        let translations = util.mapGetOrSetDefault(figures, x.pos, {figure: x.pos, translations: []}).translations;

        x.tr.forEach(y => {
          let translation = {variants: [], examples: []};

          translation.variants.push(y.text);
          y.syn && y.syn.forEach(syn => translation.variants.push(syn.text));

          y.ex && y.ex.forEach(exs => {
            translation.examples.push({text: exs.text, variants: exs.tr.map(ex => ex.text)});
          });

          translations.push(translation);
        });

      });

      let result = {word: word, figuresOfSpeech: []};

      util.forEachOwnProperty(figures, key => result.figuresOfSpeech.push(figures[key]));

      return result;
    }
  }

  function fetchJSON(uri, options) {
    options || (options = {});
    options.headers || (options.headers = new Headers());

    let token = session.getToken();
    if (token) {
      options.headers.append('X-Auth-Token', token);
    }

    return promiseWithTimeout(fetch(uri, options).then(response => {
      if (response.status === 200) {
        return response.json();
      } else {
        if (response.status === 401) onSessionBroken();

        return response.json().then(x => Promise.reject(x));
      }
    }), TIMEOUT);
  }

  function promiseWithTimeout(promise, timeout) {
    return new Promise((resolve, reject) => {
      let isHandled = false;

      let timeoutId = setTimeout(function() {
        if (isHandled) return;

        isHandled = true;
        reject('timeout');
      }, timeout);

      promise.then(function() {
        if (isHandled) return;

        isHandled = true;

        resolve.apply(this, arguments);
      }, function() {
        if (isHandled) {
          console.error('promiseWithTimeout rejected');
          console.error(arguments);
          return;
        }

        isHandled = true;

        reject.apply(this, arguments);
      });
    });
  };
}
