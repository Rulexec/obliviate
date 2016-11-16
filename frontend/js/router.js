exports.Router = Router;

function Router(options) {
  let staticRoutes = {};
  var defaultRoute = null;

  this.addRoutes = newRoutes => {
    Object.keys(newRoutes).forEach(key => {
      let handler = newRoutes[key];

      staticRoutes[key] = handler;
    });
  };
  this.setDefaultRoute = s => defaultRoute = s;

  this.go = s => {
    document.location.hash = s;
    if (s === '') history.replaceState(null, null, '/');

    staticRoutes[s]();
  };

  this.start = () => {
    let url = document.location.hash.slice(1);

    if (url in staticRoutes) {
      staticRoutes[url]();
    } else if (defaultRoute !== null) {
      this.go(defaultRoute);
    }
  };
}
