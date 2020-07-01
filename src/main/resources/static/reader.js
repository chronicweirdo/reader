function setup() {
    console.log("setting up pagination")
    //setupDocumentStyle()
    wrapContents()
    addPage()
    addButtons()
    //addSpinner()
    addLoadingScreen()

    window.setTimeout(function() {
        //showSpinner()
        computeStartPositionsOfElements(document.getElementById("content"))
        console.log("starting finding pages")
        var st = new Date()
        findPages()
        var end = new Date()
        var dur = end - st
        console.log("find pages duration: " + dur)
        jumpToLocation()
        //hideSpinner()
        hideLoadingScreen()
        console.log("setup complete")
    }, 300)
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

/*function setupDocumentStyle() {
    document.body.style.overflow = "hidden"
}*/

function addButtons() {
    var prev = document.createElement("div")
    prev.id = "prev"
    /*prev.style.display = "block"
    prev.style.position = "fixed"
    prev.style.top = "0"
    prev.style.left = "0"
    prev.style.width = "5vw"
    prev.style.height = "100vh"
    prev.style["background-color"] = "red"*/
    prev.addEventListener("click", function() {
        previousPage()
    })
    document.body.appendChild(prev)

    var next = document.createElement("div")
    next.id = "next"
    /*next.style.display = "block"
    next.style.position = "fixed"
    next.style.top = "0"
    next.style.right = "0"
    next.style.width = "5vw"
    next.style.height = "100vh"
    next.style["background-color"] = "blue"*/
    next.addEventListener("click", function() {
        nextPage()
    })
    document.body.appendChild(next)
}
function addPage() {
    var pageContainer = document.createElement("div")
    pageContainer.id="pageContainer"
    /*pageContainer.style.width = "100vw"
    pageContainer.style.height = "100vh"
    pageContainer.style.overflow = "hidden"
    pageContainer.style.padding = "0"
    pageContainer.style.margin = "0"*/
    var page = document.createElement("div")
    page.id = "page"
    /*page.style.border = "1px solid #ff0000aa"
    page.style.padding = "1vw"
    page.style.margin = "5vw"
    page.style["font-size"] = "1.2em"*/
    pageContainer.appendChild(page)
    document.body.appendChild(pageContainer)
    document.pageContainer = pageContainer
}

/*
<div id="spinner">
    <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="margin:auto;display:block;" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid">
        <rect x="20" y="20" width="60" height="60" stroke="#000000" stroke-width="10" fill="none"></rect>
        <rect x="20" y="20" width="60" height="60" stroke="#ffd700" stroke-width="10" stroke-lincap="undefined" fill="none">
            <animate attributeName="stroke-dasharray" repeatCount="indefinite" dur="1s" keyTimes="0;0.5;1" values="24 216;120 120;24 216"></animate>
            <animate attributeName="stroke-dashoffset" repeatCount="indefinite" dur="1s" keyTimes="0;0.5;1" values="0;-120;-240"></animate>
        </rect>
    </svg>
</div>
*/
/*function addSpinner() {
    var spinner = document.createElement("div")
    spinner.id = "spinner"
    spinner.innerHTML = '<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" style="margin:auto;display:block;" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid"><rect x="20" y="20" width="60" height="60" stroke="#000000" stroke-width="10" fill="none"></rect><rect x="20" y="20" width="60" height="60" stroke="#ffd700" stroke-width="10" stroke-lincap="undefined" fill="none"><animate attributeName="stroke-dasharray" repeatCount="indefinite" dur="1s" keyTimes="0;0.5;1" values="24 216;120 120;24 216"></animate><animate attributeName="stroke-dashoffset" repeatCount="indefinite" dur="1s" keyTimes="0;0.5;1" values="0;-120;-240"></animate></rect></svg>'
    document.body.appendChild(spinner)
}*/

/*function showSpinner() {
    console.log("showing spiner")
    var spinner = document.getElementById("spinner")
    spinner.style.display = "block"
}*/

/*function hideSpinner() {
    console.log("hiding spinner")
    var spinner = document.getElementById("spinner")
    spinner.style.display = "none"
}*/

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

// find the end position for this start position that constitudes a full page
function findPage(startPosition, initialJump) {
    //console.log("find page for " + startPosition + " with jump " + initialJump)
    //var initialJump = 100
    var endPosition = findNextSpaceForPosition(startPosition + initialJump)
    //console.log("end position: " + endPosition)
    //var previousEndPosition = null
    copyTextToPage(getPositions(), startPosition, endPosition)
    while ((! scrollNecessary()) && (endPosition < getMaxPosition())) {
        //previousEndPosition = endPosition
        endPosition = findNextSpaceForPosition(endPosition + 1)
        copyTextToPage(getPositions(), startPosition, endPosition)
        //console.log(endPosition)
    }
    while (scrollNecessary()) {
        endPosition = findPreviousSpaceForPosition(endPosition - 1)
        copyTextToPage(getPositions(), startPosition, endPosition)
        //console.log(endPosition)
    }
    /*if (scrollNecessary() && previousEndPosition != null) {
        copyTextToPage(getPositions(), startPosition, previousEndPosition)
        return previousEndPosition
    } else {
        return endPosition
    }*/
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
    //var el = document.getElementById("pageContainer")
    //return (el.scrollWidth > el.clientWidth) || (el.scrollHeight > el.clientHeight)
    //return el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth
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