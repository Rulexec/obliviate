let Game = require('./game.jsx').Game;

exports.gameFlow = gameFlow;

function gameFlow(options) {
  let render = options.render.bind(options, Game),
      wordsProvider = options.wordsProvider;

  function onChoose(data, choice) {
    render({
      isShowingResult: false,
      isWaitingForResult: true,
      isDisabled: true,
      choice: choice,
      word: data
    });

    wordsProvider.checkWordAndGetNextRandomWord(data.wordId, data.choices[choice].id).then(newData => {
      let correctChoice;
      data.choices.some(({id}, i) => {
        if (id === newData.correct) return (correctChoice = i), true;
        else return false;
      });

      render({
        isShowingResult: true,
        isWaitingForResult: false,
        isDisabled: true,
        choice: choice,
        correctChoice: correctChoice,
        word: data,
        onChoose: null,
        onNextWord: render.bind(null, {
          isShowingResult: false,
          isWaitingForResult: false,
          isDisabled: false,
          choice: null,
          correctChoice: null,
          word: newData.word,
          onChoose: onChoose.bind(null, newData.word)
        })
      });
    }, error => {
      console.error(error);
      // TODO: show popup
      render({
        isShowingResult: false,
        isDisabled: false,
        choice: null,
        correctChoice: null,
        word: data,
        onChoose: onChoose.bind(null, data)
      });
    });
  }

  function loadFirstWord() {
    render({isLoading: true});

    wordsProvider.getRandomWord().then(data => {
      render({
        isShowingResult: false,
        isDisabled: false,
        choice: null,
        correctChoice: null,
        word: data,
        onChoose: onChoose.bind(null, data)
      });
    }, error => {
      console.error(error);
      render({
        isError: true,
        refresh: () => loadFirstWord()
      });
    });
  }

  loadFirstWord();
}
