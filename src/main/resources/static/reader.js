function setup() {
    console.log("setting up pagination")
    wrapContents()
    addButtons()
    addPage()
    setupDocumentStyle()
    computeStartPositionsOfElements(document.getElementById("content"))
    console.log("starting finding pages")
    var st = new Date()
    findPages()
    var end = new Date()
    var dur = end - st
    console.log("find pages duration: " + dur)
    jumpToLocation()
    console.log("setup complete")
}

function jumpToLocation() {
    var url = window.location.href
    if (url.lastIndexOf("#") > 0) {
        var id = url.substring(url.lastIndexOf("#") + 1, url.length)
        //console.log(id)
        displayPage(getPageForId(id))
    }
}

function setupDocumentStyle() {
    document.body.style.overflow = "hidden"
}

function addButtons() {
    var prev = document.createElement("div")
    prev.id = "prev"
    prev.style.display = "block"
    prev.style.position = "fixed"
    prev.style.top = "0"
    prev.style.left = "0"
    prev.style.width = "5vw"
    prev.style.height = "100vh"
    prev.style["background-color"] = "red"
    prev.addEventListener("click", function() {
        previousPage()
    })
    document.body.appendChild(prev)

    var next = document.createElement("div")
    next.id = "next"
    next.style.display = "block"
    next.style.position = "fixed"
    next.style.top = "0"
    next.style.right = "0"
    next.style.width = "5vw"
    next.style.height = "100vh"
    next.style["background-color"] = "blue"
    next.addEventListener("click", function() {
        nextPage()
    })
    document.body.appendChild(next)
}
function addPage() {
    var page = document.createElement("div")
    page.id = "page"
    page.style.border = "1px solid #ff0000aa"
    page.style.padding = "1vw"
    page.style.margin = "5vw"
    page.style.width = "87vw"
    page.style["font-size"] = "1.2em"
    document.body.appendChild(page)
}

function wrapContents() {
    //var content = document.querySelectorAll("body > *")
    //var content = document.body.childNodes
    //console.log(content[0])
    //console.log(content[content.length - 1])
    var contentDiv = document.createElement("div")
    contentDiv.id = "content"
    contentDiv.style.display = "none"
    document.body.appendChild(contentDiv)

    var range = document.createRange()
    //range.setStart(content[0], 0)
    range.setStart(document.body.firstChild, 0)
    //range.setEndAfter(content[content.length - 1])
    //range.setEnd(content[content.length - 1], 0)
    range.setEndBefore(contentDiv)

    contentDiv.appendChild(range.extractContents())
    //content.forEach(e => contentDiv.appendChild(e))
    //console.log(range.extractContents())
    //contentDiv.append(range.cloneContents())
    //for (e in range.cloneContents()) contentDiv.appendChild(e)
}

function previousPage() {
    if (document.currentPage > 0) {
        document.currentPage = document.currentPage - 1
        copyTextToPage(getPositions(), document.pages[document.currentPage], document.pages[document.currentPage + 1])
    } else {
        console.log("beginning of doc")
    }
}

function nextPage() {
    // see if there is a next page
    if (document.currentPage < document.pages.length - 2) {
        document.currentPage = document.currentPage + 1
        copyTextToPage(getPositions(), document.pages[document.currentPage], document.pages[document.currentPage + 1])
    } else {
        console.log("end of doc")
    }
}

function displayPage(page) {
    if (page >= 0 && page < document.pages.length - 1) {
        document.currentPage = page
        copyTextToPage(getPositions(), document.pages[page], document.pages[page + 1])
    } else {
        console.log("page out of range")
    }
}

function clearPage() {
    document.getElementById("page").innerHTML = ""
}

function computeStartPositionsOfElements(root) {
    console.log("computing start positions of elements")
    var positionToElement = []
    var idPositions = []
    var recursive = function(element, currentPosition) {
        if (element.nodeType == Node.TEXT_NODE) {
            positionToElement.push([currentPosition, element])
            return currentPosition + element.nodeValue.length
        } else if (element.nodeType == Node.ELEMENT_NODE) {
            if (element.id && element.id != null) {
                idPositions.push([element.id, currentPosition])
            }
            var children = element.childNodes
            var newCurrentPosition = currentPosition
            for (var i = 0; i < children.length; i++) {
                newCurrentPosition = recursive(children[i], newCurrentPosition)
            }
            return newCurrentPosition
        }
    }
    recursive(root, 0)
    setPositions(positionToElement)
    setIdPositions(idPositions)
    console.log("computed start positions")
    return positionToElement
}

