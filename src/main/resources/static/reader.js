function setup() {
    console.log("setting up pagination")
    wrapContents()
    addPage()
    addButtons()
    addTools()
    addLoadingScreen()
    showLoadingScreen()
    document.testlog = false

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

    var resizeThreshold = 1000
    window.onresize = function() {
        document.resizeTime = new Date()
        window.setTimeout(function() {
            var now = new Date()
            if (now - document.resizeTime >= resizeThreshold) {
                resize()
            }
        }, resizeThreshold)
    }
}

function resize() {
    // save current position
    var location = document.pages[document.currentPage]
    showLoadingScreen()
    window.setTimeout(function() {
        findPages()
        displayPage(getPageForPosition(location))
        hideLoadingScreen()
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
            var sectionStart = parseInt(getMeta("sectionStart"))
            var positionInSection = +id - sectionStart
            displayPage(getPageForPosition(positionInSection))
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
    pageContainer.style.visibility = "hidden"
    var page = document.createElement("div")
    page.id = "page"
    pageContainer.appendChild(page)
    document.body.appendChild(pageContainer)
    document.pageContainer = pageContainer

    page.addEventListener("click", toggleTools)
}

function num(s, def) {
    var patt = /[\-]?[0-9\.]+/
    var match = patt.exec(s)
    if (match != null && match.length > 0) {
        var n = match[0]
        if (n.indexOf('.') > -1) {
            return parseFloat(n)
        } else {
            return parseInt(n)
        }
    }
    return def
}

function increaseFontSize() {
    //var currentFontSize = document.getElementById("page").style["font-size"]
    var page = document.getElementById("page")
    var currentFontSize = window.getComputedStyle(page, null).getPropertyValue('font-size')
    console.log(currentFontSize)
    currentFontSize = num(currentFontSize, 10) * 1.2
    console.log(currentFontSize)
    page.style['font-size'] = currentFontSize + 'px'
    resize()
}

/*
function enableGestures(element, click, pinch) {
    disableEventHandlers(element)
    var hammertime = new Hammer(element, {domEvents: true})
    hammertime.get('pan').set({ direction: Hammer.DIRECTION_ALL, threshold: 0 });
    hammertime.get('pinch').set({ enable: true });

    var panPreviousDeltaX = 0
    var panPreviousDeltaY = 0

    var pinching = false
    var originalZoom = null
    var pinchCenterX = null
    var pinchCenterY = null

    hammertime.on('pinchstart', function(ev) {
        ev.preventDefault();
        pinching = true
        originalZoom = 1
        pinchCenterX = ev.center.x
        pinchCenterY = ev.center.y
    })
    hammertime.on('pinch', function(ev) {
        ev.preventDefault()
        var currentDeltaX = ev.deltaX - panPreviousDeltaX
        var currentDeltaY = ev.deltaY - panPreviousDeltaY
        pinch(originalZoom * ev.scale, pinchCenterX, pinchCenterY, currentDeltaX, currentDeltaY)

        panPreviousDeltaX = ev.deltaX
        panPreviousDeltaY = ev.deltaY
    });
        hammertime.on('pinchend', function(ev) {
            panPreviousDeltaX = 0
            panPreviousDeltaY = 0
        })

        hammertime.on('pan', function(ev) {
            if (! pinching) {
                var currentDeltaX = ev.deltaX - panPreviousDeltaX
                var currentDeltaY = ev.deltaY - panPreviousDeltaY
                pan(currentDeltaX * getPanSpeed(), currentDeltaY * getPanSpeed())
                panPreviousDeltaX = ev.deltaX
                panPreviousDeltaY = ev.deltaY
            }
        })
        hammertime.on('panend', function(ev) {
            if (! pinching) {
                panPreviousDeltaX = 0
                panPreviousDeltaY = 0
            }
            // a pinch always ends with a pan
            pinching = false
        })
    }
    */

/*
<div id="tools" style="display: none">
    <h1 th:text="${title}">Comic Title</h1>
    <p><a th:href="@{/(search=${collection})}" th:text="${collection}">collection</a></p>
    <p>Page <input id="pagenum" type="number"  min="1" th:max="${pages}"/> of <span th:text="${pages}">20</span></p>
    <p>
        <a onclick="toggleFullScreen()">fullscreen</a>
        <a onclick="goBack()">back</a>
        <a onclick="removeProgress()">remove progress</a>
    </p>
</div>
*/

function addTools() {
    var toolsContainer = document.createElement("div")
    toolsContainer.id="tools"
    toolsContainer.style.visibility = "hidden"

    var title = document.createElement("h1")
    title.innerHTML = getMeta("title")
    toolsContainer.appendChild(title)

    var collection = getMeta("collection")
    var collectionParagraph = document.createElement("p")
    var collectionLink = document.createElement("a")
    collectionLink.href = "/search=" + collection
    collectionLink.innerHTML = collection
    collectionParagraph.appendChild(collectionLink)
    toolsContainer.appendChild(collectionParagraph)

    var positionParagraph = document.createElement("p")
    var currentPositionSpan = document.createElement("span")
    currentPositionSpan.id = "currentPosition"
    currentPositionSpan.innerHTML = getMeta("sectionStart")
    positionParagraph.appendChild(currentPositionSpan)
    var outOf = document.createTextNode(" / " + getMeta("bookSize"))
    positionParagraph.appendChild(outOf)
    toolsContainer.appendChild(positionParagraph)

    var actionsParagraph = document.createElement("p")
    var goBack = document.createElement("a")
    goBack.href = "/"
    goBack.innerHTML = "back"
    actionsParagraph.appendChild(goBack)
    var removeProgressLink = document.createElement("a")
    removeProgressLink.onclick = removeProgress
    removeProgressLink.innerHTML = "remove progress"
    actionsParagraph.appendChild(removeProgressLink)
    toolsContainer.appendChild(actionsParagraph)

    document.body.appendChild(toolsContainer)
}

function addLoadingScreen() {
    var loadingScreen = document.createElement("div")
    loadingScreen.id = "loading"
    loadingScreen.style.visibility = "hidden"
    loadingScreen.innerHTML = "Loading..."
    document.body.appendChild(loadingScreen)
}

function showLoadingScreen() {
    var loadingScreen = document.getElementById("loading")
    loadingScreen.style.visibility = "visible"
    var pageContainer = document.getElementById("pageContainer")
    pageContainer.style.visibility = "hidden"
}

function hideLoadingScreen() {
    var loadingScreen = document.getElementById("loading")
    loadingScreen.style.visibility = "hidden"
    var pageContainer = document.getElementById("pageContainer")
    pageContainer.style.visibility = "visible"
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
        reportPosition(document.pages[document.currentPage])
    } else {
        console.log("beginning of doc")
        var bookId = getMeta("bookId")
        var prevSection = getMeta("prevSection")
        if (prevSection && prevSection.length > 0 && bookId && bookId.length > 0) {
            window.location = "book?id=" + bookId + "&path=" + prevSection + "#" + Number.MAX_SAFE_INTEGER
        }
    }
}

function nextPage() {
    // see if there is a next page
    if (document.currentPage < document.pages.length - 2) {
        document.currentPage = document.currentPage + 1
        copyTextToPage(getPositions(), document.pages[document.currentPage], document.pages[document.currentPage + 1])
        reportPosition(document.pages[document.currentPage])
    } else {
        console.log("end of doc")
        var bookId = getMeta("bookId")
        var nextSection = getMeta("nextSection")
        if (bookId && bookId.length > 0) {
            if (nextSection && nextSection.length > 0) {
                window.location = "book?id=" + bookId + "&path=" + nextSection
            } else {
                reportPosition(getMaxPosition())
            }
        }
    }
}

function displayPage(page) {
    if (page >= 0 && page < document.pages.length - 1) {
        document.currentPage = page
        var startPosition = document.pages[page]
        copyTextToPage(getPositions(), startPosition, document.pages[page + 1])
        reportPosition(startPosition)
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
    var previousEndPosition = null
    var endPosition = findNextSpaceForPosition(startPosition + initialJump)
    copyTextToPage(getPositions(), startPosition, endPosition)
    while ((! scrollNecessary()) && (endPosition < getMaxPosition())) {
        previousEndPosition = endPosition
        endPosition = findNextSpaceForPosition(endPosition + 1)
        copyTextToPage(getPositions(), startPosition, endPosition)
    }
    while (scrollNecessary() && (endPosition > startPosition)) {
        previousEndPosition = endPosition
        endPosition = findPreviousSpaceForPosition(endPosition - 1)
        copyTextToPage(getPositions(), startPosition, endPosition)
    }
    if (endPosition == startPosition) return previousEndPosition
    return endPosition
}

function findPages() {
    var pages = []
    pages.push(0)
    var jump = 100
    while (pages[pages.length - 1] < getMaxPosition() && pages.length < 100) {
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
    if (p == el.nodeValue.length) return positions[i][0] + p
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

function getPathname() {
    var a = document.createElement('a');
    a.href = window.location;
    return a.pathname + a.search
    /*    return {
            href: a.href,
            host: a.host,
            hostname: a.hostname,
            port: a.port,
            pathname: a.pathname,
            protocol: a.protocol,
            hash: a.hash,
            search: a.search
        };
    }*/
}

function removeProgress() {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                window.location = "/"
            }
        }
    }
    xhttp.open("DELETE", "removeProgress?id=" + getMeta("bookId"), true)
    xhttp.send()
}

function toggleTools() {
    var tools = document.getElementById("tools")
    if (tools.style.visibility == "hidden") {
        tools.style.visibility = "visible"
    } else {
        tools.style.visibility = "hidden"
    }
}

function reportPosition(position) {
    var sectionStart = parseInt(getMeta("sectionStart"))
    var positionInBook = sectionStart + position

    var currentPositionEl = document.getElementById("currentPosition")
    currentPositionEl.innerHTML = positionInBook

    var xhttp = new XMLHttpRequest()
    xhttp.open("PUT", "markProgress?id=" + getMeta("bookId") + "&position=" + positionInBook, true)
    xhttp.send()
}

window.onload = function() {
    setup()
}