function scrollNecessary(el) {
    var images = el.getElementsByTagName('img')
    var imageCount = images.length
    if (imageCount > 0) {
        var loadedImages = 0
        for (var i = 0; i < imageCount; i++) {
            var imageResolvedFunction = function() {
                loadedImages = loadedImages + 1
                if (loadedImages == imageCount) {
                    return el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth
                }
            }
            images[i].onload = imageResolvedFunction
            images[i].onerror = imageResolvedFunction
        }
    } else {
        return el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth
    }
}

function scrollNecessaryAsync(el, trueCallback, falseCallback) {
    var images = el.getElementsByTagName('img')
    var imageCount = images.length
    if (imageCount > 0) {
        var loadedImages = 0
        for (var i = 0; i < imageCount; i++) {
            var imageResolvedFunction = function() {
                loadedImages = loadedImages + 1
                if (loadedImages == imageCount) {
                    if (el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth) trueCallback()
                    else falseCallback()
                }
            }
            images[i].onload = imageResolvedFunction
            images[i].onerror = imageResolvedFunction
        }
    } else {
        if (el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth) trueCallback()
        else falseCallback()
    }
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
        getContentFor(page.start, page.end, function(text) {
            var content = document.getElementById("ch_content")
            content.innerHTML = text
            document.currentPage = page
            // if book end is displayed, we mark the book as read
            if (page.end == parseInt(getMeta("bookEnd"))) {
                saveProgress(getMeta("bookId"), page.end)
            } else {
                saveProgress(getMeta("bookId"), page.start)
            }
            updatePositionInput(page.start)

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
    fixComponentHeights()
    if (document.currentPage != null) {
        var position = document.currentPage.start
        document.savedPages = []
        document.currentPage = null
        var content = document.getElementById("ch_content")
        content.innerHTML = ""
        displayPageFor(position)
    }
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
        }, 10)
    })
}

function savePage(start, end) {
    if (document.savedPages == null) {
        document.savedPages = []
    }
    document.savedPages.push({start: start, end: end})
}

function compute(section, start) {
    var shadowContent = document.getElementById("ch_shadow_content")
    shadowContent.innerHTML = ""

    var tryForPage = function(previousEnd, end) {
        shadowContent.innerHTML = section.copy(start, end).getContent()
        scrollNecessaryAsync(shadowContent,
            function() {
                // we have found our page, it ends at previousEnd
                savePage(start, previousEnd)
                if (previousEnd < section.end) { // this check is probably not necessary
                    // schedule computation for next page
                    window.setTimeout(function() {
                        compute(section, previousEnd + 1)
                    }, 10)
                }
            },
            function() {
                // if possible, increase page and try again
                if (end < section.end) {
                    var newEnd = section.findSpaceAfter(end)
                    tryForPage(end, newEnd)
                } else {
                    // we are at the end of the section, this is the last page
                    savePage(start, end)
                }
            }
        )
    }
    var firstEnd = section.findSpaceAfter(start)
    tryForPage(firstEnd, firstEnd)
}

function prepareBookTools() {
    var tools = document.getElementById("ch_tools")

    var links = tools.getElementsByTagName("a")
    var chapters = []
    for (var i = 0; i < links.length; i++) {
        links[i].classList.remove("ch_current")
        if (links[i].classList.contains("ch_chapter")) {
            chapters.push(links[i])
        }
    }
    var currentChapter = -1
    var bookEnd = parseInt(getMeta("bookEnd"))
    for (var i = 0; i < chapters.length; i++) {
        var position = parseInt(chapters[i].getAttribute("ch_position"))
        var chapterEndPosition = bookEnd
        if (i < chapters.length - 1) chapterEndPosition = parseInt(chapters[i+1].getAttribute("ch_position"))
        if (position <= document.currentPage.start && document.currentPage.end <= chapterEndPosition) {
            currentChapter = i
            break
        }
    }
    if (currentChapter != -1) {
        chapters[currentChapter].classList.add("ch_current")
    }
}

function displayPageForTocEntry(entry) {
    var position = parseInt(entry.getAttribute("ch_position"))
    displayPageFor(position)
}

function setZoom(zoom, withResize = true) {
    var defaultZoom = 1.5
    document.body.style["font-size"] = (zoom * defaultZoom) + "em"
    window.localStorage.setItem("bookZoom", zoom)
    if (withResize) handleResize()
}

function getSavedZoom() {
    var savedZoomValue = window.localStorage.getItem("bookZoom")
    if (savedZoomValue != null) {
        return parseFloat(savedZoomValue)
    } else {
        return 1
    }
}

function increaseZoom(event) {
    console.log(event)
    var currentZoom = getSavedZoom()
    setZoom(currentZoom + .1)
}

function decreaseZoom(event) {
    var currentZoom = getSavedZoom()
    setZoom(currentZoom - .1)
}

window.onload = function() {
    // fix viewport height
    fixComponentHeights()

    // other page controls heights need to be fixed like this too


    enableGesturesOnElement(document.getElementById("ch_prev"), {
        "clickAction": (x, y) => previousPage()
    })
    enableGesturesOnElement(document.getElementById("ch_next"), {
        "clickAction": (x, y) => nextPage()
    })
    document.getElementById("ch_tools_left").addEventListener("click", (event) => toggleTools(true, prepareBookTools))
    document.getElementById("ch_tools_right").addEventListener("click", (event) => toggleTools(false, prepareBookTools))
    document.getElementById("ch_tools_container").addEventListener("click", (event) => hideTools())
    document.getElementById("ch_decrease_zoom").addEventListener("click", (event) => {
        event.stopPropagation()
        decreaseZoom()
    })
    document.getElementById("ch_increase_zoom").addEventListener("click", (event) => {
        event.stopPropagation()
        increaseZoom()
    })

    addPositionInputTriggerListener(displayPageFor)


    var savedZoom = getSavedZoom()
    setZoom(savedZoom, false)

    var startPosition = num(getMeta("startPosition"))

    displayPageFor(parseInt(getMeta("startPosition")))
}