function BookNode(name, content, parent = null, children = [], start = null, end = null) {
    this.name = name
    this.content = content
    this.parent = parent
    this.children = children
    this.start = start
    this.end = end
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

module.exports = {
  getHtmlBody: getHtmlBody
};