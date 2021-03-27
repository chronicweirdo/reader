

function pan(x, y) {
    setImageLeft(getImageLeft() + x)
    setImageTop(getImageTop() + y)
    updateImage()
}
function zoom(zoom, centerX, centerY) {
    var sideLeft = centerX - getImageLeft()
    var ratioLeft = sideLeft / (getImageWidth() * getZoom())
    var newSideLeft = (getImageWidth() * zoom) * ratioLeft
    setImageLeft(centerX - newSideLeft)

    var sideTop = centerY - getImageTop()
    var ratioTop = sideTop / (getImageHeight() * getZoom())
    var newSideTop = (getImageHeight() * zoom) * ratioTop
    setImageTop(centerY - newSideTop)

    setZoom(zoom)
    setZoomJumpValue(zoom)
    updateImage()
}


function updateImage() {
    if (onMobile() && isAutoFullScreenEnabled()) {
        if (isPageFitToScreen()) {
            unmakeFullScreen()
        } else {
            makeFullScreen()
        }
    }

    var img = getImage()

    if (getZoom() < getMinimumZoom()) setZoom(getMinimumZoom())

    var newWidth = getOriginalImageWidth() * getZoom()
    var newHeight = getOriginalImageHeight() * getZoom()
    setImageWidth(newWidth)
    setImageHeight(newHeight)

    var minimumLeft = (newWidth < getViewportWidth()) ? (getViewportWidth() / 2) - (newWidth / 2) : Math.min(0, getViewportWidth() - newWidth)
    var maximumLeft = (newWidth < getViewportWidth()) ? (getViewportWidth() / 2) - (newWidth / 2) : Math.max(0, getViewportWidth() - newWidth)
    var minimumTop = (newHeight < getViewportHeight()) ? (getViewportHeight() / 2) - (newHeight / 2) : Math.min(0, getViewportHeight() - newHeight)
    var maximumTop = (newHeight < getViewportHeight()) ? (getViewportHeight() / 2) - (newHeight / 2) : Math.max(0, getViewportHeight() - newHeight)

    if (getImageLeft() < minimumLeft) setImageLeft(minimumLeft)
    if (getImageLeft() > maximumLeft) setImageLeft(maximumLeft)
    if (getImageTop() < minimumTop) setImageTop(minimumTop)
    if (getImageTop() > maximumTop) setImageTop(maximumTop)
}
function getImage() {
    return document.getElementsByTagName("img")[0]
}
function getRevertScrollZoom() {
    return true
}
function getScrollSpeed() {
    let s = getSetting(SETTING_COMIC_SCROLL_SPEED)
    return s
}

function getPanSpeed() {
    return 3
}

function getZoomJumpValue() {
    var zoomJump = window.localStorage.getItem("zoomJump")
    if (zoomJump) {
        return parseFloat(zoomJump)
    } else {
        return 2.5
    }
}

function setZoomJumpValue(value) {
    window.localStorage.setItem("zoomJump", value)
}

function zoomJump(x, y) {
    if (isPageFitToScreen()) {
        zoom(getZoomJumpValue(), x, y)
    } else {
        fitPageToScreen()
    }
}

function fitPageToScreenWidth() {
    setZoom(getViewportWidth() / getOriginalImageWidth())
    updateImage()
}

function fitPageToScreenHeight() {
    setZoom(getViewportHeight() / getOriginalImageHeight())
    updateImage()
}

function isPageFitToScreen() {
    return getZoomForFitToScreen() == getZoom()
}

function getZoomForFitToScreen() {
    return Math.min(getViewportHeight() / getOriginalImageHeight(), getViewportWidth() / getOriginalImageWidth())
}

function fitPageToScreen() {
    setZoom(getZoomForFitToScreen())
    updateImage()
}

function setPage(page) {
    if (page < 1) page = 1
    if (page > document.comicMaximumPages) page = document.comicMaximumPages
    updatePositionInput(page)
}

