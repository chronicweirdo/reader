function scrollNecessary(el) {
    return el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth
}

function getZoom() {
    if (document.bookZoom) {
        return document.bookZoom
    } else {
        var zoom = parseFloat(getMeta("bookZoom"))
        document.bookZoom = zoom
        return document.bookZoom
    }
}

function getPagesKey() {
    return getMeta("bookId") + "_" + getViewportWidth() + "_" + getViewportHeight() + "_" + getZoom()
}

function getPageFor(position) {
    var pagesKey = getPagesKey()
    //var savedPages = window.localStorage.getItem(pagesKey)
    var savedPages = document.savedPages
    if (savedPages != null) {
        // search for page
        for (var i = 0; i < savedPages.length; i++) {
            if (savedPages[i].start <= position && position <= savedPages[i].end) {
                // we found the page
                return savedPages[i]
            }
        }
    }
    // no page available
    return null
}

function computePagesFor(position) {
    if (! document.computationQueue.includes(position)) {
        document.computationQueue.push(position)
    }
    if (document.computationQueue.length > 0) {
        console.log("triggerring computation")
        scheduleComputation()
    }
}

function scheduleComputation() {
    window.setTimeout(function() {
        startComputation()
    }, 100)
}

function loadPageFor(position, callback) {
    console.log("loading page for " + position)
    var page = getPageFor(position)
    if (page == null) {
        // trigger page computation
        computePagesFor(position)

        // keep trying to retrieve the page periodically, until we have it
        window.setTimeout(function() {
            loadPageFor(position, callback)
        }, 100)
    } else {
        console.log("found page for " + position)
        var data = loadDataFor(page.start, page.end)
        callback(data)
    }
}

function loadDataFor(start, end) {
    if (document.section != null && document.section.start <= start && start <= end && end <= document.section.end) {
        return document.section.getContent().substring(start, end+1)
    } else {
        return null
    }
}

function displayPageFor(position) {
    // show spinner
    loadPageFor(position, function(page) {
        var content = document.getElementById("content")
        content.innerHTML = page
        // hide spinner
    })
}

function downloadSection(position, callback) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            console.log(this.responseText)
            var jsonObj = JSON.parse(this.responseText)
            console.log(jsonObj)
            var node = convert(jsonObj)
            console.log(node)
            document.node = node
            if (callback != null) {
                callback(node)
            }
        }
    }
    xhttp.open("GET", "bookSection?id=" + getMeta("bookId") + "&position=" + position, true)
    xhttp.send()
}

function getSectionFor(position) {
    if (document.section != null && document.section.start <= position && position <= document.section.end) {
        return document.section
    } else {
        downloadSection(position, function(node) {
            document.section = node
        })
        return null
    }
}

function startComputation() {
    console.log("starting computation")
    if (document.computationQueue.length > 0) {
        var position = document.computationQueue.shift()
        console.log("computing page for position " + position)
        var section = getSectionFor(position)
        if (section != null) {
            var textContent = section.getContent()
            var shadowContent = document.getElementById("shadowContent")
            shadowContent.innerHTML = ""

            var previousEnd = position + 1
            var end = position + 1
            while (scrollNecessary(shadowContent) == false && end < textContent.length) {
                console.log("growing content")
                previousEnd = end
                end = end + 1
                // grow content
                shadowContent.innerHTML = textContent.substring(position, end)
            }

            // store page
            var pagesKey = getPagesKey()
            //var savedPages = window.localStorage.getItem(pagesKey)
            if (document.savedPages == null) {
                document.savedPages = []
            }
            document.savedPages.push({start: position, end: previousEnd})
            //window.localStorage.setItem(pagesKey, savedPages)


            // if we did not finish finding pages in section, request computation of next page
            if (previousEnd < section.length - 1) {
                computePagesFor(previousEnd + 1)
            }
        } else {
            // try again later
            console.log("section not available yet, retry later")
            scheduleComputation()
        }
    }
}

window.onload = function() {
    var startPosition = num(getMeta("startPosition"))
    console.log("start position: " + startPosition)

    document.computationQueue = []
    //displayPageFor(startPosition)
    //downloadSection(1000)
    displayPageFor(1000)
}