let webpack = require('webpack');

module.exports = {
  resolve: {
    modulesDirectories: ['node_modules', 'bower_components']
  },

  entry: {
    main: './frontend/js/main.js',
    vendor: './frontend/js/vendor.js'
  },

  output: {
    path: __dirname + '/frontend/static',
    filename: '/bundle/bundle.js'
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
      {test: /\.less$/, loader: "style-loader!css-loader!less-loader"},
      {test: /\.(png|eot|woff|woff2|ttf|svg)(?:\?v=.+?)?$/, loader: 'file-loader?name=./bundle/bin/[hash].[ext]' }
    ]
  },
  
  plugins: [
    new webpack.optimize.CommonsChunkPlugin(/* chunkName= */"vendor", /* filename= */"bundle/vendor.bundle.js"),
    new webpack.optimize.UglifyJsPlugin({
      compress: {
        warnings: false
      }
    }),
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify('production')
      }
    })
  ]
};

if (process.env.LOCAL) {
  delete module.exports.entry.vendor;
  delete module.exports.plugins;
}
