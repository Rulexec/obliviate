module.exports = {
  entry: "./frontend/js/main.js",
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
       }}
    ]
  }
};
