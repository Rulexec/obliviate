let memoBind = require('./util').memoBind;

class EditWord extends React.Component {
  render() {
    return (
      <div className='row ui input'>
        <input type='text' placeholder='word' maxLength='24' />
        <input type='text' placeholder='translation' maxLength='24' />
        <button className='ui button'>Save</button>
        {this.props.withDelete ? <button className='ui basic button'><i class='fa fa-trash'></i></button> : null}
      </div>
    );
  }
}

class Edit extends React.Component {
  render() {
    return (
      <div className='edit-component'>
        <EditWord withDelete={false} />
      </div>
    );
  }
}

exports.Edit = Edit;
