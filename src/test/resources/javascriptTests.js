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
    testLeafAtPosition(tree)
    testFindSpaceAfter(tree)
    testFindSpaceBefore(tree)
    testFindSpacesBothWays(tree)
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

function inorderLeavesWeighted(node) {
    if (node.children.length == 0) {
        var result = []
        for (var i = 0; i < node.getLength(); i++) {
            result.push(node)
        }
        return result
    } else {
        var result = []
        for (var i = 0; i < node.children.length; i++) {
            result = result.concat(inorderLeavesWeighted(node.children[i]))
        }
        return result
    }
  }

function testLeafAtPosition(tree) {
    var treeLength = tree.getLength()
    var leavesAtPositions = inorderLeavesWeighted(tree)
    console.assert(treeLength == leavesAtPositions.length, "tree and weighted leaves lengths do not match")

    for (var i = 0; i < tree.getLength(); i++) {
      var leaf = tree.leafAtPosition(i)
      console.assert(leaf == leavesAtPositions[i], "leaf at position do not match expected")
    }
}

function testFindSpaceAfter(tree) {
    var space = tree.findSpaceAfter(0)
    while (space != tree.getDocumentEnd()) {
      console.log(space)
      space = tree.findSpaceAfter(space)
    }
    // we should be able to go over the whole document through conseccutive spaces
    console.assert(space == tree.getDocumentEnd(), "find space after did not finish")
}

function testFindSpaceBefore(tree) {
    var space = tree.findSpaceBefore(tree.getDocumentEnd())

    while (space != 0) {
      console.log(space)
      space = tree.findSpaceBefore(space)
    }
    // we should be able to go over the whole document through preceeding spaces
    console.assert(space == tree.getDocumentStart(), "find space before did not finish")
}

function testFindSpacesBothWays(tree) {
    var spacesAfter = []
    var spacesBefore = []

    var space = tree.findSpaceAfter(tree.getDocumentStart())
    while (space != tree.getDocumentEnd()) {
      spacesAfter.push(space)
      space = tree.findSpaceAfter(space)
    }

    space = tree.findSpaceBefore(tree.getDocumentEnd())
    while (space != tree.getDocumentStart()) {
      spacesBefore.unshift(space) // prepending
      space = tree.findSpaceBefore(space)
    }

    console.log(spacesAfter)
    console.log(spacesBefore)

    console.assert(spacesAfter.length == spacesBefore.length, "number of spaces found in document traversal do not match")
    for (var i = 0; i < spacesAfter.length && i < spacesBefore.length; i++) {
        console.assert(spacesAfter[i] == spacesBefore[i], "spaces at index " + i + " not a match")
    }
}