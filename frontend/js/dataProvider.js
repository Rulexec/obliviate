exports.DataProvider = DataProvider;

let util = require('./util');

function DataProvider(options) {
  let session = options.session,
      onSessionBroken = options.onSessionBroken;

  this.getRandomWord = function() {
    return fetchJSON('words/random');
  };

  this.checkWordAndGetNextRandomWord = function(wordId, choiceId) {
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
  }

  this.updateWord = function(id, word, translation) {
    return fetchJSON('words/' + id, { method: 'POST', body: JSON.stringify([word, translation]) });
  }

  this.createWord = function(word, translation) {
    return fetchJSON('words/', { method: 'POST', body: JSON.stringify([word, translation]) });
  }

  this.loginVk = function(data) {
    return fetchJSON('log/in/vk', { method: 'POST', body: JSON.stringify(data) });
  }

  this.logout = function(token) {
    return fetchJSON('log/out', { method: 'POST', body: token });
  }

  this.getTranslations = function(word) {
    let uri = 'https://dictionary.yandex.net/api/v1/dicservice.json/lookup?key=dict.1.1.20161128T194919Z.e29f78b0008f5e8d.78bb20ee265923dc722cc8e4728fc3a81e41aa82&lang=en-ru&ui=ru&flags=10&text=' + word;

    return fetch(uri).then(response => {
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
    });

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

    return fetch(uri, options).then(response => {
      if (response.status === 200) {
        return response.json();
      } else {
        if (response.status === 401) onSessionBroken();

        return response.json().then(x => Promise.reject(x));
      }
    });
  }
}
