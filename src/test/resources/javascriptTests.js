//import getHtmlBody from "./bookNode.js"

var fs = require('fs')
var bn = require('../../main/resources/static/bookNode');
var filePath = "test1.html"

var parsedNodes = {
                  	"name": "body",
                  	"children": [
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 0,
                  			"end": 3,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Title of the chapter",
                  					"start": 4,
                  					"end": 23,
                  					"length": 20
                  				}
                  			],
                  			"content": "<p class=\"chapter_title\" id=\"ch1\">",
                  			"start": 4,
                  			"end": 23,
                  			"length": 20
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 24,
                  			"end": 27,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "As all things go, this will be a test chapter. Written in the peaceful dusk of a late autumn afternoon, this text will be used to look into three main functionalities required for an ebook parsing algorithm. Three, the magic number, that is not mere two, and nowhere as far as four main functionalities, but a modest three.",
                  					"start": 28,
                  					"end": 350,
                  					"length": 323
                  				}
                  			],
                  			"content": "<p class=\"simple_text\">",
                  			"start": 28,
                  			"end": 350,
                  			"length": 323
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 351,
                  			"end": 354,
                  			"length": 4
                  		},
                  		{
                  			"name": "h2",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Parsing to tree",
                  					"start": 355,
                  					"end": 369,
                  					"length": 15
                  				}
                  			],
                  			"content": "<h2 id=\"ch1s1\">",
                  			"start": 355,
                  			"end": 369,
                  			"length": 15
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 370,
                  			"end": 373,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Test if we can parse document to a tree of nodes. Leaves hold content. Leaves can be text nodes, images, empty tags and maybe tables.",
                  					"start": 374,
                  					"end": 506,
                  					"length": 133
                  				}
                  			],
                  			"content": "<p class=\"simple_text\">",
                  			"start": 374,
                  			"end": 506,
                  			"length": 133
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 507,
                  			"end": 510,
                  			"length": 4
                  		},
                  		{
                  			"name": "img",
                  			"children": [],
                  			"content": "<img src=\"well_formatted_void_element.png\" id=\"ch1im1\"/>",
                  			"start": 511,
                  			"end": 511,
                  			"length": 1
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 512,
                  			"end": 515,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Images, empty tags for spacing and maybe tables have a size of 1, meaning they can't be split in multiple pages.",
                  					"start": 516,
                  					"end": 627,
                  					"length": 112
                  				}
                  			],
                  			"content": "<p class=\"simple_text\">",
                  			"start": 516,
                  			"end": 627,
                  			"length": 112
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 628,
                  			"end": 631,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Compute sizes to be used in the page computation algorithm and when computing book reading progress.",
                  					"start": 632,
                  					"end": 731,
                  					"length": 100
                  				}
                  			],
                  			"content": "<p class=\"simple_text\">",
                  			"start": 632,
                  			"end": 731,
                  			"length": 100
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 732,
                  			"end": 735,
                  			"length": 4
                  		},
                  		{
                  			"name": "div",
                  			"children": [],
                  			"content": "<div class=\"space_for_effect\">",
                  			"start": 736,
                  			"end": 736,
                  			"length": 1
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 737,
                  			"end": 740,
                  			"length": 4
                  		},
                  		{
                  			"name": "h2",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Splitting the tree into chunks",
                  					"start": 741,
                  					"end": 770,
                  					"length": 30
                  				}
                  			],
                  			"content": "<h2 id=\"ch1s2\">",
                  			"start": 741,
                  			"end": 770,
                  			"length": 30
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 771,
                  			"end": 774,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Functionality for ",
                  					"start": 775,
                  					"end": 792,
                  					"length": 18
                  				},
                  				{
                  					"name": "a",
                  					"children": [
                  						{
                  							"name": "text",
                  							"children": [],
                  							"content": "copying",
                  							"start": 793,
                  							"end": 799,
                  							"length": 7
                  						}
                  					],
                  					"content": "<a href=\"#ch1\">",
                  					"start": 793,
                  					"end": 799,
                  					"length": 7
                  				},
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": " parts of the tree, while keeping the structure of the tree.",
                  					"start": 800,
                  					"end": 859,
                  					"length": 60
                  				}
                  			],
                  			"content": "<p class=\"simple_text\">",
                  			"start": 775,
                  			"end": 859,
                  			"length": 85
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 860,
                  			"end": 863,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Used when splitting the document into pages.",
                  					"start": 864,
                  					"end": 907,
                  					"length": 44
                  				}
                  			],
                  			"content": "<p>",
                  			"start": 864,
                  			"end": 907,
                  			"length": 44
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 908,
                  			"end": 911,
                  			"length": 4
                  		},
                  		{
                  			"name": "img",
                  			"children": [],
                  			"content": "<img src=\"void_element_with_separate_closing_tag.png\" id=\"ch1im2\"></img>",
                  			"start": 912,
                  			"end": 912,
                  			"length": 1
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 913,
                  			"end": 916,
                  			"length": 4
                  		},
                  		{
                  			"name": "h2",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Computing pages",
                  					"start": 917,
                  					"end": 931,
                  					"length": 15
                  				}
                  			],
                  			"content": "<h2 id=\"ch1s3\">",
                  			"start": 917,
                  			"end": 931,
                  			"length": 15
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 932,
                  			"end": 935,
                  			"length": 4
                  		},
                  		{
                  			"name": "img",
                  			"children": [],
                  			"content": "<img src=\"void_element_without_closing_slash.png\" id=\"ch1im3\">",
                  			"start": 936,
                  			"end": 936,
                  			"length": 1
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 937,
                  			"end": 940,
                  			"length": 4
                  		},
                  		{
                  			"name": "p",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "Functionality for finding spaces. Getting the previous and next position of a space will allow us to compute pages without splitting mid-word.",
                  					"start": 941,
                  					"end": 1082,
                  					"length": 142
                  				}
                  			],
                  			"content": "<p class=\"simple_text\">",
                  			"start": 941,
                  			"end": 1082,
                  			"length": 142
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n  ",
                  			"start": 1083,
                  			"end": 1086,
                  			"length": 4
                  		},
                  		{
                  			"name": "table",
                  			"children": [
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "\r\n    ",
                  					"start": 1087,
                  					"end": 1092,
                  					"length": 6
                  				},
                  				{
                  					"name": "tr",
                  					"children": [],
                  					"content": "<tr>\r\n      <th>A</th>\r\n      <th>S</th>\r\n      <th>L</th>\r\n    </tr>",
                  					"start": 1093,
                  					"end": 1093,
                  					"length": 1
                  				},
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "\r\n    ",
                  					"start": 1094,
                  					"end": 1099,
                  					"length": 6
                  				},
                  				{
                  					"name": "tr",
                  					"children": [],
                  					"content": "<tr>\r\n      <td>22</td>\r\n      <td>-</td>\r\n      <td>Lat</td>\r\n    </tr>",
                  					"start": 1100,
                  					"end": 1100,
                  					"length": 1
                  				},
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "\r\n    ",
                  					"start": 1101,
                  					"end": 1106,
                  					"length": 6
                  				},
                  				{
                  					"name": "tr",
                  					"children": [],
                  					"content": "<tr>\r\n      <td>56</td>\r\n      <td>T</td>\r\n      <td>Lon</td>\r\n    </tr>",
                  					"start": 1107,
                  					"end": 1107,
                  					"length": 1
                  				},
                  				{
                  					"name": "text",
                  					"children": [],
                  					"content": "\r\n",
                  					"start": 1108,
                  					"end": 1109,
                  					"length": 2
                  				}
                  			],
                  			"content": "<table style=\"width:100%\" id=\"ch1tbl1\">",
                  			"start": 1087,
                  			"end": 1109,
                  			"length": 23
                  		},
                  		{
                  			"name": "text",
                  			"children": [],
                  			"content": "\r\n",
                  			"start": 1110,
                  			"end": 1111,
                  			"length": 2
                  		}
                  	],
                  	"content": "",
                  	"start": 0,
                  	"end": 1111,
                  	"length": 1112
                  }

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
    testCopySection(tree)
    testCopy(tree)
    testCopy2(tree)
    testConvert(tree)
    testResources(tree)
    testNextTraversal(tree)
    testNextHeader(tree)
})

