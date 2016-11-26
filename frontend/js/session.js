let User = require('./user').User;

exports.Session = Session;

function Session() {
  let self = this;

  this.getToken = function() {
    return localStorage.getItem('authToken') || null;
  };

  this.getUser = function() {
    let token = localStorage.getItem('authToken');

    if (typeof token !== 'string') return null;

    let expiresAt = parseInt(localStorage.getItem('expiresAt'));
    if (isNaN(expiresAt) || expiresAt < Date.now()) {
      self.forgetAuth();
      return null;
    }

    let userId = parseInt(localStorage.getItem('userId'));

    return new User({
      id: userId
    });
  };

  this.createUser = function(options) {
    let token = options.token;

    if (typeof token !== 'string') throw new Error('No token');

    localStorage.setItem('authToken', token);
    localStorage.setItem('expiresAt', options.expiresAt);
    localStorage.setItem('userId', options.id);

    return new User({
      id: options.id
    });
  };

  this.forgetAuth = function() {
    localStorage.removeItem('authToken');
    localStorage.removeItem('expiresAt');
    localStorage.removeItem('userId');
  };
}
