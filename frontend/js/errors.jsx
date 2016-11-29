let React = require('react');

exports.dataLoadErrorRefresh = function(props) {
  return <div>
    <h2 className='ui header dividing'>Ошибка</h2>
    <p>При загрузке данных произошла ошибка, <button className='ui button basic' onClick={props.refresh}><i className='fa fa-refresh'></i> попробовать ещё раз</button></p>
  </div>;
};
