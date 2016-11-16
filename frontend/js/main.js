let DataProvider = require('./dataProvider').DataProvider,

    Header = require('./header.jsx').Header,
    Game = require('./game.jsx').Game,

    NotImplemented = require('./notImplemented.jsx').NotImplemented;

let dataProvider = new DataProvider();

if (document.readyState === 'complete') start();
else window.addEventListener('load', start);

function start() {
  let headerEl = document.getElementById('header'),
      containerEl = document.getElementById('container'),
      footerEl = document.getElementById('footer');

  let events = new EventEmitter();

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
    events: events,

    setUnmountHandler: handler => {
      unmountHandler = unmount => {
        unmountHandler = unmount => unmount();
        handler(unmount);
      };
    },

    render: (component, props) => ReactDOM.render(React.createElement(component, props), containerEl)
  };

  renderHeader();
  
  gameFlow(flowOptions);

  function renderHeader(props) {
    props || (props = {});

    let defaultProps = {
      onMenuItemSelected: menuItem => {
        unmountHandler(() => {
          chooseMenuItem(menuItem); renderHeader();

          switch (menuItem) {
          case 'home':
            document.getElementById('loading').style.display = 'flex';
            ReactDOM.render(React.createElement('noscript'), containerEl)
            gameFlow(flowOptions);
            break;
          case 'edit':
            editFlow(flowOptions)
            break;
          case 'stats':
          case 'duel':
            flowOptions.render(NotImplemented, {});
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
    document.getElementById('loading').style.display = 'none';

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
  let render = options.render.bind(options, NotImplemented);

  render({});
}
