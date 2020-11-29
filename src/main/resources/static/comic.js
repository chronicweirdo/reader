

function pan(x, y) {
    setImageLeft(getImageLeft() + x)
    setImageTop(getImageTop() + y)
    updateImage()
}
function zoom(zoom, centerX, centerY) {
    var oldRatio = getViewportWidth() / getZoom()
    var currentRatio = getViewportWidth() / zoom

    var sideLeft = centerX - getImageLeft()
    var ratioLeft = sideLeft / (getImageWidth() * oldRatio)
    var newSideLeft = (getImageWidth() * currentRatio) * ratioLeft
    setImageLeft(centerX - newSideLeft)

    var sideTop = centerY - getImageTop()
    var ratioTop = sideTop / (getImageHeight() * oldRatio)
    var newSideTop = (getImageHeight() * currentRatio) * ratioTop
    setImageTop(centerY - newSideTop)

    setZoom(zoom)
    //setZoomJumpValue(zoom / getZoomForFitToScreen())
    updateImage()
}
function toggleFullScreen() {
    var doc = window.document;
    var docEl = doc.documentElement;

    var requestFullScreen = docEl.requestFullscreen || docEl.mozRequestFullScreen || docEl.webkitRequestFullScreen || docEl.msRequestFullscreen;
    var cancelFullScreen = doc.exitFullscreen || doc.mozCancelFullScreen || doc.webkitExitFullscreen || doc.msExitFullscreen;

    if(!doc.fullscreenElement && !doc.mozFullScreenElement && !doc.webkitFullscreenElement && !doc.msFullscreenElement) {
        requestFullScreen.call(docEl);
    }
    else {
        cancelFullScreen.call(doc);
    }
}
function makeFullScreen() {
    var doc = window.document;
    var docEl = doc.documentElement;

    var requestFullScreen = docEl.requestFullscreen || docEl.mozRequestFullScreen || docEl.webkitRequestFullScreen || docEl.msRequestFullscreen;

    requestFullScreen.call(docEl);
}
function unmakeFullScreen() {
    var doc = window.document;

    var cancelFullScreen = doc.exitFullscreen || doc.mozCancelFullScreen || doc.webkitExitFullscreen || doc.msExitFullscreen;

    cancelFullScreen.call(doc);
}

function isAutoFullScreenEnabled() {
    return false;
}

function setFitToScreen(val) {
    document.isFitToScreenValue = val
}

function isFitToScreen() {
    return document.isFitToScreenValue
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

    if (getZoom() > getMinimumZoom()) setZoom(getMinimumZoom())

    /*
        zoom means how many of the original image width, in pixels, is visible on screen
        if zoom is less than the image width, only part of the image is visible on screen, so the width needs to be larger than the viewport width
        if zoom is more, the whole image width is visible

       zoom value       original image width
       ------------  =  --------------------
       screen width     current image width

       current image width = screen width * original image width / zoom value
    */
    //var ratio = getOriginalImageWidth() / getZoom()
    var currentZoom = getZoom()
    //if (isFitToScreen()) currentZoom = getMinimumZoom()
    //console.log(currentZoom)

    //var newWidth = getOriginalImageWidth() * getZoom()
    var newWidth = (getViewportWidth() / currentZoom) * getOriginalImageWidth()
    //var newHeight = getOriginalImageHeight() * getZoom()
    var newHeight = (getViewportWidth() / currentZoom) * getOriginalImageHeight()
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
    //return .5 * .1
    return 30
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

/*function setZoomJumpValue(value) {
    window.localStorage.setItem("zoomJump", value)
}*/

function zoomJump(x, y) {
    /*if (isFitToScreen()) {
        setFitToScreen(false)
        zoom(getZoom(), x, y)
    } else {
        setFitToScreen(true)
        updateImage()
    }*/

    /*if (isPageFitToScreen()) {
        zoom(getZoom() * getZoomJumpValue(), x, y)
    } else {
        fitPageToScreen()
    }*/

    if (getZoom() == getMinimumZoom()) {
        // we are zoomed out
        // get old zoom value from storage
        var oldZoom = parseFloat(window.localStorage.getItem("comicZoomJump"))
        if (!oldZoom) oldZoom = getMinimumZoom() / 2
        zoom(oldZoom, x, y)
    } else {
        // we are zoomed in
        // save current zoom value to storage
        window.localStorage.setItem("comicZoomJump", getZoom())
        // set zoom to minimum
        setZoom(getMinimumZoom())
        updateImage()
    }


    // save current zoom

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
    window.localStorage.setItem("comicZoom", zoom)
}
function getZoom() {
    if (document.imageSettings.zoom) {
        return document.imageSettings.zoom
    } else {
        var fromStorage = parseFloat(window.localStorage.getItem("comicZoom"))
        if (fromStorage) {
            document.imageSettings.zoom = fromStorage
            return document.imageSettings.zoom
        } else {
            setZoom(getOriginalImageWidth())
            return document.imageSettings.zoom
        }
    }

}
// minimum zoom is determined by image and viewport dimensions
// this is the zoom necessary to display the whole image on the screen
function updateMinimumZoom() {
    //document.imageSettings.minimumZoom = Math.min(getViewportHeight() / getOriginalImageHeight(), getViewportWidth() / getOriginalImageWidth())

    // what should the zoom be so that the width fits in the screen
    var widthZoom = getOriginalImageWidth()

    // ratio so that height fits in screen
    var ratio = getViewportHeight() / getOriginalImageHeight()
    var heightZoom = getViewportWidth() / ratio
    document.imageSettings.minimumZoom = Math.max(widthZoom, heightZoom)
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
            addToCache(page, this.responseText)
            if (callback != null) {
                callback()
            }
        }
    }
    xhttp.open("GET", "imageData?id=" + getBookId() + "&page=" + (page-1), true)
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

function displayPage(page, callback) {
    var timestamp = + new Date()
    showSpinner()
    document.pageDisplayTimestamp = timestamp
    var displayPageInternalCallback = function(data) {
        if (document.pageDisplayTimestamp == timestamp) {
            hideSpinner()
            var img = getImage()
            img.onload = function() {
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
                prefetch(page+1, function() {
                    prefetch(page+2, function() {
                        prefetch(page+3, function() {
                            prefetch(page-1, function() {
                                prefetch(page-2, null)
                            })
                        })
                    })
                })
            }
            img.src = data
        }
    }
    var imageData = getFromCache(page)
    if (imageData != null) {
        displayPageInternalCallback(imageData)
    } else {
        downloadImageData(page, function() {
            displayPageInternalCallback(getFromCache(page))
        })
    }
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
    return getImageHeight() * .1
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
    var newZoom = getZoom() - zoomDelta
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
    //setZoom(1.0)
    //setFitToScreen(true)
    var startPage = num(getMeta("startPosition")) + 1

    displayPage(startPage, function() {
        /*var fit = getMeta("defaultFit")
        if (fit == "width") {
            fitPageToScreenWidth()
        } else if (fit = "screen") {
            fitPageToScreen()
        }*/
        //setZoom(getMinimumZoom())
        updateImage()
    })
}

/*
New approach for zoom, we have two modes:
- fit page to screen
- fit page to zoom value
    - zoom is now a number representing the number of pixels of the original image width visible in the screen width
*/