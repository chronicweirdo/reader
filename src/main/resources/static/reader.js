function setup() {
    console.log("setting up pagination")
    wrapContents()
    addButtons()
    addPage()
    setupDocumentStyle()
    var positions = computeStartPositionsOfElements(document.getElementById("content"))
    setPositions(positions)
    findPages()
    console.log("setup complete")
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
    let content = document.querySelectorAll("body > *")
    var range = document.createRange()
    range.setStartBefore(content[0])
    range.setEndAfter(content[content.length - 1])
    var contentDiv = document.createElement("div")
    contentDiv.id = "content"
    contentDiv.style.display = "none"
    document.body.appendChild(contentDiv)
    contentDiv.appendChild(range.extractContents())
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

function clearPage() {
    document.getElementById("page").innerHTML = ""
}

function findSpaceAfter(str, pos) {
    for (var i = pos; i < str.length; i++) {
        if (str.charAt(i) == ' ') return i
    }
    return str.length
}

function computeStartPositionsOfElements(root) {
    console.log("computing start positions of elements")
    var positionToElement = []
    var recursive = function(element, currentPosition) {
        if (element.nodeType == Node.TEXT_NODE) {
            positionToElement.push([currentPosition, element])
            return currentPosition + element.nodeValue.length
        } else if (element.nodeType == Node.ELEMENT_NODE) {
            var children = element.childNodes
            var newCurrentPosition = currentPosition
            for (var i = 0; i < children.length; i++) {
                newCurrentPosition = recursive(children[i], newCurrentPosition)
            }
            return newCurrentPosition
        }
    }
    recursive(root, 0)
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

function getMaxPosition() {
    var last = document.positions[document.positions.length - 1]
    return last[0] + last[1].nodeValue.length
}

// find the end position for this start position that constitudes a full page
function findPage(startPosition) {
    var initialJump = 100
    var endPosition = getNextSpaceForPosition(getPositions(), startPosition + initialJump)
    var previousEndPosition = null
    copyTextToPage(getPositions(), startPosition, endPosition)
    while ((! scrollNecessary()) && (endPosition < getMaxPosition())) {
        previousEndPosition = endPosition
        endPosition = getNextSpaceForPosition(getPositions(), endPosition + 1)
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

function getNextSpaceForPosition(positions, position) {
    var index = null
    for (var i = 1; i < positions.length-1; i++) {
        if (positions[i][0] > position) {
            index = i - 1
            break
        }
    }
    if (index == null) return getMaxPosition()

    var nextSpaceInElementText = findSpaceAfter(positions[index][1].nodeValue, position - positions[index][0])

    if (nextSpaceInElementText < positions[index][1].nodeValue.length) {
        return positions[index][0] + nextSpaceInElementText
    } else {
        if (index < positions.length-1) {
            return positions[index+1][0]
        } else {
            return getMaxPosition()
        }
    }
}

function scrollNecessary() {
    return (document.body.scrollWidth > document.body.clientWidth) || (document.body.scrollHeight > document.body.clientHeight)
}

window.onload = function() {
    setup()
}