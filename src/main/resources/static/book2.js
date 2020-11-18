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

function getContentFor(start, end, callback) {
    if (document.section != null && document.section.start <= start && start <= end && end <= document.section.end) {
        if (callback != null) {
            callback(document.section.copy(start, end).getContent())
        }
    } else {
        // download section
        downloadSection(start, function(section) {
            document.section = section
            if (document.section.start <= start && start <= end && end <= document.section.end) {
                if (callback != null) {
                    callback(document.section.copy(start, end).getContent())
                }
            }
        })
    }
}

function displayPageFor(position, firstTry = true) {
    console.log("displaying page " + position)
    showSpinner()
    var page = getPageFor(position)
    if (page == null) {
        // compute pages for section and retry
        if (firstTry) {
            computePagesForSection(position)
        }
        window.setTimeout(function() {
            displayPageFor(position, false)
        }, 100)
    } else {
        console.log("found page for " + position)
        getContentFor(page.start, page.end, function(text) {
            var content = document.getElementById("content")
            content.innerHTML = text
            document.currentPage = page
            hideSpinner()
        })
    }
}

function nextPage() {
    if (document.currentPage != null) {
        if (document.currentPage.end < parseInt(getMeta("bookEnd"))) {
            displayPageFor(document.currentPage.end + 1)
        }
    }
}
function previousPage() {
    if (document.currentPage != null) {
        if (document.currentPage.start > parseInt(getMeta("bookStart"))) {
            displayPageFor(document.currentPage.start - 1)
        }
    }
}

function handleResize() {
    var position = document.currentPage.start
    document.savedPages = []
    document.currentPage = null
    var content = document.getElementById("content")
    content.innerHTML = ""
    displayPageFor(position)
}

function downloadSection(position, callback) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            var jsonObj = JSON.parse(this.responseText)
            var node = convert(jsonObj)
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

function computePagesForSection(position) {
    downloadSection(position, function(section) {
        window.setTimeout(function() {
            compute(section, section.start)
        }, 100)
    })
}

function compute(section, start) {
    console.log("computing pages for section " + section.start + " position " + start)
    var shadowContent = document.getElementById("shadowContent")
    shadowContent.innerHTML = ""

    var previousEnd = start
    var end = section.findSpaceAfter(start)
    shadowContent.innerHTML = section.copy(start, end).getContent()
    while (scrollNecessary(shadowContent) == false && end < section.end) {
        previousEnd = end
        end = section.findSpaceAfter(end)
        shadowContent.innerHTML = section.copy(start, end).getContent()
    }
    if (end < section.end) {
        end = previousEnd
    }

    // store page
    if (document.savedPages == null) {
        document.savedPages = []
    }
    document.savedPages.push({start: start, end: end})

    if (end < section.end) {
        // schedule computation for the next page
        window.setTimeout(function() {
            compute(section, end + 1)
        }, 100)
    }
}

function showSpinner() {
    var spinner = document.getElementById("spinner")
    spinner.style.visibility = "visible"
}

function hideSpinner() {
    var spinner = document.getElementById("spinner")
    spinner.style.visibility = "hidden"
}

window.onload = function() {
    var startPosition = num(getMeta("startPosition"))
    console.log("start position: " + startPosition)

    displayPageFor(parseInt(getMeta("startPosition")))
}