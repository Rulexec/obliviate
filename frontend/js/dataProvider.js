exports.DataProvider = DataProvider;

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

        return Promise.reject(response.json());
      }
    });
  }
}
