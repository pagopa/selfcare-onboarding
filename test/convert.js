var fs = require('fs');

fs.readFile('test/test-result.json', 'utf8', function (err, data) {
    if (err) throw err; // we'll not consider error handling for now
    var obj = JSON.parse(data);
    var run = obj["run"]
    result = {}
    result["stats"] = run["stats"]
    let json = JSON.stringify(result);
    fs.writeFile("test/result.json", json, 'utf8', (err) => {
      if (err) {
          console.error('Error writing to file', err);
      } else {
          console.log('Data written to file');
      }
  });
});