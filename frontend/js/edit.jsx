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

  onMaybeUpdate() {
    if (!this.state.isDisabled) {
      if (typeof this.props.onUpdate === 'function') {
        this.onUpdate();
      }
    }
  }

  onKeyPress(event) {
    if (event.key === 'Enter') {
      this.onMaybeUpdate();
      event.preventDefault();
    }
  }

  render() {
    let $ = this.props,
        isDisabled = !!this.state.isDisabled,
        isValid = this.state.isValid;

    return (
      <div className={'row ui input' + (this.state.isDeleted ? ' hide' : '') +
                                       (this.props.isNewWord ? ' focus' : '')}>
        <div className={'input-text ui input' + (isValid ? '' : ' error')}>
          <input type='text' defaultValue={$.word} placeholder='слово' maxLength='24'
                 onKeyPress={this.onKeyPress.bind(this)}
                 readOnly={ isDisabled } ref={x => this.wordEl = x} />
        </div>
        <div className={'input-text ui input' + (isValid ? '' : ' error')}>
          <input type='text' defaultValue={$.translation} placeholder='перевод' maxLength='24'
                 onKeyPress={this.onKeyPress.bind(this)}
                 readOnly={ isDisabled } ref={x => this.translationEl = x} />
        </div>
        <button className={'ui button' + (isDisabled ? ' disabled' : '')}
                onClick={this.onMaybeUpdate.bind(this)}>{$.saveButtonText || 'Сохранить'}</button>
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
        onDelete = this.props.onDelete ? memoBind(this, 'onDelete', this.props.onDelete, this.props) : null,

        newWordId = this.props.newWord ? this.props.newWord.id : null;

    return (
      <div className='edit-component'>
        <div className='left-panel'></div>
        <div className='container'>
          <EditWord onUpdate={onUpdate} id={0} withoutDelete saveButtonText='Добавить' />
          { this.props.isLoading ?
              <p style={{marginTop: '1em'}}>Loading...</p> :
              this.props.words.map(({id, word, translation}) =>
                <EditWord key={id} id={id} word={word} translation={translation}
                          isNewWord={id === newWordId}
                          onUpdate={onUpdate} onDelete={onDelete}/>)
          }
        </div>
        <div className='index-panel'>
          <div className='index'>
            {this.props.index.map(x => {
              return <div key={x.value} className={x.active ? 'active' : null}
                      onClick={!x.active ? this.props.onIndex.bind(this.props, x) : null}><span>{x.value}</span></div>
            })}
          </div>
        </div>
      </div>
    );
  }
}

exports.Edit = Edit;