function setImageWidth(width) {
    getImage().width = width
}
function getImageWidth() {
    return getImage().width
}
function setImageHeight(height) {
    getImage().height = height
}
function getImageHeight() {
    return getImage().height
}
function getOriginalImageWidth() {
    return getImage().naturalWidth
}
function getOriginalImageHeight() {
    return getImage().naturalHeight
}
function getHorizontalJumpPercentage() {
    if (getViewportHeight() > getViewportWidth()) return .9
    else return .5
}
function getVerticalJumpPercentage() {
    if (getViewportHeight() > getViewportWidth()) return .5
    else return .9
}
function setImageLeft(left) {
    getImage().style.left = left + "px"
}
function getImageLeft() {
    return num(getImage().style.left, 0)
}
function setImageTop(top) {
    getImage().style.top = top + "px"
}
function getImageTop() {
    return num(getImage().style.top, 0)
}
function setZoom(zoom) {
    document.imageSettings.zoom = zoom
}
function getZoom() {
    return document.imageSettings.zoom
}
// minimum zoom is determined by image and viewport dimensions
function updateMinimumZoom() {
    document.imageSettings.minimumZoom = Math.min(getViewportHeight() / getOriginalImageHeight(), getViewportWidth() / getOriginalImageWidth())
}
function getMinimumZoom() {
    return document.imageSettings.minimumZoom
}

function evictOldest() {
    if (document.comicPageCache) {
        var oldest = null
        var oldestPage = null
        for (let [key, value] of Object.entries(document.comicPageCache)) {
            if (oldest == null) {
                oldest = value.timestamp
                oldestPage = key
            } else if (value.timestamp < oldest) {
                oldest = value.timestamp
                oldestPage = key
            }
        }
        delete document.comicPageCache[oldestPage]
    }
}
function getMaximumCacheSize() {
    return 10
}
function getCacheSize() {
    return Object.keys(document.comicPageCache).length
}
function addToCache(page, data) {
    if (! document.comicPageCache) document.comicPageCache = {}
    document.comicPageCache[page] = {
        "timestamp": + new Date(),
        "data": data
    }
    // evict some old data if cache is too large
    while (getCacheSize() > getMaximumCacheSize()) {
        evictOldest()
    }
}
function getFromCache(page) {
    if (document.comicPageCache && document.comicPageCache[page] && document.comicPageCache[page] != null) {
        return document.comicPageCache[page].data
    } else {
        return null
    }
}
function cacheContains(page) {
    if (document.comicPageCache && document.comicPageCache[page] && document.comicPageCache[page] != null) {
        return true
    } else {
        return false
    }
}

function downloadImageData(page, callback) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200 && this.responseText.length > 0) {
            var jsonResponse = JSON.parse(this.responseText)
            addToCache(page, jsonResponse)
            if (callback != null) {
                callback()
            }
        }
    }
    xhttp.open("GET", "imageData?id=" + getBookId() + "&page=" + (page-1))
    xhttp.send()
}

function updateDownloadUrl() {
    var url = "downloadPage?id=" + getBookId() + "&page=" + (getPositionInput()-1)
    var downloadLink = document.getElementById("downloadPageButton")
    downloadLink.href = url
}

function prefetch(page, callback) {
    if (! cacheContains(page)) {
        downloadImageData(page, callback)
    } else {
        if (callback != null) {
            callback()
        }
    }
}

function getRgb(colorArray) {
    return "rgb(" + colorArray[0] + "," + colorArray[1] + "," + colorArray[2] + ")"
}

function displayPage(page, callback) {
    var timestamp = + new Date()
    showSpinner()
    document.pageDisplayTimestamp = timestamp
    var displayPageInternalCallback = function(data) {
        if (document.pageDisplayTimestamp == timestamp) {
            hideSpinner()
            var img = getImage()
            img.onload = function() {
                document.body.style.background = getRgb(data.color)
                setPage(page)
                saveProgress(getBookId(), page-1)
                setPageTitle(page + "/" + document.comicMaximumPages + " - " + document.bookTitle)
                setImageWidth(getOriginalImageWidth())
                setImageHeight(getOriginalImageHeight())
                setImageLeft(0)
                setImageTop(0)
                updateMinimumZoom()
                updateDownloadUrl()
                if (callback != null) {
                    callback()
                }
                /*prefetch(page+1, function() {
                    prefetch(page+2, function() {
                        prefetch(page+3, function() {
                            prefetch(page-1, function() {
                                prefetch(page-2, null)
                            })
                        })
                    })
                })*/
            }
            img.src = data.image
        }
    }
    /*var imageData = getFromCache(page)
    if (imageData != null) {
        displayPageInternalCallback(imageData)
    } else {*/
        downloadImageData(page, function() {
            displayPageInternalCallback(getFromCache(page))
        })
    /*}*/
}
function setPageTitle(title) {
    document.title = title
}

