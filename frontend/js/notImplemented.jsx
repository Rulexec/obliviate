let React = require('react');

class NotImplemented extends React.Component {
  render() {
    return (
      <div className='not-implemented-component'>
        <i className='fa fa-wrench'></i> Находится в разработке, stay tuned.
      </div>
    );
  }
}

exports.NotImplemented = NotImplemented
