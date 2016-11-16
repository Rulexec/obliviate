let memoBind = require('./util').memoBind;

class EditWord extends React.Component {
  render() {
    return (
      <div className='row ui input'>
        <input type='text' value={this.props.word} placeholder='word' maxLength='24' readOnly />
        <input type='text' value={this.props.translation} placeholder='translation' maxLength='24' readOnly />
        <button className='ui button disabled'>{this.props.saveButtonText || 'Save'}</button>
        {this.props.withoutDelete ? null : <button className='ui basic button disabled'><i className='fa fa-trash'></i></button>}
      </div>
    );
  }
}

class Edit extends React.Component {
  render() {
    return (
      <div className='edit-component'>
        <EditWord withoutDelete saveButtonText='Add' />
        { this.props.isLoading ?
            <p style={{marginTop: '1em'}}>Loading...</p> :
            this.props.words.map(({word, translation}) => <EditWord key={word} word={word} translation={translation} />)
        }
      </div>
    );
  }
}

exports.Edit = Edit;
