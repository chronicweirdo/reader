function BookNode(name, content, parent = null, children = [], start = null, end = null) {
  this.name = name
  this.content = content
  this.parent = parent
  this.children = children
  this.start = start
  this.end = end

  this.addChild = addChild
  this.prettyPrint = prettyPrint
  this.collapseLeafs = collapseLeafs
  this.getContent = getContent
  this.updatePositions = updatePositions
  this.getLength = getLength
  this.nextLeaf = nextLeaf
  this.previousLeaf = previousLeaf
  this.leafAtPosition = leafAtPosition
  this.getRoot = getRoot
  this.getDocumentStart = getDocumentStart
  this.getDocumentEnd = getDocumentEnd
  this.findSpaceAfter = findSpaceAfter
  this.findSpaceBefore = findSpaceBefore
  this.copy = copy
}

var VOID_ELEMENTS = ["area","base","br","col","hr","img","input","link","meta","param","keygen","source"]

function isVoidElement(tagName) {
  return VOID_ELEMENTS.includes(tagName.toLowerCase())
}

var LEAF_ELEMENTS = ["img", "table"]

function shouldBeLeafElement(tagName) {
  return LEAF_ELEMENTS.includes(tagName.toLowerCase())
}

function addChild(node) {
  this.children.push(node)
  node.parent = this
}

function printAtLevel(level, text) {
  var message = ""
  for (var i = 0; i <= level; i++) message += "\t"
  message += text
  console.log(message)
}

function prettyPrint(level = 0) {
  printAtLevel(level, this.name + "[" + this.start + "," + this.end + "]: " + this.content)
  for (var i = 0; i < this.children.length; i++) {
    this.children[i].prettyPrint(level+1)
  }
}

function getHtmlBody(html) {
  var bodyStartPattern = /<body[^>]*>/
  var bodyStartMatch = bodyStartPattern.exec(html)

  var bodyEndPattern = /<\/body\s*>/
  var bodyEndMatch = bodyEndPattern.exec(html)

  if (bodyStartMatch != null && bodyEndMatch != null) {
    var from = bodyStartMatch.index + bodyStartMatch[0].length
    var to = bodyEndMatch.index
    return html.substring(from, to)
  } else {
    return null
  }
}

function isTag(str) {
  return /^<\/?[^>]+>$/.exec(str) != null
}

function isEndTag(str) {
  return /^<\/[^>]+>$/.exec(str) != null
}
  
function isBothTag(str) {
  return /^<[^>\/]+\/>$/.exec(str) != null
}

function getTagName(str) {
  var tagNamePattern = /<\/?([^>\s]+)/
  var match = tagNamePattern.exec(str)
  if (match != null) {
    return match[1]
  }
  return null
}

function getLength() {
  return this.end - this.start + 1
}

function parseBody(body) {
  var bodyNode = new BookNode("body", "")
  var current = bodyNode

  var content = ""

  for (var i = 0; i < body.length; i++) {
    var c = body.charAt(i)

    if (c == '<') {
      // starting a new tag
      // save what we have in content
      if (isTag(content)) throw "this should not happen"
      else {
        // can only be a text node or nothing
        if (content.length > 0) {
          current.addChild(new BookNode("text", content))
          content = ""
        }
      }
    }

    // accumulate content
    content += c

    if (c == '>') {
      // ending a tag
      if (isTag(content)) {
        var name = getTagName(content)
        // can only be a tag
        if (isEndTag(content)) {
          // we check that this tag closes the current node correctly
          if (isVoidElement(name)) {
            // the last child should have the correct name
            var lastChild = current.children[current.children.length - 1]
            if (name != lastChild.name) throw "incompatible end for void tag"
            else {
              lastChild.content += content
            }
          } else {
            // the current node should have the correct name, and it is getting closed
            if (name != current.name) throw "incompatible end tag"
            // move current node up
            current = current.parent
          }
        } else if (isBothTag(content) || isVoidElement(name)) {
          // just add this tag without content
          current.addChild(new BookNode(name, content))
        } else {
          // a start tag
          var newNode = new BookNode(name, content)
          current.addChild(newNode)
          current = newNode
        }
        // reset content
        content = ""
      } else throw "wild > encountered"
    }
  }
  // add the last text node, if there is still such a thing remaining
  if (content.length > 0) {
    if (isTag(content)) throw "this should not happen"
    else {
      // can only be a text node or nothing
      if (content.length > 0) {
        current.addChild(new BookNode("text", content))
      }
    }
  }

  bodyNode.collapseLeafs()
  bodyNode.updatePositions()
  return bodyNode
}

function updatePositions(entrancePosition = 0) {
  var position = entrancePosition
  this.start = position
  if (this.name == "text") {
    this.end = this.start + this.content.length - 1
  } else if (shouldBeLeafElement(this.name)) {
    // occupies a single position
    this.end = this.start
  } else if (this.children.length == 0) {
    // an element without children, maybe used for spacing, should occupy a single position
    this.end = this.start
  } else {
    // compute for children and update
    for (var i = 0; i < this.children.length; i++) {
      var child = this.children[i]
      child.updatePositions(position)
      position = child.end + 1
    }
    this.end = this.children[this.children.length - 1].end
  }
}

function getContent() {
  if (this.name == "text") {
    return this.content
  } else if (this.name == "body") {
    var result = ""
    for (var i = 0; i < this.children.length; i++) {
      result += this.children[i].getContent()
    }
    return result
  } else if (shouldBeLeafElement(this.name) && this.children.length == 0) {
    return this.content
  } else {
    var result = this.content
    for (var i = 0; i < this.children.length; i++) {
      result += this.children[i].getContent()
    }
    result += "</" + this.name + ">"
    return result
  }
}

