let http = require('http');

http.createServer((req, res) => {
  res.setHeader('Content-Type', 'text/plain; charset=utf-8');
  res.end('Производятся технические работы, ожидайте.\n\n' +
          'Если вы видите эту надпись, значит, у нас слишком мало пользователей, чтобы совоокупляться с бесшовными обновлениями. В будущем вы сильно вряд ли её увидите.');
}).listen(process.env.PORT);
