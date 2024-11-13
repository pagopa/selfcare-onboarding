'use strict';
var fs = require('fs');
const axios = require('axios').default;

fs.readFile('test/test-result.json', 'utf8', function (err, data) {
    if (err) throw err; 
    var obj = JSON.parse(data);
    sendToSlack(obj["run"]["stats"], process.argv[2]);
});


async function sendToSlack(payload, url) {
    //console.log(payload);

    var block =
    {
      "blocks": [
        {
          "type": "section",
          "text": {
            "type": "mrkdwn",
            "text": "*:white_check_mark: GitHub Actions*"
          }
        },
        {
          "type": "section",
          "fields": [
            {
              "type": "mrkdwn",
              "text": "*Iterations*:\ntotal: "+payload["iterations"]["total"]+" pending: "+payload["iterations"]["pending"]+" failed: "+payload["iterations"]["failed"]
            },
            {
                "type": "mrkdwn",
                "text": "*Items*:\ntotal: "+payload["items"]["total"]+" pending: "+payload["items"]["pending"]+" failed: "+payload["items"]["failed"]
                },
                {
                "type": "mrkdwn",
                "text": "*Scripts*:\ntotal: "+payload["scripts"]["total"]+" pending: "+payload["scripts"]["pending"]+" failed: "+payload["scripts"]["failed"]
                },
                {
                "type": "mrkdwn",
                "text": "*Prerequests*:\ntotal: "+payload["prerequests"]["total"]+" pending: "+payload["prerequests"]["pending"]+" failed: "+payload["prerequests"]["failed"]
                },
                {
                "type": "mrkdwn",
                "text": "*Requests*:\ntotal: "+payload["requests"]["total"]+" pending: "+payload["requests"]["pending"]+" failed: "+payload["requests"]["failed"]
                },
                {
                "type": "mrkdwn",
                "text": "*Tests*:\ntotal: "+payload["tests"]["total"]+" pending: "+payload["tests"]["pending"]+" failed: "+payload["tests"]["failed"]
                },
                {
                "type": "mrkdwn",
                "text": "*Assertions*:\ntotal: "+payload["assertions"]["total"]+" pending: "+payload["assertions"]["pending"]+" failed: "+payload["assertions"]["failed"]
                },
                {
                "type": "mrkdwn",
                "text": "*TestScripts*:\ntotal: "+payload["testScripts"]["total"]+" pending: "+payload["testScripts"]["pending"]+" failed: "+payload["testScripts"]["failed"]
                },
                {
                "type": "mrkdwn",
                "text": "*PrerequestScripts*:\ntotal: "+payload["prerequestScripts"]["total"]+" pending: "+payload["prerequestScripts"]["pending"]+" failed: "+payload["prerequestScripts"]["failed"]
                },
          ]
        }
      ]
    }

    payload = JSON.stringify(block)
    try {
     
      await axios.post(url, payload, {headers: {
        'Content-Type': 'application/json'
      }});
    } catch (e) {
      const status = e.response.status;
      console.error(`There was an error, HTTP status code: ${status}`);
    }
}