function testTreeParsingDoesNotLoseInformation(bodyString, tree) {
    console.assert(bodyString == tree.getContent(), "tree parsing loses information")
}

function testContentSize(tree) {
    console.assert(tree.getLength() == 1112, "content size not as expected")
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
    //console.log("testing next leaf")
    var expectedLeaves = inorderLeaves(tree)

    var leaf = tree.nextLeaf()
    var i = 0
    while (leaf != null) {
      //leaf.prettyPrint()
      console.assert(leaf == expectedLeaves[i], "next leaves wrong result")
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
      //leaf.prettyPrint()
      console.assert(leaf == expectedLeaves[i], "previous leaves wrong result")
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
      //console.log(space)
      space = tree.findSpaceAfter(space)
    }
    // we should be able to go over the whole document through conseccutive spaces
    console.assert(space == tree.getDocumentEnd(), "find space after did not finish")
}

function testFindSpaceBefore(tree) {
    var space = tree.findSpaceBefore(tree.getDocumentEnd())

    while (space != 0) {
      //console.log(space)
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

    //console.log(spacesAfter)
    //console.log(spacesBefore)

    console.assert(spacesAfter.length == spacesBefore.length, "number of spaces found in document traversal do not match")
    for (var i = 0; i < spacesAfter.length && i < spacesBefore.length; i++) {
        console.assert(spacesAfter[i] == spacesBefore[i], "spaces at index " + i + " not a match")
    }
}

function testCopySection(tree) {
    var expectedSubtree = "<h2 id=\"ch1s1\">Parsing to tree</h2>\r\n" +
        "  <p class=\"simple_text\">Test if we can parse document to a tree of nodes. Leaves hold content. Leaves can be text nodes, images, empty tags and maybe tables.</p>"

    var part = tree.copy(355, 506)
    //part.prettyPrint()
    //console.log(part.getContent())
    console.assert(part.getContent() == expectedSubtree, "copied subtree does not match expected")
}

function testCopy(tree) {
    var middleChildIndex = Math.floor(tree.children.length / 2)
    var middleChildEnd = tree.children[middleChildIndex].end

    var copy1 = tree.copy(tree.start, middleChildEnd)
    //copy1.prettyPrint()
    var copy2 = tree.copy(middleChildEnd + 1, tree.end)
    //copy2.prettyPrint()

    console.assert(copy1.getContent() + copy2.getContent() == tree.getContent(), "two halves merged do not match original")
}

function testCopy2(tree) {
    var mergedContent = ""
    for (var i = 0; i < tree.children.length; i++) {
        var copiedChild = tree.copy(tree.children[i].start, tree.children[i].end)
        var copiedContent = copiedChild.getContent()
        mergedContent += copiedContent
    }

    console.assert(mergedContent == tree.getContent(), "merged content does not match original content")
}

function testConvert(tree) {
    var tree2 = bn.convert(parsedNodes)
    console.assert(tree2.getContent() == tree.getContent(), "converted content does not match")
}

function testResources(tree) {
    var resources = tree.getResources()
    console.assert(resources.length == 4, "failed to find all resources")
}

function testNextTraversal(tree) {
    let expectedNodes = [['body', 0, 1111],['text', 0, 3],['p', 4, 23],['text', 4, 23],['text', 24, 27],['p', 28, 350],['text', 28, 350],['text', 351, 354],['h2', 355, 369],['text', 355, 369],['text', 370, 373],['p', 374, 506],['text', 374, 506],['text', 507, 510],['img', 511, 511],['text', 512, 515],['p', 516, 627],['text', 516, 627],['text', 628, 631],['p', 632, 731],['text', 632, 731],['text', 732, 735],['div', 736, 736],['text', 737, 740],['h2', 741, 770],['text', 741, 770],['text', 771, 774],['p', 775, 859],['text', 775, 792],['a', 793, 799],['text', 793, 799],['text', 800, 859],['text', 860, 863],['p', 864, 907],['text', 864, 907],['text', 908, 911],['img', 912, 912],['text', 913, 916],['h2', 917, 931],['text', 917, 931],['text', 932, 935],['img', 936, 936],['text', 937, 940],['p', 941, 1082],['text', 941, 1082],['text', 1083, 1086],['table', 1087, 1109],['text', 1087, 1092],['tr', 1093, 1093],['text', 1094, 1099],['tr', 1100, 1100],['text', 1101, 1106],['tr', 1107, 1107],['text', 1108, 1109],['text', 1110, 1111]]
    let current = tree
    let index = 0
    while (current != null) {
        console.assert(expectedNodes[index], "expected node missing for index " + index)
        console.assert(current.name == expectedNodes[index][0], "node name in traversal does not match on index " + index)
        console.assert(current.start == expectedNodes[index][1], "start position in traversal does not match on index " + index)
        console.assert(current.end == expectedNodes[index][2], "end node position in traversal does not match on index " + index)
        current = current.nextNode()
        index = index + 1
    }
    console.assert(index == 55, "missing nodes in traversal")
}

function testNextHeader(tree) {
    let p1 = tree.leafAtPosition(632)
    let h1 = p1.nextNodeOfName("h2")
    console.assert(h1 != null, "next header not found")
    console.assert(h1.start == 741, "next header position incorrect")
    let h2 = h1.nextNodeOfName("h2")
    console.assert(h2 != null, "next header after next header not found")
    console.assert(h2.start == 917, "next header after next header position incorrect")

    let p2 = tree.leafAtPosition(1108)
    let h3 = p2.nextNodeOfName("h2")
    console.assert(h3 == null, "end of document not handled correctly in next header")
}