function collapseLeafs() {
  if (shouldBeLeafElement(this.name) && this.children.length > 0) {
    // extract content from children
    this.content = this.getContent()
    this.children = []
  } else {
    for (var i = 0; i < this.children.length; i++) {
      this.children[i].collapseLeafs()  
    }
  }
}

function parse(html) {
  var body = getHtmlBody(html)
  if (body != null) {
    return parseBody(body)
  }
  return null
}

function nextLeaf() {
  // is this a leaf?
  var current = this
  if (current.children.length == 0) {
    // go up the parent line until we find next sibling
    var parent = current.parent
    while (parent != null && parent.children.indexOf(current) == parent.children.length - 1) {
      current = parent
      parent = current.parent
    }
    if (parent != null) {
      // we have the next sibling in current, must find first leaf
      current = parent.children[parent.children.indexOf(current) + 1]
    } else {
      // we have reached root, this was the last leaf, there is no other
      return null
    }
  }
  // find first child of the current node
  while (current.children.length > 0) {
    current = current.children[0]
  }
  return current
}

function getRoot() {
  var current = this
  while (current.parent != null) current = current.parent
  return current
}

function getDocumentStart() {
  return this.getRoot().start
}

function getDocumentEnd() {
  return this.getRoot().end
}

function previousLeaf() {
  var current = this
  var parent = current.parent
  while (parent != null && parent.children.indexOf(current) == 0) {
    // keep going up
    current = parent
    parent = current.parent
  }
  if (parent != null) {
    current = parent.children[parent.children.indexOf(current) - 1]
    // go down on the last child track
    while (current.children.length > 0) current = current.children[current.children.length - 1]
    return current
  } else return null
}

function leafAtPosition(position) {
  if (position < this.start || this.end < position) return null
  else {
    var currentNode = this
    while (currentNode != null && currentNode.children.length > 0) {
      var i = 0;
      while (i < currentNode.children.length 
        && (currentNode.children[i].start > position || currentNode.children[i].end < position)) {
          i = i + 1;
      }
      if (i < currentNode.children.length) {
        currentNode = currentNode.children[i]
      } else {
        currentNode = null
      }
    }
    return currentNode
  }
}

function findSpaceAfter(position) {
  var spacePattern = /\s/
  // first get leaf at position
  var leaf = this.leafAtPosition(position)
  // for a text node, next space may be in the text node, next space character after position
  // if other kind of node, next space is the start of next leaf
  if (leaf != null && leaf.end == position) {
    // we need to look in the next node
    leaf = leaf.nextLeaf()
  }
  if (leaf != null && leaf.name == "text") {
    var searchStartPosition = (position - leaf.start + 1 > 0) ? position - leaf.start + 1 : 0
    var m = spacePattern.exec(leaf.content.substring(searchStartPosition))
    if (m != null) {
      return m.index + position + 1
    }
  }
  if (leaf != null) return leaf.end
  else return this.getDocumentEnd()
}

function findSpaceBefore(position) {
  var spacePattern = /\s[^\s]*$/
  var leaf = this.leafAtPosition(position)
  if (leaf != null && leaf.name == "text") {
    var searchText = leaf.content.substring(0, position - leaf.start)
    var m = spacePattern.exec(searchText)
    if (m != null) {
      return m.index + leaf.start
    }
  }
  if (leaf != null) {
    leaf = leaf.previousLeaf()
  }
  if (leaf != null) return leaf.end
  else return this.getDocumentStart()
}

function copy(from, to) {
  if (this.name == "text") {
    if (from <= this.start && this.end <= to) {
      // this node is copied whole
      return new BookNode("text", this.content, null, [], this.start, this.end)
    } else if (from <= this.start && this.start <= to && to<= this.end) {
      // copy ends at this node
      return new BookNode(this.name, this.content.substring(0, to - this.start + 1), null, [], this.start, to)
    } else if (this.start <= from && from <= this.end && this.end <= to) {
      // copy starts at this node
      return new BookNode(this.name, this.content.substring(from - this.start), null, [], from, this.end)
    } else if (this.start <= from && to < this.end) {
      // we only copy part of this node
      return new BookNode(this.name, this.content.substring(from - this.start, to - this.start + 1), null, [], from, to)
    } else {
      return null
    }
  } else if (shouldBeLeafElement(this.name)) {
    if (from <= this.start && this.end <= to) {
      // include element in selection
      return new BookNode(this.name, this.content, null, [], this.start, this.end)
    } else {
      return null
    }
  } else {
    if (this.end < from || this.start > to) {
      // this node is outside the range and should not be copied
      return null
    } else {
      var newNode = new BookNode(this.name, this.content)
      var newChildren = []
      for (var i = 0; i < this.children.length; i++) {
        var copiedChild = this.children[i].copy(from, to)
        if (copiedChild != null) {
          copiedChild.parent = newNode
          newChildren.push(copiedChild)
        }
      }
      newNode.children = newChildren
      if (newNode.children.length == 0) {
        newNode.start = this.start
        newNode.end = this.end
      } else {
        newNode.start = newNode.children[0].start
        newNode.end = newNode.children[newNode.children.length - 1].end
      }
      return newNode
    }
  }
}

function convert(object) {
    var node = new BookNode(object.name, object.content)
    node.start = object.start
    node.end = object.end
    for (var i = 0; i < object.children.length; i++) {
        var childNode = convert(object.children[i])
        childNode.parent = node
        node.children.push(childNode)
    }
    return node
}

module.exports = {
  BookNode: BookNode,
  getHtmlBody: getHtmlBody,
  parse: parse,
  getTagName: getTagName,
  parseBody: parseBody,
  convert: convert
};