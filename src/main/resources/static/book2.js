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
            updatePositionInput(getPositionPercentage(page.start, page.end))

            hideSpinner()
        })
    }
}

function getPositionPercentage(pageStart, pageEnd) {
    var bookSize = parseInt(getMeta("bookEnd"))
    var percentage = (pageEnd / bookSize) * 100.0
    var percentageInteger = Math.floor(percentage)
    var percentageFraction = Math.floor((percentage - percentageInteger)*100)

    var text = percentageInteger
    if (percentageFraction > 0) {
        text += "." + percentageFraction
    }
    text += "%"

    return text
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
        //document.savedPages = []
        loadCache()
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
    xhttp.open("GET", "bookSection?id=" + getMeta("bookId") + "&position=" + position)
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
    // saveCache() // we probably should not update the cache with every page
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
                    saveCache() // only update cache once all pages for a section were computed
                }
            }
        )
    }
    var firstEnd = section.findSpaceAfter(start)
    tryForPage(firstEnd, firstEnd)
}

function initializeMode() {
    var savedMode = window.localStorage.getItem("mode")
    if (savedMode != null) {
        if (savedMode == "dark") {
            var body = document.getElementsByTagName("BODY")[0]
            body.classList.add("dark")
        }
    } else {
        window.localStorage.setItem("mode", "light")
    }
}

function toggleMode() {
    var body = document.getElementsByTagName("BODY")[0]
    if (body.classList.contains("dark")) {
        body.classList.remove("dark")
        window.localStorage.setItem("mode", "light")
    } else {
        body.classList.add("dark")
        window.localStorage.setItem("mode", "dark")
    }
}

function getChapters() {
    if (! document.chapters) {
        var chapterElements = document.getElementsByClassName("ch_chapter")
        document.chapters = []
        for (var i = 0; i < chapterElements.length; i++) {
            document.chapters.push({
                start: parseInt(chapterElements[i].getAttribute("ch_position")),
                element: chapterElements[i]
            })
        }
    }
    return document.chapters
}

function resetCurrentChapter() {
    // remove current chapter selection
    var currentChapters = document.getElementsByClassName("ch_current")
    for (var i = 0; i < currentChapters.length; i++) {
        currentChapters[i].classList.remove("ch_current")
    }
}

function getCurrentChapter() {
    var chapters = getChapters()

    // find current chapter
    var currentChapter = -1
    var position = (document.currentPage.start + document.currentPage.end) / 2
    while (currentChapter < chapters.length - 1 && position > chapters[currentChapter + 1].start) {
        currentChapter = currentChapter + 1
    }
    return chapters[currentChapter]
}

function expandPathToChapter(chapter) {
    if (chapter != null) {
        chapter.element.classList.add("ch_current")
        var current = chapter.element
        while (current != null) {
            if (current.nodeName == "UL") {
                current.style.display = "block"
            }
            current = current.parentElement
        }
    }
}

function prepareBookTools() {
    resetCurrentChapter()
    hideAllSubchapters()
    var chapter = getCurrentChapter()
    expandPathToChapter(chapter)
}

function getChildList(current) {
    var childLists = current.getElementsByTagName("ul")
    if (childLists.length > 0) {
        return childLists[0]
    } else {
        return null
    }
}

function toggleChildList(current) {
    var childList = getChildList(current)
    if (childList != null) {
        if (childList.style.display == "none") {
            childList.style.display = "block"
        } else {
            childList.style.display = "none"
        }
    }
}

function hideAllSubchapters() {
    var listItemsWithSubchapters = document.getElementsByClassName("ch_withsubchapters")
    for (var i = 0; i < listItemsWithSubchapters.length; i++) {
        var subchapterList = listItemsWithSubchapters[i].getElementsByTagName("ul")[0]
        subchapterList.style.display = "none"
    }
}

function initializeChapters() {
    var listItemsWithSubchapters = document.getElementsByClassName("ch_withsubchapters")
    for (var i = 0; i < listItemsWithSubchapters.length; i++) {
        var para = listItemsWithSubchapters[i].getElementsByTagName("p")[0]
        console.log(para)
        para.addEventListener("click", (event) => {
            event.stopPropagation()
            toggleChildList(event.target.parentElement)
        })
    }
}

function initTableOfContents() {
    initializeChapters()
    hideAllSubchapters()
}

function displayPageForTocEntry(entry) {
    var position = parseInt(entry.getAttribute("ch_position"))
    hideTools()
    displayPageFor(position)
}

function setZoom(zoom, withResize = true) {
    //var defaultZoom = 1.5
    document.body.style["font-size"] = (zoom/10.0) + "em"
    window.localStorage.setItem("bookZoom", zoom)
    highlightZoomSetButtons(zoom)
    if (withResize) handleResize()
}

function getSavedZoom() {
    var savedZoomValue = window.localStorage.getItem("bookZoom")
    if (savedZoomValue != null) {
        return parseInt(savedZoomValue)
    } else {
        return 15
    }
}

function getCacheKey() {
    var pagesKey = getMeta("bookId") + "_" + getViewportWidth() + "_" + getViewportHeight() + "_" + getSavedZoom()
    return pagesKey
}

function increaseZoom(event) {
    var currentZoom = getSavedZoom()
    var newZoom = currentZoom + 1
    setZoom(newZoom)
}

function decreaseZoom(event) {
    var currentZoom = getSavedZoom()
    var newZoom = currentZoom - 1
    setZoom(newZoom)
}

function loadCache() {
    var cacheKey = getCacheKey()
    var cache = window.localStorage.getItem(cacheKey)
    if (cache != null) {
        document.savedPages = JSON.parse(cache)
    } else {
        document.savedPages = []
    }
}

function saveCache() {
    var cacheKey = getCacheKey()
    window.localStorage.setItem(cacheKey, JSON.stringify(document.savedPages))
}

function setupZoomSetButton(el) {
    var zoom = parseInt(el.getAttribute("zoom"))
    el.style["font-size"] = (zoom * .1) + "rem"
    el.addEventListener("click", (event) => {
        event.stopPropagation()
        setZoom(zoom)
    })
}

function highlightZoomSetButtons(currentZoom) {
    var zoomSetters = document.getElementsByClassName("ch_set_zoom")
    for (var i = 0; i < zoomSetters.length; i++) {
        var el = zoomSetters[i]
        var elZoom = parseInt(el.getAttribute("zoom"))
        if (elZoom == currentZoom) {
            el.classList.add("selected")
        } else {
            el.classList.remove("selected")
        }
    }
}

window.onload = function() {
    // fix viewport height
    fixComponentHeights()

    initTableOfContents()

    // other page controls heights need to be fixed like this too
    enableKeyboardGestures({
        "leftAction": previousPage,
        "rightAction": nextPage
    })

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
    var zoomSetters = document.getElementsByClassName("ch_set_zoom")
    for (var i = 0; i < zoomSetters.length; i++) {
        setupZoomSetButton(zoomSetters[i])
    }

    initializeMode()

    var savedZoom = getSavedZoom()
    setZoom(savedZoom, false)

    loadCache()

    var startPosition = num(getMeta("startPosition"))

    displayPageFor(parseInt(getMeta("startPosition")))
}