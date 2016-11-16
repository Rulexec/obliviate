let memoBind = require('./util').memoBind;

class Header extends React.Component {
  render() {
    return (
      <div className='header'>
        <div className={'logo' + (this.props.homeIsEnabled ? '' : ' disabled')}
             onClick={this.props.homeIsEnabled ? this.props.onHome.bind(this.props) : null}>Obliviate</div>
        <div className='sections'>
          <a className={this.props.editIsEnabled ? '' : 'disabled'}
          onClick={this.props.editIsEnabled ? this.props.onEdit.bind(this.props) : null}>Edit</a>
        </div>
      </div>
    );
  }
}

exports.Header = Header;
