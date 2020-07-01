function setup() {
    console.log("setting up pagination")
    wrapContents()
    addPage()
    addButtons()
    addLoadingScreen()

    window.setTimeout(function() {
        computeStartPositionsOfElements(document.getElementById("content"))
        console.log("starting finding pages")
        var st = new Date()
        findPages()
        var end = new Date()
        var dur = end - st
        console.log("find pages duration: " + dur)
        jumpToLocation()
        hideLoadingScreen()
        console.log("setup complete")
    }, 100)
}

function jumpToLocation() {
    var url = window.location.href
    if (url.lastIndexOf("#") > 0) {
        var id = url.substring(url.lastIndexOf("#") + 1, url.length)
        if (isNaN(id)) {
            console.log("jumping to id " + id)
            displayPage(getPageForId(id))
        } else {
            console.log("jumping to position " + id)
            displayPage(getPageForPosition(+id))
        }
    }
}

function addButtons() {
    var prev = document.createElement("div")
    prev.id = "prev"
    prev.addEventListener("click", function() {
        previousPage()
    })
    document.body.appendChild(prev)

    var next = document.createElement("div")
    next.id = "next"
    next.addEventListener("click", function() {
        nextPage()
    })
    document.body.appendChild(next)
}
function addPage() {
    var pageContainer = document.createElement("div")
    pageContainer.id="pageContainer"
    var page = document.createElement("div")
    page.id = "page"
    pageContainer.appendChild(page)
    document.body.appendChild(pageContainer)
    document.pageContainer = pageContainer
}

function addLoadingScreen() {
    var loadingScreen = document.createElement("div")
    loadingScreen.id = "loading"
    loadingScreen.innerHTML = "Loading..."
    document.body.appendChild(loadingScreen)
}

function showLoadingScreen() {
    var loadingScreen = document.getElementById("loading")
    loadingScreen.style.display = "block"
}

function hideLoadingScreen() {
    var loadingScreen = document.getElementById("loading")
    loadingScreen.style.display = "none"
}

function wrapContents() {
    var contentDiv = document.createElement("div")
    contentDiv.id = "content"
    //contentDiv.style.display = "none"
    document.body.appendChild(contentDiv)

    var range = document.createRange()
    range.setStart(document.body.firstChild, 0)
    range.setEndBefore(contentDiv)

    contentDiv.appendChild(range.extractContents())
}

function previousPage() {
    if (document.currentPage > 0) {
        document.currentPage = document.currentPage - 1
        copyTextToPage(getPositions(), document.pages[document.currentPage], document.pages[document.currentPage + 1])
    } else {
        console.log("beginning of doc")
        var bookId = getMeta("bookId")
        var prevSection = getMeta("prevSection")
        if (prevSection && prevSection.length > 0 && bookId && bookId.length > 0) {
            window.location = "book?id=" + bookId + "&path=" + prevSection
        }
    }
}

function nextPage() {
    // see if there is a next page
    if (document.currentPage < document.pages.length - 2) {
        document.currentPage = document.currentPage + 1
        copyTextToPage(getPositions(), document.pages[document.currentPage], document.pages[document.currentPage + 1])
    } else {
        console.log("end of doc")
        var bookId = getMeta("bookId")
        var nextSection = getMeta("nextSection")
        if (nextSection && nextSection.length > 0 && bookId && bookId.length > 0) {
            window.location = "book?id=" + bookId + "&path=" + nextSection
        }
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

function findPage(startPosition, initialJump) {
    var endPosition = findNextSpaceForPosition(startPosition + initialJump)
    copyTextToPage(getPositions(), startPosition, endPosition)
    while ((! scrollNecessary()) && (endPosition < getMaxPosition())) {
        endPosition = findNextSpaceForPosition(endPosition + 1)
        copyTextToPage(getPositions(), startPosition, endPosition)
    }
    while (scrollNecessary()) {
        endPosition = findPreviousSpaceForPosition(endPosition - 1)
        copyTextToPage(getPositions(), startPosition, endPosition)
    }
    return endPosition
}

function findPages() {
    var pages = []
    pages.push(0)
    while (pages[pages.length - 1] < getMaxPosition() && pages.length < 100) {
        var jump = 100
        if (pages.length > 2) jump = pages[pages.length - 1] - pages[pages.length - 2]
        var endPosition = findPage(pages[pages.length - 1], jump)
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

    if (p >= str.length) {
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
    return document.pageContainer.scrollHeight > document.pageContainer.offsetHeight || document.pageContainer.scrollWidth > document.pageContainer.offsetWidth
}

function getMeta(metaName) {
    const metas = document.getElementsByTagName('meta');

    for (let i = 0; i < metas.length; i++) {
        if (metas[i].getAttribute('name') === metaName) {
        return metas[i].getAttribute('content');
        }
    }

    return '';
}

window.onload = function() {
    setup()
}