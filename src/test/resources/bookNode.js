function BookNode(name, content, parent = null, children = [], start = null, end = null) {
  this.name = name
  this.content = content
  this.parent = parent
  this.children = children
  this.start = start
  this.end = end

  this.addChild = addChild
  this.prettyPrint = prettyPrint
}

var VOID_ELEMENTS = ["area","base","br","col","hr","img","input","link","meta","param","keygen","source"]

function isVoidElement(tagName) {
  return VOID_ELEMENTS.includes(tagName.toLowerCase())
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

  //bodyNode.collapseLeafs()
  //bodyNode.updatePositions()
  return bodyNode
}

function parse(html) {
  var body = getHtmlBody(html)
  if (body != null) {
    return parseBody(body)
  }
  return null
}

module.exports = {
  BookNode: BookNode,
  getHtmlBody: getHtmlBody,
  parse: parse,
  getTagName: getTagName,
  parseBody: parseBody
};