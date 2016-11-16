let webpack = require('webpack');

module.exports = {
  resolve: {
    modulesDirectories: ['bower_components']
  },

  entry: {
    main: './frontend/js/main.js',
    vendor: './frontend/js/vendor.js'
  },

  output: {
    path: __dirname + '/frontend/static',
    filename: 'bundle.js'
  },
  
  module: {
    loaders: [
      {test: /\.js$/,
       exclude: /(node_modules|bower_components)/,
       loader: 'babel',
       query: {
         presets: ['es2015']
       }},
      {test: /\.jsx$/,
       exclude: /(node_modules|bower_components)/,
       loader: 'babel',
       query: {
         presets: ['react', 'es2015']
       }},

      {test: /\.css$/, loader: 'style-loader!css-loader'},
      {test: /\.(png|eot|woff|woff2|ttf|svg|)$/, loader: 'url-loader' }
    ]
  },
  
  plugins: [
    new webpack.optimize.CommonsChunkPlugin(/* chunkName= */"vendor", /* filename= */"vendor.bundle.js")
  ]
};

if (process.env.LOCAL) {
  delete module.exports.entry.vendor;
  delete module.exports.plugins;
}
