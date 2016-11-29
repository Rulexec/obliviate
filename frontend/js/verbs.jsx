let React = require('react');

class Verbs extends React.Component {
  render() {
    let self = this;

    function onEmail() {
      if (self.props.emailIsDisabled) return;

      self.props.onEmail(self.refs.email.value);
    }
    function onEmailKeyPress(event) {
      if (event.key === 'Enter') {
        onEmail();
        event.preventDefault();
      }
    }

    return <div className='verbs'>
      <h2 className='ui header dividing'><i className='fa fa-wrench'></i> Находится в разработке</h2>
      <p>Неправильные глаголы — очень важная часть английского языка, без знания которой невозможно корректно выражать свои мысли.</p>
      <p>Это список из примерно <b>200 глаголов</b>, формы прошлого времени и причастия которого изменяются не по общим правилам склонения глаголов.</p>
      <p>К примеру, глагол <span className='ui basic label green small'>play</span> — правильный, его форма прошедшего времени образуется путём добавления окончания <span className='ui basic label small'>-ed</span> — <span className='ui basic label green small'>played</span>. А <span className='ui basic label red small'>think</span> — неправильный, и нет другого способа выяснить его вторую форму кроме как знать наизусть — <span className='ui basic label red small'>thought</span>.</p>
      <p>И на данный момент cредство для простого, быстрого и эффективного выучивания этих слов является нашей главной целью дальнейшей разработки сервиса.</p>
      <div className='ui piled segment'>
        <p style={{fontSize: '1.2em'}}>Оставьте ваш email, чтобы мы уведомили вас, когда вы сможете воспользоваться данной возможностью:</p>
        <div style={{display: 'flex', flexDirection: 'row-reverse'}}>
          <div className='ui action input big'>
            <input ref='email' type='email' placeholder='name@example.com' onKeyPress={onEmailKeyPress} />
            <button onClick={onEmail}
                    className={'ui teal right labeled icon button' +
                               (this.props.emailIsDisabled ? ' disabled' : '')}>
              <i className={'icon' + (this.props.emailed ? ' checkmark' : ' mail outline')}></i>
              {this.props.emailed ? 'Сохранено' : 'Сохранить'}
            </button>
          </div>
        </div>
      </div>
    </div>;
  }
}

exports.Verbs = Verbs;
