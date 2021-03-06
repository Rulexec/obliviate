require('../css/index.less');

let React = require('react'),
    ReactDOM = require('react-dom'),

    DataProvider = require('./data/dataProvider').DataProvider,

    Session = require('./session').Session,

    Router = require('./router').Router,

    Header = require('./header.js').Header,
    Edit = require('./edit.js').Edit,
    gameFlow = require('./game.js').gameFlow,

    VerbsReact = require('./verbs.jsx').Verbs,

    NotImplemented = require('./notImplemented.jsx').NotImplemented;

if (document.readyState === 'complete') start();
else window.addEventListener('load', start);

function start() {
  let containerEl = document.getElementById('container'),
      footerEl = document.getElementById('footer');

  var unmountHandler = unmount => unmount();

  let session = new Session();

  let user = session.getUser();

  let router = new Router();

  let dataProvider = new DataProvider({
    session,
    onSessionBroken: () => {
      forgetAuth();

      router.go('');
    }
  });

  function forgetAuth() {
    user = null;
    let token = session.getToken();
    session.forgetAuth();

    header.render();

    return token;
  }

  let header = new Header({
    el: document.getElementById('header'),
    getUser() { return user; },
    getUnmountHandler() { return unmountHandler; },
    router: router,

    onLogout() {
      let token = forgetAuth();

      dataProvider.logout(token);

      router.go('');
    }
  });

  let flowOptions = {
    setUnmountHandler: handler => {
      unmountHandler = unmount => {
        unmountHandler = unmount => unmount();
        handler(unmount);
      };
    },

    dataProvider: dataProvider,

    render: (component, props) => {
      return ReactDOM.render(React.createElement(component, props), containerEl);
    }
  };

  router.addRoutes({
    ['']() { unmountHandler(() => {
      header.chooseMenuItem('home'); header.render();

      ReactDOM.render(React.createElement('noscript'), containerEl)
      gameFlow(Object.assign({}, flowOptions, {
        wordsProvider: {
          checkWordAndGetNextRandomWord: dataProvider.checkWordAndGetNextRandomWord.bind(dataProvider),
          getRandomWord: dataProvider.getRandomWord.bind(dataProvider)
        }
      }));
    }) }, edit() {
      if (!user) { router.go(''); return; }

      unmountHandler(() => {
        header.chooseMenuItem('edit'); header.render();

        // TODO: save current controller, call .onUnmount on unmounting, remove unmountHandler at all
        new Edit(flowOptions).start();
      })
    }, verbs() { unmountHandler(() => {
      header.chooseMenuItem('verbs'); header.render();

      ReactDOM.render(React.createElement('noscript'), containerEl)
      gameFlow(Object.assign({}, flowOptions, {
        isVerbs: true,
        wordsProvider: dataProvider.verbs
      }));
    }) }, stats() { unmountHandler(() => {
      header.chooseMenuItem('stats'); header.render();
      flowOptions.render(NotImplemented, {});
    }) }, duel() { unmountHandler(() => {
      header.chooseMenuItem('duel'); header.render();
      flowOptions.render(NotImplemented, {});
    }) }
  });
  router.setDefaultRoute('');

  document.getElementById('loading').style.display = 'none';
  router.start();

  window.addEventListener('message', function(event) {
    let data = event.data;

    if (data !== undefined && data.type === 'auth') {
      if (data.auth === 'vk' && typeof data.code === 'string') {
        let responseReceived = false;

        header.loginButtonEnable(false);

        dataProvider.loginVk({code: data.code}).then(loginData => {
          if (loginData.user && typeof loginData.user.token === 'string') {
            user = session.createUser({
              token: loginData.user.token,
              expiresAt: loginData.user.expiresAt,
              id: loginData.user.id
            });

            router.go('');
          } else {
            console.log('login failed');
            console.error(loginData);
          }

          header.loginButtonEnable(true);
        }, error => {
          console.error(error);
          header.loginButtonEnable(true, {notLogined: true});
        });
      }
    }
  });
}