function approx(val1, val2, threshold = 1) {
    return Math.abs(val1 - val2) < threshold
}

function getRowThreshold() {
    return getImageWidth() * .1
}

function getColumnThreshold() {
    return getImageHeight() * .05
}

function isEndOfRow() {
    return (getImage().width <= getViewportWidth()) || approx(getImageLeft() + getImageWidth(), getViewportWidth(), getRowThreshold())
}
function isBeginningOfRow() {
    return (getImage().width <= getViewportWidth()) || approx(getImageLeft(), 0, getRowThreshold())
}

function isEndOfColumn() {
    return (getImage().height <= getViewportHeight()) || approx(getImageTop() + getImageHeight(), getViewportHeight(), getColumnThreshold())
}
function isBeginningOfColumn() {
    return (getImage().height <= getViewportHeight()) || approx(getImageTop(), 0, getColumnThreshold())
}

function getNextPosition(imageDimension, viewportDimension, imageValue, viewportJumpPercentage, threshold) {
    if (approx(imageValue, viewportDimension - imageDimension, threshold)) return 0
    var proposedNextPosition = (imageValue - viewportDimension *  viewportJumpPercentage) | 0
    if (proposedNextPosition < viewportDimension - imageDimension) return viewportDimension - imageDimension
    return proposedNextPosition
}
function getPreviousPosition(imageDimension, viewportDimension, imageValue, viewportJumpPercentage, threshold) {
    if (approx(imageValue, 0, threshold)) return viewportDimension - imageDimension
    var proposedPreviousPosition = (imageValue + viewportDimension * viewportJumpPercentage) | 0
    if (proposedPreviousPosition > 0) return 0
    return proposedPreviousPosition
}

function getBookId() {
    return document.bookId
}

function goToNextPage() {
    if (getPositionInput() < document.comicMaximumPages) {
        displayPage(getPositionInput() + 1, function() {
            updateImage()
        })
    }
}

function goToPreviousPage() {
    if (getPositionInput() > 1) {
        displayPage(getPositionInput() - 1, function() {
            updateImage()
        })
    }
}

function goToNextView() {
    if (isEndOfRow()) {
        if (isEndOfColumn()) {
            goToNextPage()
        } else {
            setImageLeft(getNextPosition(getImage().width, getViewportWidth(), getImageLeft(), getHorizontalJumpPercentage(), getRowThreshold()))
            setImageTop(getNextPosition(getImage().height, getViewportHeight(), getImageTop(), getVerticalJumpPercentage(), getColumnThreshold()))
            updateImage()
        }
    } else {
        setImageLeft(getNextPosition(getImage().width, getViewportWidth(), getImageLeft(), getHorizontalJumpPercentage(), getRowThreshold()))
        updateImage()
    }
}

function goToPreviousView() {
    if (isBeginningOfRow()) {
        if (isBeginningOfColumn()) {
            goToPreviousPage()
        } else {
            setImageLeft(getPreviousPosition(getImage().width, getViewportWidth(), getImageLeft(), getHorizontalJumpPercentage(), getRowThreshold()))
            setImageTop(getPreviousPosition(getImage().height, getViewportHeight(), getImageTop(), getVerticalJumpPercentage(), getColumnThreshold()))
            updateImage()
        }
    } else {
        setImageLeft(getPreviousPosition(getImage().width, getViewportWidth(), getImageLeft(), getHorizontalJumpPercentage(), getRowThreshold()))
        updateImage()
    }

}

function handleResize() {
    fixComponentHeights()
    updateMinimumZoom()
    updateImage()
}

function jumpToPage(page) {
    displayPage(page, function() {
        updateImage()
    })
}

function goBack() {
    window.history.back();
}

