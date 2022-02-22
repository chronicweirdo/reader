var panX = 0
var panY = 0
var swipeStart = false

function touchGestureStartPan(event) {
    if (event.touches.length == 1 && window.getSelection().type != "Range") {
        panX = event.touches[0].pageX
        panY = event.touches[0].pageY
        swipeStart = true
    }
}

function touchGesturePan(event) {
    if (SETTING_SWIPE_PAGE.get() && event.touches.length == 1 && window.getSelection().type != "Range" && swipeStart) {
        let newX = event.touches[0].pageX
        let newY = event.touches[0].pageY
        let deltaX = newX - panX
        let deltaY = newY - panY
        let swipeParameters = computeSwipeParameters(deltaX, deltaY)

        let horizontalThreshold = getViewportWidth() * SETTING_SWIPE_LENGTH.get()
        let verticalMoveValid = swipeParameters.angle < SETTING_SWIPE_ANGLE_THRESHOLD.get()
        if (verticalMoveValid && deltaX < -horizontalThreshold) {
            swipeStart = false
            nextPage()
        } else if (verticalMoveValid && deltaX > horizontalThreshold) {
            swipeStart = false
            previousPage()
        }
    }
}

function imageLoadedPromise(image) {
    return new Promise((resolve, reject) => {
        let imageResolveFunction = function() {
            resolve()
        }
        image.onload = imageResolveFunction
        image.onerror = imageResolveFunction
    })
}

function scrollNecessaryPromise(el) {
    return new Promise((resolve, reject) => {
        var images = el.getElementsByTagName('img')
        var imageCount = images.length
        if (imageCount > 0) {
            let imagePromises = []
            for (var i = 0; i < imageCount; i++) {
                imagePromises.push(imageLoadedPromise(images[i]))
            }
            Promise.all(imagePromises).then(() => {
                if (el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth) resolve(true)
                else resolve(false)
            })
        } else {
            if (el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth) resolve(true)
            else resolve(false)
        }
    })
}

function getPageFor(position, withIndex = false) {
    var savedPages = document.savedPages
    if (savedPages != null) {
        // search for page
        for (var i = 0; i < savedPages.length; i++) {
            if (savedPages[i].start <= position && position <= savedPages[i].end) {
                // we found the page
                let page = savedPages[i]
                if (withIndex) {
                    page.index = i
                }
                return page
            }
        }
    }
    // no page available
    return null
}

function getRemainingPagesInChapter() {
    if (document.currentPage
        && document.section != null
        && document.section.start <= document.currentPage.end
        && document.currentPage.end <= document.section.end) {

        let currentNode = document.section.leafAtPosition(document.currentPage.end)
        let nextHeader = currentNode.nextNodeOfName("h1")
        let startPage = getPageFor(document.currentPage.end, true)
        let endPage
        if (nextHeader) {
            endPage = getPageFor(nextHeader.start, true)
        } else {
            endPage = getPageFor(document.section.end, true)
        }
        if (endPage) {
            let pagesLeft = endPage.index - startPage.index
            return pagesLeft
        }
    }
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



async function displayPageFor(position) {
    let displayPageForInternal = async function(position) {
        document.lastPageChange = now

        showSpinner()
        let page = getPageFor(position)
        if (page == null) {
            computePagesForSection(position)
            page = await getPageForPromise(position)
        }

        getContentFor(page.start, page.end, function(text) {
            var content = document.getElementById("ch_content")
            content.innerHTML = text
            document.currentPage = page
            // if book end is displayed, we mark the book as read
            if (page.end == parseInt(getMeta("bookEnd"))) {
                saveProgress(getMeta("bookId"), page.end)
            } else {
                // don't save progress again if the current progress is on this page
                if (document.currentPosition == undefined || document.currentPosition == 0 || document.currentPosition < page.start || page.end < document.currentPosition) {
                    saveProgress(getMeta("bookId"), page.start)
                }
            }
            updatePositionInput(getPositionPercentage(page.start, page.end))
            updatePagesLeft()
            //initializeMode() todo: check and update theme?
            //configureTheme()
            setUiColors()
            // check if overflow is triggerred on every page display
            scrollNecessaryPromise(content).then(scrollNecessary => {
                if (scrollNecessary) {
                    resetPagesForSection()
                    displayPageFor(position)
                } else {
                    hideSpinner()
                }
            })
        })
    }

    let now = new Date()
    if (document.lastPageChange == undefined) {
        window.location.reload()
    }
    let difference = now - document.lastPageChange
    if (difference > REFRESH_PAGE_TIME_DIFFERENCE) {
        showSpinner()
        loadProgress(function(currentPosition) {
            document.currentPosition = currentPosition
            if (currentPosition < document.currentPage.start || document.currentPage.end < currentPosition) {
                window.location.reload()
            } else {
                // continue as normal
                displayPageForInternal(position)
            }
        })
    } else {
        displayPageForInternal(position)
    }
}

function updatePagesLeft() {
    let el = document.getElementById("pagesLeft")
    el.innerHTML = ""

    let remainingPages = getRemainingPagesInChapter()
    if (remainingPages != undefined) {
        let span = document.createElement("span")
        text = remainingPages
        if (remainingPages == 1) {
            text += " page "
        } else {
            text += " pages "
        }
        text += "left in chapter"
        span.innerHTML = text
        el.appendChild(span)
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
    fixControlSizes()
    if (document.currentPage != null) {
        var position = document.currentPage.start
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
        if (this.readyState == 4) {
            if (this.status == 200) {
                var jsonObj = JSON.parse(this.responseText)
                var node = convert(jsonObj)
                if (callback != null) {
                    callback(node)
                }
            } else {
                reportError(this.status + " " + this.responseText)
            }
        }
    }
    xhttp.open("GET", "bookSection?id=" + getMeta("bookId") + "&position=" + position)
    xhttp.send()
}

function downloadBookToDevice() {
    if('serviceWorker' in navigator) {
        var bookId = getMeta("bookId")
        var size = num(getMeta("size"))
        navigator.serviceWorker.controller.postMessage({type: 'storeBook', bookId: bookId, maxPositions: size, kind: 'book'})
    }
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
        compute(section, section.start)
    })
}

