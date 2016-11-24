exports.DataProvider = DataProvider;

function DataProvider(options) {
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

  function fetchJSON() {
    return fetch.apply(null, arguments).then(response => {
      if (response.status in {'200': true, '400': true}) {
        return response.json();
      } else {
        return Promise.reject(['status not 200,400', response]);
      }
    });
  }
}
