let memoBind = require('./util').memoBind;

class Choice extends React.Component {
  choose() {
    this.props.onChoose();
  }

  render() {
    return (
      <div className={'choice' +
                      (this.props.isBlinking ? ' blink' : '') +
                      (this.props.isDisabled ? ' disabled' : '') +
                      (this.props.isCorrect ? ' correct' : this.props.isIncorrect ? ' incorrect' : '')}
           onClick={this.props.isDisabled ? null : this.choose.bind(this)}>
        <span>{this.props.value}</span>
      </div>
    );
  }
}

class Game extends React.Component {
  constructor(props, context) {
    super(props, context);

    this._showingResultTimeout = null;
  }

  componentDidUpdate() {
    let self = this;

    if (this.props.isShowingResult) {
      this._showingResultTimeout !== null && clearTimeout(this._showingResultTimeout);
      this._showingResultTimeout = setTimeout(() => {
        clearTimeout(self._showingResultTimeout);
        self._showingResultTimeout = null;

        self.props.onNextWord();
      }, 1500);
    }
  }
  componentWillUnmount() {
    this._showingResultTimeout !== null && clearTimeout(this._showingResultTimeout);
    this._showingResultTimeout = null;
  }

  choose(choice) {
    this.props.onChoose(choice);
  }

  render() {
    let choices = [];
    for (let i = 0; i < 4; i++) {
      let choice =
        <Choice {...this.props.word.choices[i]} key={i}
                isDisabled={this.props.isDisabled}
                isBlinking={!this.props.isShowingResult && this.props.choice === i}
                isCorrect={this.props.correctChoice === i}
                isIncorrect={this.props.isShowingResult && this.props.choice === i && this.props.choice !== this.props.correctChoice}
                onChoose={memoBind(this, 'choose' + i, this.choose, this, i)} />;
      choices.push(choice);
    }

    return (
      <div className='game'>
        <div className='word-box'><span>{this.props.word.word}</span></div>
        <div className='choices'>
          {choices}
        </div>
      </div>
    );
  }
}

exports.Choice = Choice;
exports.Game = Game;
