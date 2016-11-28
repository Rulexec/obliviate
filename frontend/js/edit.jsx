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
      word: self.refs.word.value,
      translation: self.refs.translation.value,

      onUpdated() {
        self.setState({isDisabled: false});
      },
      
      clearFields() {
        self.refs.word.value = '';
        self.refs.translation.value = '';
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

  onKeyPress(elementName, event) {
    if (event.key === 'Enter') {
      this.onMaybeUpdate();
      event.preventDefault();
      return;
    }
  }

  onWordChange() {
    if (this.refs.word.value.length > 0) {
      this.props.getDictContainer().showDict(true);
    } else {
      this.props.getDictContainer().showDict(false);
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
                 onChange={this.onWordChange.bind(this)}
                 readOnly={ isDisabled } ref='word' />
        </div>
        <div className={'input-text ui input' + (isValid ? '' : ' error')}>
          <input type='text' defaultValue={$.translation} placeholder='перевод' maxLength='24'
                 onKeyPress={this.onKeyPress.bind(this)}
                 readOnly={ isDisabled } ref='translation' />
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

class EditWordsOrShowDict extends React.Component {
  constructor(props, context) {
    super(props, context);

    this.state = {
      isDict: false
    };
  }

  showDict(flag) {
    this.setState({isDict: flag});
  }

  render() {
    return <div className='words-container'>
      { this.state.isDict ?
        <span>Dict</span> :
        this.props.words.map(({id, word, translation}) =>
          <EditWord key={id} id={id} word={word} translation={translation}
                    isNewWord={id === this.props.newWordId}
                    onUpdate={this.props.onUpdate} onDelete={this.props.onDelete}/>) }
    </div>
  }
}

class Edit extends React.Component {
  constructor(props, context) {
    super(props, context);

    let self = this;

    this.getDictContainer = function() { return self.refs.dictContainer; };
  }

  render() {
    let onUpdate = this.props.onUpdate ? memoBind(this, 'onUpdate', this.props.onUpdate, this.props) : null,
        onDelete = this.props.onDelete ? memoBind(this, 'onDelete', this.props.onDelete, this.props) : null,

        newWordId = this.props.newWord ? this.props.newWord.id : null;

    return (
      <div className='edit-component'>
        <div className='left-panel'></div>
        <div className='container'>
          <EditWord onUpdate={onUpdate} id={0} getDictContainer={this.getDictContainer} withoutDelete saveButtonText='Добавить' />
          { this.props.isLoading ?
              <p style={{marginTop: '1em'}}>Loading...</p> :
              <EditWordsOrShowDict ref='dictContainer' onUpdate={onUpdate} onDelete={onDelete} newWordId={newWordId} words={this.props.words} />
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
