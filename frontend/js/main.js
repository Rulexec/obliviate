let DataProvider = require('./dataProvider').DataProvider,

    Header = require('./header.jsx').Header,
    Game = require('./game.jsx').Game,

    Edit = require('./edit.jsx').Edit;

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
    homeIsEnabled: false,
    editIsEnabled: true
  };

  let flowOptions = {
    events: events,

    header: {
      enableHomeButton(flag) {
        headerButtonsState.homeIsEnabled = flag;
        renderHeader();
      },
      enableEditButton(flag) {
        headerButtonsState.editIsEnabled = flag;
        renderHeader();
      }
    },

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
      onHome: () => {
        unmountHandler(() => {
          flowOptions.header.enableHomeButton(false);
          document.getElementById('loading').style.display = 'block';
          gameFlow(flowOptions);
        });
      },
      onEdit: () => {
        unmountHandler(() => {
          editFlow(flowOptions)
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
    dataProvider.checkWordAndGetNextRandomWord(data.wordId, data.choices[choice].id).then(newData => {
      let correctChoice;
      data.choices.some(({id}, i) => {
        if (id === newData.correct) return (correctChoice = i), true;
        else return false;
      });

      render({
        isShowingResult: true,
        isDisabled: true,
        choice: choice,
        correctChoice: correctChoice,
        word: data,
        onChoose: null,
        onNextWord: render.bind(null, {
          isShowingResult: false,
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
  let render = options.render.bind(options, Edit);

  options.header.enableHomeButton(true);
  options.header.enableEditButton(false);
  options.setUnmountHandler(unmount => {
    console.log('unmounting');
    options.header.enableEditButton(true);
    unmount();
  });

  render({});
}
