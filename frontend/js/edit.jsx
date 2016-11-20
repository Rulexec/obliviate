let React = require('react'),

    memoBind = require('./util').memoBind;

class EditWord extends React.Component {
  constructor(props, context) {
    super(props, context)

    this.state = {
      isDisabled: false,
      isDeleted: false,
      isValid: true
    };
  }

  onDelete() {
    let self = this;

    this.setState({isDisabled: true});

    this.props.onDelete({
      id: this.props.id,
      onDeleted: () => {
        self.setState({isDeleted: true});
      }
    });
  }

  onUpdate() {
    let self = this;

    this.setState({isDisabled: true});

    this.props.onUpdate({
      id: self.props.id,
      word: self.wordEl.value,
      translation: self.translationEl.value,

      onUpdated() {
        self.setState({isDisabled: false});
      },
      
      clearFields() {
        self.wordEl.value = '';
        self.translationEl.value = '';
      },

      validationError(flag) {
        self.setState({
          isValid: !flag
        });
      }
    });
  }

  render() {
    let $ = this.props,
        isDisabled = !!this.state.isDisabled,
        isValid = this.state.isValid;

    return (
      <div className={'row ui input' + (this.state.isDeleted ? ' hide' : '')}>
        <div className={'input-text ui input' + (isValid ? '' : ' error')}>
          <input type='text' defaultValue={$.word} placeholder='word' maxLength='24'
                 readOnly={ isDisabled } ref={x => this.wordEl = x} />
        </div>
        <div className={'input-text ui input' + (isValid ? '' : ' error')}>
          <input type='text' defaultValue={$.translation} placeholder='translation' maxLength='24'
                 readOnly={ isDisabled } ref={x => this.translationEl = x} />
        </div>
        <button className={'ui button' + (isDisabled ? ' disabled' : '')}
                onClick={isDisabled || !$.onUpdate ? null : this.onUpdate.bind(this)}>{$.saveButtonText || 'Save'}</button>
        {this.props.withoutDelete ? null :
          <button className={'ui basic button' + (isDisabled ? ' disabled' : '')}
                  onClick={isDisabled || !$.onDelete ? null : this.onDelete.bind(this)}><i className='fa fa-trash'></i></button>}
      </div>
    );
  }
}

class Edit extends React.Component {
  render() {
    let onUpdate = this.props.onUpdate ? memoBind(this, 'onUpdate', this.props.onUpdate, this.props) : null,
        onDelete = this.props.onDelete ? memoBind(this, 'onDelete', this.props.onDelete, this.props) : null;

    return (
      <div className='edit-component'>
        <EditWord onUpdate={onUpdate} id={0} withoutDelete saveButtonText='Add' />
        { this.props.isLoading ?
            <p style={{marginTop: '1em'}}>Loading...</p> :
            this.props.words.map(({id, word, translation}) =>
              <EditWord key={id} id={id} word={word} translation={translation}
                        onUpdate={onUpdate} onDelete={onDelete}/>)
        }
      </div>
    );
  }
}

exports.Edit = Edit;