function mouseGestureDrag(mouseButtonPressed, deltaX, deltaY) {
    if (mouseButtonPressed) {
        pan(deltaX * getPanSpeed(), deltaY * getPanSpeed())
    }
}

function mouseGestureScroll(scrollCenterX, scrollCenterY, scrollValue) {
    var zoomDelta = 1 + scrollValue * getScrollSpeed() * (getRevertScrollZoom() ? -1 : 1)
    var newZoom = getZoom() * zoomDelta
    zoom(newZoom, scrollCenterX, scrollCenterY)
}

function touchGesturePinchStart(pinchCenterX, pinchCenterY) {
    document.originalZoom = getZoom()
}

function touchGesturePinchOngoing(currentZoom, pinchCenterX, pinchCenterY) {
    zoom(document.originalZoom * currentZoom, pinchCenterX, pinchCenterY)
}

function touchGesturePan(deltaX, deltaY) {
    pan(deltaX * getPanSpeed(), deltaY * getPanSpeed())
}

function downloadComicToDevice() {
    if('serviceWorker' in navigator) {
        var bookId = getMeta("bookId")
        var pages = num(getMeta("size"))
        navigator.serviceWorker.controller.postMessage({type: 'storeBook', bookId: bookId, maxPositions: pages, kind: 'comic'})
    }
}

window.onload = function() {
    fixComponentHeights()
    enableKeyboardGestures({
        "upAction": () => pan(0, getViewportHeight() / 2),
        "downAction": () => pan(0, - (getViewportHeight() / 2)),
        "leftAction": goToPreviousView,
        "rightAction": goToNextView
    })

    // supported actions:
    // clickAction(mouseX, mouseY)
    // doubleClickAction(mouseX, mouseY)
    // mouseMoveAction(mouseButtonPressed, deltaX, deltaY)
    // scrollAction(scrollCenterX, scrollCenterY, scrollValue)
    // pinchStartAction(pinchCenterX, pinchCenterY)
    // pinchAction(currentZoom, pinchCenterX, pinchCenterY)
    // panAction(deltaX, deltaY)
    //var originalZoom = null

    enableGesturesOnElement(document.getElementById("ch_canv"), {
        "doubleClickAction": zoomJump,
        "mouseMoveAction": mouseGestureDrag,
        "scrollAction": mouseGestureScroll,
        "pinchStartAction": touchGesturePinchStart,
        "pinchAction": touchGesturePinchOngoing,
        "panAction": touchGesturePan
    })

    enableGesturesOnElement(document.getElementById("ch_prev"), {
        "mouseMoveAction": mouseGestureDrag,
        "scrollAction": mouseGestureScroll,
        "pinchStartAction": touchGesturePinchStart,
        "pinchAction": touchGesturePinchOngoing,
        "panAction": touchGesturePan
    })
    document.getElementById("ch_prev").addEventListener("click", (event) => goToPreviousView())
    enableGesturesOnElement(document.getElementById("ch_next"), {
        "mouseMoveAction": mouseGestureDrag,
        "scrollAction": mouseGestureScroll,
        "pinchStartAction": touchGesturePinchStart,
        "pinchAction": touchGesturePinchOngoing,
        "panAction": touchGesturePan
    })
    document.getElementById("ch_next").addEventListener("click", (event) => goToNextView())

    document.getElementById("ch_tools_left").addEventListener("click", (event) => toggleTools(true))
    document.getElementById("ch_tools_right").addEventListener("click", (event) => toggleTools(false))
    document.getElementById("ch_tools_container").addEventListener("click", (event) => toggleTools())

    addPositionInputTriggerListener(jumpToPage)

    document.bookId = getMeta("bookId")
    document.bookTitle = getMeta("bookTitle")
    document.comicMaximumPages = num(getMeta("size"))
    document.imageSettings = {}
    setZoom(1.0)

    loadProgress(currentPosition => {
        var startPage = currentPosition + 1
        displayPage(startPage, function() {
            var fit = getMeta("defaultFit")
            if (fit == "width") {
                fitPageToScreenWidth()
            } else if (fit = "screen") {
                fitPageToScreen()
            }
        })
    })

    downloadComicToDevice()
}