function resetPagesForSection() {
    if (document.section) {
        let start = document.section.start
        let end = document.section.end
        let remainingPages = document.savedPages.filter(page => page.end < start || end < page.start)
        document.savedPages = remainingPages
        saveCache()
    }
}

function savePage(start, end) {
    if (document.savedPages == null) {
        document.savedPages = []
    }
    document.savedPages.push({start: start, end: end})
}

function timeout(ms) {
    return new Promise((resolve, reject) => {
        window.setTimeout(function() {
            resolve()
        }, ms)
    })
}

function getPageForPromise(position) {
    return new Promise((resolve, reject) => {
        let page = getPageFor(position)
        if (page == null) {
            timeout(100).then(() => resolve(getPageForPromise(position)))
        } else {
            resolve(page)
        }
    })
}

async function compute(section, start) {
    let shadowContent = document.getElementById("ch_shadow_content")
    shadowContent.innerHTML = ""

    //let firstEnd = section.findSpaceAfter(start)
    let firstEnd = start
    let end = firstEnd
    let previousEnd = firstEnd
    shadowContent.innerHTML = section.copy(start, end).getContent()
    let overflow = await scrollNecessaryPromise(shadowContent)

    while ((!overflow) && (end < section.end)) {
        previousEnd = end
        end = section.findSpaceAfter(end)
        shadowContent.innerHTML = section.copy(start, end).getContent()
        overflow = await scrollNecessaryPromise(shadowContent)
    }

    // we have a page
    if (end < section.end) {
        savePage(start, previousEnd)
        timeout(10).then(() => compute(section, previousEnd + 1))
    } else {
        savePage(start, end)
        saveCache()
        updatePagesLeft()
    }
}

function getBookStyleSheet() {
    let styleSheets = window.document.styleSheets
    for (let i = 0; i < styleSheets.length; i++) {
        if (styleSheets[i].href.endsWith("book.css")) {
            return styleSheets[i]
        }
    }
}

/*function setDarkMode() {
    let background = SETTING_DARK_MODE_BACKGROUND.get()
    let foreground = SETTING_DARK_MODE_FOREGROUND.get()
    setUiColors(foreground, background)
}*/

function createDynamicStyleSheet() {
    var sheet = (function() {
        var style = document.createElement("style");
        // WebKit hack
        style.appendChild(document.createTextNode(""));
        document.head.appendChild(style);
        return style.sheet;
    })();
    document.dynamicStyleSheet = sheet
}

/*function setLightMode() {
    let background = SETTING_LIGHT_MODE_BACKGROUND.get()
    let foreground = SETTING_LIGHT_MODE_FOREGROUND.get()
    setUiColors(foreground, background)
}*/

