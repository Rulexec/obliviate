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
        if (this.handleSaveLock()) { // validation
          this.onUpdate();
        }
      }
    }
  }

  onKeyPress(event) {
    if (event.key === 'Enter') {
      this.onMaybeUpdate();
      event.preventDefault();
      return;
    }
  }

  onWordChange() {
    let text = this.refs.word.value;

    this.validate('word');

    this.props.onWordChange && this.props.onWordChange(text);
  }
  onTranslateChange() {
    this.validate('translation');
  }

  validate(name) {
    let item = this.needToValidateMap[name],
        value = item.getValue(),
        validator = item.validator,
        el = item.el;

    let isValid;

    if (value.length > 0) {
      isValid = validator(value);

      if (isValid) {
        el.classList.remove('error');
      } else {
        el.classList.add('error');
      }
    } else {
      isValid = false;
      el.classList.remove('error');
    }

    this.needToValidateMap[name].valid = isValid;

    isValid ? this.handleSaveLock() : this.lockSaveButton();
  }

  handleSaveLock() {
    let formIsValid = this.needToValidate.every(x => x.valid);
    
    if (formIsValid) {
      this.refs.saveButton.classList.remove('disabled');
    } else {
      this.lockSaveButton();
    }

    return formIsValid;
  }
  lockSaveButton() {
    this.refs.saveButton.classList.add('disabled');
  }
  
  componentDidMount() {
    this.setUpValidation(this.props.validations);
  }
  componentWillReceiveProps(props) {
    this.setUpValidation(props.validations);
  }

  setUpValidation(validators) {
    if (!validators) return;

    this.needToValidate = [
      {name: 'word',
       getValue: () => this.refs.word.value,
       validator: validators.word,
       el: this.refs.wordContainer,
       valid: validators.word(this.refs.word.value)},
      {name: 'translation',
       getValue: () => this.refs.translation.value,
       validator: validators.translation,
       el: this.refs.translationContainer,
       valid: validators.translation(this.refs.translation.value)}
    ];
    let map = this.needToValidateMap = {};

    this.needToValidate.forEach(x => map[x.name] = x);

    this.handleSaveLock();
  }

  changeTranslationText(text) {
    this.refs.translation.value = text;
    this.validate('translation');
  }

  render() {
    let $ = this.props,
        isDisabled = !!this.state.isDisabled,
        isValid = this.state.isValid;

    return (
      <div className={'row ui input' + (this.state.isDeleted ? ' hide' : '') +
                                       (this.props.isNewWord ? ' focus' : '')}>
        <div ref='wordContainer' className={'input-text ui input' + (isValid ? '' : ' error')}>
          <input type='text' defaultValue={$.word} placeholder='слово' maxLength='24'
                 onKeyPress={this.onKeyPress.bind(this)}
                 onChange={this.onWordChange.bind(this)}
                 readOnly={ isDisabled } ref='word' />
        </div>
        <div ref='translationContainer' className={'input-text ui input' + (isValid ? '' : ' error')}>
          <input type='text' defaultValue={$.translation} placeholder='перевод' maxLength='24'
                 onKeyPress={this.onKeyPress.bind(this)}
                 onChange={this.onTranslateChange.bind(this)}
                 readOnly={ isDisabled } ref='translation' />
        </div>
        <button ref='saveButton' className={'ui button' + (isDisabled ? ' disabled' : '')}
                onClick={this.onMaybeUpdate.bind(this)}>{$.saveButtonText || 'Сохранить'}</button>
        {this.props.withoutDelete ? null :
          <button className={'ui basic button' + (isDisabled ? ' disabled' : '')}
                  onClick={isDisabled || !$.onDelete ? null : this.onDelete.bind(this)}><i className='fa fa-trash'></i></button>}
      </div>
    );
  }
}

