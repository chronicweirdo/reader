//import getHtmlBody from "./bookNode.js"

var fs = require('fs')
var bn = require('./bookNode');
var filePath = "test1.html"

fs.readFile(filePath, 'utf8', function (err, data) {
    if (err) {
        return console.log(err);
    }
    console.log(data)
    //testRegex(data)
    var body = bn.getHtmlBody(data)
    console.log(body)
})

function testRegex(html) {
    var bodyStartPattern = /<cody[^>]*>/
    var match = bodyStartPattern.exec(html)
    console.log(match)
    console.log(match.index + " " + (match.index  + match[0].length));
    console.log(match[0])
}