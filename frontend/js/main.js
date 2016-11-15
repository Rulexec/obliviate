let DataProvider = require('./dataProvider').DataProvider,

    Game = require('./game.jsx').Game;

let dataProvider = new DataProvider();

if (document.readyState === 'complete') start();
else window.addEventListener('load', start);

function start() {
  let containerEl = document.getElementById('container');

  let props = {
    isShowingResult: false,
    isDisabled: false,
    choice: null,
    correctChoice: null,
    word: null, // will
    onChoose: null // will
  };
  
  function onChoose(data, choice) {
    dataProvider.checkWordAndGetNextRandomWord(data.wordId, data.choices[choice].id).then(newData => {
      let correctChoice;
      data.choices.some(({id}, i) => {
        if (id === newData.correct) return (correctChoice = i), true;
        else return false;
      });

      props = {
        isShowingResult: true,
        isDisabled: true,
        choice: choice,
        correctChoice: correctChoice,
        word: data,
        onChoose: null
      };

      props.onNextWord = function() {
        props = {
          isShowingResult: false,
          isDisabled: false,
          choice: null,
          correctChoice: null,
          word: newData.word,
          onChoose: onChoose.bind(null, newData.word)
        };

        rerender();
      };

      rerender();
    });
  }

  dataProvider.getRandomWord().then(data => {
    document.getElementById('loading').style.display = 'none';

    props.onChoose = onChoose.bind(null, data);
    props.word = data;

    rerender();
  });

  function rerender() {
    ReactDOM.render(
      React.createElement(Game, props),
      containerEl
    );
  }
}
