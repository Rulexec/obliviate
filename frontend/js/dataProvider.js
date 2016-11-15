exports.DataProvider = DataProvider;

function DataProvider(options) {
  this.getRandomWord = function() {
    return fetch('words/random').then(response => {
      if (response.status === 200) {
        return response.json();
      } else {
        return Promise.reject(['status not 200', response]);
      }
    });
  };

  this.checkWordAndGetNextRandomWord = function(wordId, choiceId) {
    let correct = wordId;

    return this.getRandomWord().then(data => {
      return {correct: correct, word: data};
    });
  };
}
