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
    testNextLeaf(tree)
    testPreviousLeaf(tree)
})

function testTreeParsingDoesNotLoseInformation(bodyString, tree) {
    console.assert(bodyString == tree.getContent(), "tree parsing loses information")
}

function testContentSize(tree) {
    console.assert(tree.getLength() == 1090, "content size not as expected")
}

function inorderLeaves(node) {
    if (node.children.length == 0) {
        return [node]
    } else {
        var result = []
        for (var i = 0; i < node.children.length; i++) {
            result = result.concat(inorderLeaves(node.children[i]))
        }
        return result
    }
}

function testNextLeaf(tree) {
    console.log("testing next leaf")
    var expectedLeaves = inorderLeaves(tree)

    var leaf = tree.nextLeaf()
    var i = 0
    while (leaf != null) {
      leaf.prettyPrint()
      console.assert(leaf == expectedLeaves[i], "next leafs wrong result")
      leaf = leaf.nextLeaf()
      i = i + 1
    }
    console.assert(i == expectedLeaves.length, "did not check all next leaves")
}

function getLastLeaf(node) {
    var current = node
    while (current.children.length > 0) {
        current = current.children[current.children.length - 1]
    }
    return current
}

function testPreviousLeaf(tree) {
    var expectedLeaves = inorderLeaves(tree)
    var leaf = getLastLeaf(tree)
    var i = expectedLeaves.length - 1
    while (leaf != null) {
      leaf.prettyPrint()
      console.assert(leaf == expectedLeaves[i], "previous leafs wrong result")
      leaf = leaf.previousLeaf()
      i = i - 1
    }
    console.assert(i == -1, "did not check all previous leaves")
}
