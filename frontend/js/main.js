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
  const MENU_ITEMS = ['home', 'stats', 'duel', 'edit'];
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
          switch (menuItem) {
          case 'home':
            router.go('');
            break;
          case 'edit':
          case 'stats':
          case 'duel':
            router.go(menuItem);
            break;
          default: throw 'unknown menu item: ' + menuItem
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
    isLoading: true
  });

  function rerender(words) {
    render({
      words: words,
      onDelete: word => {
        dataProvider.deleteWord(word.id).then(() => word.onDeleted())
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
              words.unshift({id: data.id, word: word.word, translation: word.translation});
              rerender(words);
            } else if (data.error === 'validation') {
              word.validationError(true)
            } else {
              alert('TODO: Error: ' + data);
            }
          });
        }
      }
    });
  }

  dataProvider.getAllWordsWithTranslations().then(words => rerender(words));
}
