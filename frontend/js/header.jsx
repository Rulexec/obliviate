let React = require('react');

class Header extends React.Component {
  render() {
    let self = this;

    function createItem([id, text]) {
      return <a key={id}
                className={'item' + (self.props.menuItemIsEnabled[id] ? '' : ' disabled') +
                                    (self.props.menuItemIsActive[id] ? ' active' : '')}
                onClick={self.props.menuItemIsEnabled[id] ? self.props.onMenuItemSelected.bind(null, id) : null}>{text}</a>
    }

    let leftItems = [
          ['home', 'Играть'],
          ['verbs', 'Неправильные глаголы'],
          ['stats', 'Статистика'],
          ['duel', 'Дуэли']
        ].map(createItem),
        rightItems = [['edit', 'Редактировать']].map(createItem);

    let onLogin = this.props.loginButtonEnabled ? this.props.onLogin.bind(this.props) : null;

    return (
      <div className='header-component ui secondary pointing menu'>
        {leftItems}
        <div className='right menu'>
          {rightItems}
          {this.props.user ?
            <a className='item disabled'>Выхода нет</a> :

            <a className={'item login' + (this.props.loginButtonEnabled ? '' : ' disabled')}
               onClick={onLogin}><span>Войти</span></a>}
        </div>
      </div>
    );
  }
}

exports.Header = Header;
