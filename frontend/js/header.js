let React = require('react'),
    ReactDOM = require('react-dom'),
    HeaderReact = require('./header.jsx').Header;

exports.Header = Header;

function Header(options) {
  let self = this;

  let headerButtonsState = {
    menuItemIsEnabled: {},
    menuItemIsActive: {}
  };
  const MENU_ITEMS = ['home', 'verbs', 'stats', 'duel', 'edit'];

  this.chooseMenuItem = function(item) {
    MENU_ITEMS.forEach(x => {
      headerButtonsState.menuItemIsEnabled[x] = true;
      headerButtonsState.menuItemIsActive[x] = false;
    });
    headerButtonsState.menuItemIsEnabled[item] = false;
    headerButtonsState.menuItemIsActive[item] = true;
  }
  this.chooseMenuItem('home');

  let headerProps = {
    loginButtonEnabled: true
  };

  this.render = function() {
    let defaultProps = {
      user: options.getUser(),

      onMenuItemSelected: menuItem => {
        options.getUnmountHandler()(() => {
          switch (menuItem) {
          case 'home':
            options.router.go('');
            break;
          case 'verbs':
          case 'edit':
          case 'stats':
          case 'duel':
            options.router.go(menuItem);
            break;
          default: throw 'unknown menu item: ' + menuItem
          }
        });
      },

      onLogin: () => {
        let uri = document.location.protocol + '//' + document.location.host + '/auth/vk';

        let authUri = 'https://oauth.vk.com/authorize?client_id=5740564' +
                      '&state=vk' +
                      '&display=popup&scope=0&response_type=code&v=5.60&redirect_uri=' + uri;

        let left = (window.screen.width - 600) / 2,
            top = (window.screen.height - 400) / 2;
        window.open(authUri, '_blank', `width=600,height=400,left=${left},top=${top}`);

        headerProps.loginButtonEnabled = false;

        self.render();
      }
    };

    let mixedProps = Object.assign({}, defaultProps, headerButtonsState, headerProps);

    ReactDOM.render(React.createElement(HeaderReact, mixedProps), options.el);
  }

  this.loginButtonEnable = (flag) => {
    headerProps.loginButtonEnabled = flag;
    self.render();
  };
}
