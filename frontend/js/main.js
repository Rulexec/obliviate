require('../css/index.less');

let React = require('react'),
    ReactDOM = require('react-dom'),

    DataProvider = require('./dataProvider').DataProvider,

    Router = require('./router').Router,

    Header = require('./header.jsx').Header,
    Game = require('./game.jsx').Game,
    Edit = require('./edit.jsx').Edit,
    NotImplemented = require('./notImplemented.jsx').NotImplemented;

let dataProvider = new DataProvider();

if (document.readyState === 'complete') start();
else window.addEventListener('load', start);

function start() {
  let headerEl = document.getElementById('header'),
      containerEl = document.getElementById('container'),
      footerEl = document.getElementById('footer');

  var unmountHandler = unmount => unmount();

  let headerButtonsState = {
    menuItemIsEnabled: {},
    menuItemIsActive: {}
  };
  const MENU_ITEMS = ['home', 'verbs', 'stats', 'duel', 'edit'];
  function chooseMenuItem(item) {
    MENU_ITEMS.forEach(x => {
      headerButtonsState.menuItemIsEnabled[x] = true;
      headerButtonsState.menuItemIsActive[x] = false;
    });
    headerButtonsState.menuItemIsEnabled[item] = false;
    headerButtonsState.menuItemIsActive[item] = true;
  }
  chooseMenuItem('home');

  let flowOptions = {
    setUnmountHandler: handler => {
      unmountHandler = unmount => {
        unmountHandler = unmount => unmount();
        handler(unmount);
      };
    },

    render: (component, props) => ReactDOM.render(React.createElement(component, props), containerEl)
  };

  let router = new Router();
  router.addRoutes({
    ['']() { unmountHandler(() => {
      chooseMenuItem('home'); renderHeader();

      ReactDOM.render(React.createElement('noscript'), containerEl)
      gameFlow(flowOptions);
    }) }, edit() { unmountHandler(() => {
      chooseMenuItem('edit'); renderHeader();

      editFlow(flowOptions)
    }) }, verbs() { unmountHandler(() => {
      chooseMenuItem('verbs'); renderHeader();
      flowOptions.render(NotImplemented, {});
    }) }, stats() { unmountHandler(() => {
      chooseMenuItem('stats'); renderHeader();
      flowOptions.render(NotImplemented, {});
    }) }, duel() { unmountHandler(() => {
      chooseMenuItem('duel'); renderHeader();
      flowOptions.render(NotImplemented, {});
    }) }
  });
  router.setDefaultRoute('');

  document.getElementById('loading').style.display = 'none';
  router.start();

  function renderHeader(props) {
    props || (props = {});

    let defaultProps = {
      onMenuItemSelected: menuItem => {
        unmountHandler(() => {
          if (menuItem === 'home') {
            router.go('');
          } else {
            router.go(menuItem);
          }
        });
      }
    };

    let mixedProps = Object.assign({}, defaultProps, headerButtonsState, props);

    ReactDOM.render(React.createElement(Header, mixedProps), headerEl);
  }
}

function gameFlow(options) {
  let render = options.render.bind(options, Game);

  function onChoose(data, choice) {
    render({
      isShowingResult: false,
      isWaitingForResult: true,
      isDisabled: true,
      choice: choice,
      word: data
    })

    dataProvider.checkWordAndGetNextRandomWord(data.wordId, data.choices[choice].id).then(newData => {
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
    });
  }

  dataProvider.getRandomWord().then(data => {
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

function editFlow(options) {
  let render = options.render.bind(options, Edit);

  render({
    isLoading: true,
    index: []
  });

  function rerender(allWords, selectedFilter, newWord) {
    let index = buildIndex(allWords),
        words = allWords;
    
    newWord || (newWord = null);

    if (index.length > 0) {
      if (typeof selectedFilter !== 'number') {
        if (typeof selectedFilter === 'string') {
          selectedFilter = selectedFilter.toLowerCase();

          index.some((x, i) => {
            if (isWordSatisfiesFilter(selectedFilter, x.value.toLowerCase())) {
              selectedFilter = i;
              return true;
            } else return false;
          });
        } else {
          selectedFilter = 0;
        }
      }

      index[selectedFilter].active = true;
      let filter = index[selectedFilter].value.toLowerCase();

      words = allWords.filter(x => {
        return isWordSatisfiesFilter(x.word, filter)
      });

      function isWordSatisfiesFilter(word, filter) {
        return word.slice(0, filter.length).toLowerCase() === filter;
      }
    }

    let wordsCount = words.length;

    render({
      words: words,
      newWord: newWord,
      index: index,
      onDelete: word => {
        allWords.some((x, i) => x.id === word.id && (allWords.splice(i, 1), true));

        dataProvider.deleteWord(word.id).then(() => {
          wordsCount--;

          if (wordsCount === 0) {
            rerender(allWords);
          } else {
            word.onDeleted();
          }
        });
      },
      onUpdate: word => {
        if (word.id !== 0) {
          dataProvider.updateWord(word.id, word.word, word.translation).then(() => word.onUpdated());
        } else { // create
          dataProvider.createWord(word.word, word.translation).then(data => {
            word.onUpdated();
            word.validationError(false)

            if (data.error === null && typeof data.id === 'number') {
              word.clearFields();

              // TODO: we can use binary search here
              let i = 0;
              allWords.some((x, j) => {
                if (x.word.toLowerCase() > word.word.toLowerCase()) return true;
                else return (j = i), false;
              });
              let newWord = {id: data.id, word: word.word, translation: word.translation};
              allWords.splice(i, 0, newWord);

              wordsCount++;

              selectedFilter = word.word;

              rerender(allWords, selectedFilter, newWord);
            } else if (data.error === 'validation') {
              word.validationError(true)
            } else {
              alert('TODO: Error: ' + data);
            }
          });
        }
      },
      onIndex(indexItem) {
        rerender(allWords, indexItem.id);
      }
    });
  }

  function buildIndex(words) {
    // TODO: do it recursively, if some bucket have too much words

    let buckets = new Map();

    words.forEach(x => {
      let c = x.word[0].toLowerCase();

      let list = buckets.get(c);
      if (!list) buckets.set(c, list = []);

      list.push(x);
    });

    let result = [];

    for (let [key, value] of buckets) {
      key = key[0].toUpperCase() + key.slice(1);

      result.push({
        value: key,
        active: false
      });
    }

    result.sort(function(a, b) {
      if (a.value < b.value)   return -1;
      if (a.value === b.value) return 0;
                               return 1;
    });

    result.forEach((x, i) => x.id = i);

    return result;
  }

  dataProvider.getAllWordsWithTranslations().then(words => rerender(words));
}