function Translation(props) {
  let data = props.translation;

  function joinWithComma(list, fn) {
    let result = [];

    list.forEach((x, i) => {
      result.push(fn(x, i));

      if (i !== list.length - 1) result.push(', ');
    });

    return result;
  }

  function createSynonyms(synonyms) {
    return joinWithComma(synonyms, (x, i) =>
      <span key={i} onClick={props.onSelected.bind(props, x)} className='synonym'>{x}</span>);
  }

  function createExamples(examples) {
    let result = [];

    examples.forEach((x, i) => {
      result.push(<span key={i} className='example'><span className='example-text'>{x.text}</span> → {
        joinWithComma(x.variants, (x, i) => <span key={i} className='example-variant'>{x}</span>)
      }</span>);

      if (i !== examples.length - 1) result.push(', ');
    });

    return result;
  }

  return <div className='dict-translation'>
    <h2 className='ui dividing header'>{data.word}</h2>
    {data.figuresOfSpeech.length > 0 ?
      <div className='ui list'>{ data.figuresOfSpeech.map(figure =>
        <div key={figure.figure} className='item'>
          <span className='figure-of-speech'>{figure.figure}.</span>
          <div>
            <div className='ui ordered list'>{ figure.translations.map((translation, i) =>
              <div key={i} className='item translation-variant'>
                <div className='synonyms'>
                  {createSynonyms(translation.variants)}
                </div>
                {translation.examples.length > 0 ? <div className='examples'>
                  {createExamples(translation.examples)}
                </div> : null}
              </div>
            )}</div>
          </div>
        </div>
      )}</div>
      
      :

      <p>Перевода нет</p>}

    <div className='yandex-dictionary-text'>Реализовано с помощью сервиса <a href='https://tech.yandex.ru/dictionary/' style={{color: 'red'}}>«Яндекс.Словарь»</a></div>
  </div>;
}

class EditWordsOrShowDict extends React.Component {
  constructor(props, context) {
    super(props, context);

    let self = this;

    this.state = {
      isDict: false,
      isLoading: false
    };

    this.onTranslationSelected = function(text) {
      self.props.onTranslationSelected(text);
    };
  }

  changeDictState(state) {
    this.setState(state);
  }

  render() {
    return <div className='words-container'>
      { this.state.isDict ?
        (this.state.isLoading ? <span><i className='fa fa-spinner'></i> Loading...</span> :
                                <Translation translation={this.state.translation} onSelected={this.onTranslationSelected} />) :
        this.props.words.map(({id, word, translation}) =>
          <EditWord key={id} id={id} word={word} translation={translation}
                    validations={this.props.validations}
                    isNewWord={id === this.props.newWordId}
                    onUpdate={this.props.onUpdate} onDelete={this.props.onDelete}/>) }
    </div>
  }
}

class Edit extends React.Component {
  constructor(props, context) {
    super(props, context);

    let self = this;

    this.onWordChange = function(text) {
      self.props.onNewWordChange(text);
    };
    this.onTranslationSelected = function(text) {
      self.refs.creation.changeTranslationText(text);
    };

    this.onUpdate = function() { self.props.onUpdate.apply(self.props, arguments); };
    this.onDelete = function() { self.props.onDelete.apply(self.props, arguments); };

    this.state = {isShowingIndex: true};
  }

  changeDictState(state) {
    this.setState({isShowingIndex: !state.isDict});

    this.refs.dictContainer.changeDictState(state);
  }

  render() {
    let newWordId = this.props.newWord ? this.props.newWord.id : null,
        onUpdate = this.onUpdate,
        onDelete = this.onDelete;

    return (
      <div className='edit-component'>
        <div className='left-panel'></div>
        <div className='container'>
          <EditWord ref='creation'
              onUpdate={onUpdate} id={0} onWordChange={this.onWordChange}
              validations={this.props.validations}
              withoutDelete saveButtonText='Добавить' />
          { this.props.isLoading ?
              <p style={{marginTop: '1em'}}>Loading...</p> :
              <EditWordsOrShowDict ref='dictContainer'
                  validations={this.props.validations}
                  onUpdate={onUpdate} onDelete={onDelete}
                  onTranslationSelected={this.onTranslationSelected}
                  newWordId={newWordId} words={this.props.words} />
          }
        </div>
        <div className='index-panel'>
          <div className='index'>
            {this.state.isShowingIndex ? this.props.index.map(x => {
              return <div key={x.value} className={x.active ? 'active' : null}
                      onClick={!x.active ? this.props.onIndex.bind(this.props, x) : null}><span>{x.value}</span></div>
            }) : null}
          </div>
        </div>
      </div>
    );
  }
}

exports.Edit = Edit;
