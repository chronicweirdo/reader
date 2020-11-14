//import getHtmlBody from "./bookNode.js"

var fs = require('fs')
var bn = require('./bookNode');
var filePath = "test1.html"

fs.readFile(filePath, 'utf8', function (err, data) {
    if (err) {
        return console.log(err);
    }
    var body = bn.getHtmlBody(data)
    console.assert(body != null && body.length > 0, "body is empty")

    var tree = bn.parse(data)
    console.assert(tree != null, "tree parse failed")
    tree.prettyPrint()

    testTreeParsingDoesNotLoseInformation(body, tree)
    testContentSize(tree)
})

function testTreeParsingDoesNotLoseInformation(bodyString, tree) {
    console.assert(bodyString == tree.getContent(), "tree parsing loses information")
}

function testContentSize(tree) {
    console.assert(tree.getLength() == 1090, "content size not as expected")
}

function testRegex(html) {
    var bodyStartPattern = /<cody[^>]*>/
    var match = bodyStartPattern.exec(html)
    console.log(match)
    console.log(match.index + " " + (match.index  + match[0].length));
    console.log(match[0])
}