function setUiColors() {
    /*let foreground = "#000000"
    let background = "#ffffff"
    if (getTheme() == "dark") {
        foreground = SETTING_DARK_TEXT_COLOR.get()
        background = SETTING_DARK_BACKGROUND_COLOR.get()
    } else {
        foreground = SETTING_LIGHT_TEXT_COLOR.get()
        background = SETTING_LIGHT_BACKGROUND_COLOR.get()
    }
    console.log("set ui colors to: " + foreground + " " + background)*/
    let bookStyleSheet = document.dynamicStyleSheet
    if (bookStyleSheet) {
        while (bookStyleSheet.cssRules.length > 0) bookStyleSheet.deleteRule(0)
        //bookStyleSheet.insertRule('#content { color: ' + foreground + '; background-color: ' + background + '; }', 0)
        bookStyleSheet.insertRule('#content { color: var(--text-color, black); background-color: var(--background-color, white); }', 0)
        //bookStyleSheet.insertRule('a { color: ' + foreground + '; }', 0)
        bookStyleSheet.insertRule('a { color: var(--text-color, black); }', 0)
        //bookStyleSheet.insertRule('table, th, td { border-color: ' + foreground + '; }', 0)
        bookStyleSheet.insertRule('table, th, td { border-color: var(--text-color, black); }', 0)
        //setStatusBarColor(background)
    }
}

/*function initializeMode() {
    let bookMode = SETTING_BOOK_MODE.get()
    if (bookMode == 0) {
        setDarkMode()
    } else if (bookMode == 2) {
        setLightMode()
    } else {
        let dayStart = timeStringToDate(SETTING_DAY_START.get())
        let dayEnd = timeStringToDate(SETTING_DAY_END.get())
        let now = new Date()
        if (now < dayStart || dayEnd < now) {
            setDarkMode()
        } else {
            setLightMode()
        }
    }
}*/

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
    return document.chapters.sort((a, b) => parseFloat(a.start) - parseFloat(b.start));
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
    document.getElementById("content").style["font-size"] = zoom + "rem"
    if (withResize) handleResize()
}

function getBookPagesCacheKey() {
    var pagesKey = "bookPages_" + getMeta("bookId") + "_" + getViewportWidth() + "_" + getViewportHeight() + "_" + SETTING_BOOK_ZOOM.get() + "_" + SETTING_BOOK_EDGE_HORIZONTAL.get() + "_" + SETTING_BOOK_EDGE_VERTICAL.get()
    return pagesKey
}

function loadCache() {
    var cacheKey = getBookPagesCacheKey()
    var cache = window.localStorage.getItem(cacheKey)
    if (cache != null) {
        document.savedPages = JSON.parse(cache)
    } else {
        document.savedPages = []
    }
}

function saveCache() {
    var cacheKey = getBookPagesCacheKey()
    window.localStorage.setItem(cacheKey, JSON.stringify(document.savedPages))
}

function initSettings() {
    let settingsWrapper = document.getElementById('ch_settings')
    settingsWrapper.appendChild(SETTING_BOOK_ZOOM.controller)

    SETTING_BOOK_ZOOM.addListener((zoom) => setTimeout(function() { setZoom(zoom)}, 1000))

    settingsWrapper.appendChild(getRemoveProgressButton())
    settingsWrapper.appendChild(getMarkAsReadButton())
}

window.onload = function() {
    //document.documentElement.style.setProperty('--accent-color', SETTING_ACCENT_COLOR.get());
    //document.documentElement.style.setProperty('--foreground-color', SETTING_FOREGROUND_COLOR.get());
    //document.documentElement.style.setProperty('--background-color', SETTING_BACKGROUND_COLOR.get());
    configureTheme()
    setUiColors()

    createDynamicStyleSheet()
    fixControlSizes()
    initTableOfContents()
    initSettings()
    initFullscreenButton()
    initBookCollectionLinks()

    // other page controls heights need to be fixed like this too
    enableKeyboardGestures({
        "leftAction": previousPage,
        "rightAction": nextPage,
        "escapeAction": () => toggleTools(true, prepareBookTools)
    })
    document.getElementById("ch_content").addEventListener('touchstart', touchGestureStartPan, false);
    document.getElementById("ch_content").addEventListener('touchmove', touchGesturePan, false);

    document.getElementById("ch_prev").addEventListener("click", (event) => previousPage())
    document.getElementById("ch_next").addEventListener("click", (event) => nextPage())

    document.getElementById("ch_tools_left").addEventListener("click", (event) => toggleTools(true, prepareBookTools))
    document.getElementById("ch_tools_right").addEventListener("click", (event) => toggleTools(false, prepareBookTools))
    document.getElementById("ch_tools_container").addEventListener("click", (event) => hideTools())
    document.getElementById("ch_tools").addEventListener("click", event => event.stopPropagation())

    //initializeMode()
    initAlpha()
    window.addEventListener("focus", function(event) {
        //initializeMode()
        configureTheme() // todo: check if theme needs to change, then configure it
        setUiColors()
    }, false)
    setZoom(SETTING_BOOK_ZOOM.get(), false)
    loadCache()

    document.lastPageChange = new Date()
    timeout(100).then(() => {loadProgress(function(currentPosition) {
        document.currentPosition = currentPosition
        displayPageFor(currentPosition)
    })})

    downloadBookToDevice()
}