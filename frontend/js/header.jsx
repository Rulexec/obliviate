let React = require('react');

class Header extends React.Component {
  render() {
    let self = this;

    function onClick(id) {
      if (!self.props.menuItemIsEnabled[id]) return null;
      else return function(event) {
        if (event.button !== 0) return;

        self.props.onMenuItemSelected.call(self.props, id);

        event.preventDefault();
      };
    }

    function createItem([id, text]) {
      return <a key={id}
                href={'#' + id}
                className={'item' + (self.props.menuItemIsEnabled[id] ? '' : ' disabled') +
                                    (self.props.menuItemIsActive[id] ? ' active' : '')}
                onClick={onClick(id)}>{text}</a>
    }

    let rightItemsSource;
    if (this.props.user) rightItemsSource = [['edit', 'Редактировать']]
    else rightItemsSource = [];

    let leftItems = [
          ['home', 'Играть'],
          ['verbs', 'Неправильные глаголы'],
          ['stats', 'Статистика'],
          ['duel', 'Дуэли']
        ].map(createItem),
        rightItems = rightItemsSource.map(createItem);

    let onLogin = this.props.loginButtonEnabled ? this.props.onLogin.bind(this.props) : null;

    return (
      <div className='header-component ui secondary pointing menu'>
        {leftItems}
        <div className='right menu'>
          {rightItems}
          {this.props.user ?
            <a className='item login'
               onClick={this.props.onLogout.bind(this.props)}>
              <span>Выйти</span>
            </a> :

            <a className={'item login' + (this.props.loginButtonEnabled ? '' : ' disabled')}
               onClick={onLogin}><span>Войти</span></a>}
        </div>
      </div>
    );
  }
}

exports.Header = Header;