function getElementForPosition(positions, position) {
    for (var i = 1; i < positions.length; i++) {
        if (positions[i][0] > position) return positions[i-1]
    }
    return positions[positions.length - 1]
}

function getPositions() {
    return document.positions
}

function setPositions(positions) {
    document.positions = positions
}

function setIdPositions(idPositions) {
    document.idPositions = idPositions
}

function getIdPositions(idPositions) {
    return document.idPositions
}

function getPositionForId(id) {
    for (var i = 0; i < document.idPositions.length; i++) {
        if (document.idPositions[i][0] == id) return document.idPositions[i][1]
    }
    return 0
}

function getPageForPosition(position) {
    for (var i = 1; i < document.pages.length - 1; i++) {
        if (document.pages[i] > position) return i-1
    }
    return document.pages.length - 2
}

function getPageForId(id) {
    return getPageForPosition(getPositionForId(id))
}

function getMaxPosition() {
    var last = document.positions[document.positions.length - 1]
    return last[0] + last[1].nodeValue.length
}

// find the end position for this start position that constitudes a full page
function findPage(startPosition) {
    console.log("find page for " + startPosition)
    var initialJump = 100
    var endPosition = findNextSpaceForPosition(startPosition + initialJump)
    var previousEndPosition = null
    copyTextToPage(getPositions(), startPosition, endPosition)
    while ((! scrollNecessary()) && (endPosition < getMaxPosition())) {
        previousEndPosition = endPosition
        endPosition = findNextSpaceForPosition(endPosition + 1)
        copyTextToPage(getPositions(), startPosition, endPosition)
    }
    if (scrollNecessary() && previousEndPosition != null) {
        copyTextToPage(getPositions(), startPosition, previousEndPosition)
        return previousEndPosition
    } else {
        return endPosition
    }
}

function findPages() {
    var pages = []
    pages.push(0)
    while (pages[pages.length - 1] < getMaxPosition() && pages.length < 100) {
        var endPosition = findPage(pages[pages.length - 1])
        pages.push(endPosition)
    }
    clearPage()
    document.pages = pages
    document.currentPage = 0
    copyTextToPage(getPositions(), document.pages[0], document.pages[1])
}


function copyTextToPage(positions, from, to) {
    var range = document.createRange()

    var startEl = getElementForPosition(positions, from)
    var startElement = startEl[1]
    var locationInStartEl = from - startEl[0]
    range.setStart(startElement, locationInStartEl)

    var endEl = getElementForPosition(positions, to)
    var locationInEndEl = to - endEl[0]
    range.setEnd(endEl[1], locationInEndEl)

    var page = document.getElementById("page")
    page.innerHTML = ""
    page.appendChild(range.cloneContents())
}

function findNextSpaceForPosition(position) {
    var positions = getPositions()
    var i = 0
    while (i < positions.length-1 && positions[i+1][0] < position) i = i + 1

    var el = positions[i][1]
    var p = position - positions[i][0]
    var str = el.nodeValue
    while (p < str.length && str.charAt(p) != ' ') p = p + 1

    if (p == str.length) {
        if (i == positions.length - 1) return getMaxPosition()
        else return positions[i+1][0]
    } else {
        return positions[i][0] + p
    }
}

function findPreviousSpaceForPosition(position) {
    var positions = getPositions()
    var i = 0
    while (i < positions.length-1 && positions[i+1][0] < position) i = i + 1

    var el = positions[i][1]
    var p = position - positions[i][0]
    while (p > 0 && el.nodeValue.charAt(p) != ' ') p = p - 1

    return positions[i][0] + p
}

function scrollNecessary() {
    return (document.body.scrollWidth > document.body.clientWidth) || (document.body.scrollHeight > document.body.clientHeight)
}

window.onload = function() {
    setup()
}