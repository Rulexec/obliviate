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

  function fetchJSON() {
    return fetch.apply(null, arguments).then(response => {
      if (response.status === 200) {
        return response.json();
      } else {
        return Promise.reject(['status not 200', response]);
      }
    });
  }
}