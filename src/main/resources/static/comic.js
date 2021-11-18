var panEnabled = true
var pinching = false
var panX = 0
var panY = 0
var swipeNextPossible = false
var swipePreviousPossible = false

function pan(x, y, currentX, currentY) {
    if (getSetting(SETTING_SWIPE_PAGE)) {
        let horizontalThreshold = getViewportWidth() * getSetting(SETTING_SWIPE_LENGTH)
        let verticalMoveValid = Math.abs(panY - currentY) < (getViewportHeight() * getSetting(SETTING_SWIPE_VERTICAL_THRESHOLD))
        let deltaX = panX - currentX
        let deltaY = panY - currentY
        if (swipeNextPossible && deltaX < 0 ) swipeNextPossible = false
        if (swipePreviousPossible && deltaX > 0 ) swipePreviousPossible = false
        if (panEnabled && !pinching && verticalMoveValid && deltaX > horizontalThreshold && swipeNextPossible) {
            panEnabled = false
            goToNextView()
        } else if (panEnabled && !pinching && verticalMoveValid && deltaX < -horizontalThreshold && swipePreviousPossible) {
            panEnabled = false
            goToPreviousView()
        } else if (panEnabled) {
            setImageLeft(getImageLeft() + x)
            setImageTop(getImageTop() + y)
            updateImage()
        }
    } else {
        setImageLeft(getImageLeft() + x)
        setImageTop(getImageTop() + y)
        updateImage()
    }
}

function touchGestureStartPan(x, y) {
    if (getSetting(SETTING_SWIPE_PAGE)) {
        panX = x
        panY = y
        if (isEndOfRow() && isEndOfColumn()) swipeNextPossible = true
        if (isBeginningOfRow() && isBeginningOfColumn()) swipePreviousPossible = true
    }
}

function touchGestureEndPan() {
    panEnabled = true
    swipeNextPossible = false
    swipePreviousPossible = false
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
    return getSetting(SETTING_COMIC_INVERT_SCROLL)
}

function getScrollSpeed() {
    return getSetting(SETTING_COMIC_SCROLL_SPEED)
}

function getPanSpeed() {
    return getSetting(SETTING_COMIC_PAN_SPEED)
}

function getZoomJumpValue() {
    var zoomJump = window.localStorage.getItem("zoomJump")
    if (zoomJump) {
        return parseFloat(zoomJump)
    } else {
        return 1.0
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
    return getSetting(SETTING_COMIC_HORIZONTAL_JUMP)
}

function getVerticalJumpPercentage() {
    return getSetting(SETTING_COMIC_VERTICAL_JUMP)
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

function downloadImageData(page, callback) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200 && this.responseText.length > 0) {
                var jsonResponse = JSON.parse(this.responseText)
                if (callback != null) {
                    callback(jsonResponse)
                }
            } else {
                reportError(this.status + " " + this.responseText)
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

function displayPage(page, callback) {
    let displayPageInternal = function(page, callback) {
        document.lastPageChange = timestamp
        document.pageDisplayTimestamp = timestamp
        var displayPageInternalCallback = function(data) {
            if (document.pageDisplayTimestamp == timestamp) {
                document.pageDisplayTimestamp = null
                hideSpinner()
                var img = getImage()
                img.onload = function() {
                    document.getElementById("content").style.background = getHexCode(data.color)
                    setStatusBarColor(getHexCode(data.color))
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
                }
                img.src = data.image
            }
        }
        downloadImageData(page, displayPageInternalCallback)
        window.setTimeout(function() {
            if (document.pageDisplayTimestamp == timestamp) {
                showSpinner()
            }
        }, 100)
    }

    var timestamp = + new Date()
    if (document.lastPageChange == undefined) {
        window.location.reload()
    }
    let difference = timestamp - document.lastPageChange
    if (difference > REFRESH_PAGE_TIME_DIFFERENCE) {
        // load progress
        // if progress is equal to current position, continue as normal, if not reload page
        showSpinner()
        loadProgress(currentPosition => {
            if (currentPosition != getPositionInput() - 1) {
                window.location.reload()
            } else {
                displayPageInternal(page, callback)
            }
        })
    } else {
        displayPageInternal(page, callback)
    }
}

function setPageTitle(title) {
    document.title = title
}

function approx(val1, val2, threshold = 1) {
    return Math.abs(val1 - val2) < threshold
}

function getRowThreshold() {
    return getImageWidth() * getSetting(SETTING_COMIC_ROW_THRESHOLD)
}

function getColumnThreshold() {
    return getImageHeight() * getSetting(SETTING_COMIC_COLUMN_THRESHOLD)
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

function getLastPosition(imageDimension, viewportDimension, imageValue, viewportJumpPercentage, threshold) {
    return viewportDimension - imageDimension
}

function getNextPosition(imageDimension, viewportDimension, imageValue, viewportJumpPercentage, threshold) {
    if (approx(imageValue, viewportDimension - imageDimension, threshold)) return 0
    var proposedNextPosition = (imageValue - viewportDimension * viewportJumpPercentage) | 0
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

function goToPreviousPage(proposedLeft = undefined, proposedTop = undefined) {
    if (getPositionInput() > 1) {
        displayPage(getPositionInput() - 1, function() {
            if (proposedLeft) setImageLeft(proposedLeft)
            if (proposedTop) setImageTop(proposedTop)
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
            let lastLeft = getLastPosition(getImage().width, getViewportWidth(), getImageLeft(), getHorizontalJumpPercentage(), getRowThreshold())
            let lastTop = getLastPosition(getImage().height, getViewportHeight(), getImageTop(), getVerticalJumpPercentage(), getColumnThreshold())
            goToPreviousPage(lastLeft, lastTop)
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
    var zoomDelta = 1 + scrollValue * getScrollSpeed() * (getRevertScrollZoom() ? 1 : -1)
    var newZoom = getZoom() * zoomDelta
    zoom(newZoom, scrollCenterX, scrollCenterY)
}

function touchGesturePinchStart(pinchCenterX, pinchCenterY) {
    pinching = true
    document.originalZoom = getZoom()
}

function touchGesturePinchOngoing(currentZoom, pinchCenterX, pinchCenterY) {
    zoom(document.originalZoom * currentZoom, pinchCenterX, pinchCenterY)
}

function touchGesturePinchEnd() {
    pinching = false
}

function touchGesturePan(deltaX, deltaY, currentX, currentY) {
    pan(deltaX * getPanSpeed(), deltaY * getPanSpeed(), currentX, currentY)
}

function downloadComicToDevice() {
    if('serviceWorker' in navigator) {
        var bookId = getMeta("bookId")
        var pages = num(getMeta("size"))
        navigator.serviceWorker.controller.postMessage({type: 'storeBook', bookId: bookId, maxPositions: pages, kind: 'comic'})
    }
}

function getDownloadPageButton() {
    let label = document.createElement('span')
    label.innerHTML = ""
    let button = document.createElement('a')
    button.id = 'downloadPageButton'
    button.innerHTML = 'download'

    return [label, button]
}

function initSettings() {
    let settingsWrapper = document.getElementById('ch_settings')
    appendAll(settingsWrapper, getDownloadPageButton())
    appendAll(settingsWrapper, getSettingController(SETTING_COMIC_HORIZONTAL_JUMP))
    appendAll(settingsWrapper, getSettingController(SETTING_COMIC_VERTICAL_JUMP))
    appendAll(settingsWrapper, getSettingController(SETTING_COMIC_ROW_THRESHOLD))
    appendAll(settingsWrapper, getSettingController(SETTING_COMIC_COLUMN_THRESHOLD))
    appendAll(settingsWrapper, getSettingController(SETTING_COMIC_INVERT_SCROLL))
    appendAll(settingsWrapper, getSettingController(SETTING_COMIC_SCROLL_SPEED))
    appendAll(settingsWrapper, getSettingController(SETTING_COMIC_PAN_SPEED))
    appendAll(settingsWrapper, getSettingController(SETTING_SWIPE_PAGE))
    appendAll(settingsWrapper, getSettingController(SETTING_SWIPE_VERTICAL_THRESHOLD))
    appendAll(settingsWrapper, getSettingController(SETTING_SWIPE_LENGTH))
    appendAll(settingsWrapper, getRemoveProgressButton())
    appendAll(settingsWrapper, getMarkAsReadButton())
}

window.onload = function() {
    document.documentElement.style.setProperty('--accent-color', getSetting(SETTING_ACCENT_COLOR));
    document.documentElement.style.setProperty('--foreground-color', getSetting(SETTING_FOREGROUND_COLOR));
    document.documentElement.style.setProperty('--background-color', getSetting(SETTING_BACKGROUND_COLOR));

    fixComponentHeights()
    enableKeyboardGestures({
        "upAction": () => pan(0, getViewportHeight() / 2),
        "downAction": () => pan(0, - (getViewportHeight() / 2)),
        "leftAction": goToPreviousView,
        "rightAction": goToNextView,
        "escapeAction": () => toggleTools(true)
    })

    enableGesturesOnElement(document.getElementById("ch_canv"), {
        "doubleClickAction": zoomJump,
        "mouseMoveAction": mouseGestureDrag,
        "scrollAction": mouseGestureScroll,
        "pinchStartAction": touchGesturePinchStart,
        "pinchAction": touchGesturePinchOngoing,
        "pinchEndAction": touchGesturePinchEnd,
        "panStartAction": touchGestureStartPan,
        "panAction": touchGesturePan,
        "panEndAction": touchGestureEndPan
    })

    enableGesturesOnElement(document.getElementById("ch_prev"), {
        "mouseMoveAction": mouseGestureDrag,
        "scrollAction": mouseGestureScroll,
        "pinchStartAction": touchGesturePinchStart,
        "pinchAction": touchGesturePinchOngoing,
        "pinchEndAction": touchGesturePinchEnd,
        "panStartAction": touchGestureStartPan,
        "panAction": touchGesturePan,
        "panEndAction": touchGestureEndPan
    })
    document.getElementById("ch_prev").addEventListener("click", (event) => goToPreviousView())
    enableGesturesOnElement(document.getElementById("ch_next"), {
        "mouseMoveAction": mouseGestureDrag,
        "scrollAction": mouseGestureScroll,
        "pinchStartAction": touchGesturePinchStart,
        "pinchAction": touchGesturePinchOngoing,
        "pinchEndAction": touchGesturePinchEnd,
        "panStartAction": touchGestureStartPan,
        "panAction": touchGesturePan,
        "panEndAction": touchGestureEndPan
    })
    document.getElementById("ch_next").addEventListener("click", (event) => goToNextView())

    document.getElementById("ch_tools_left").addEventListener("click", (event) => toggleTools(true))
    document.getElementById("ch_tools_right").addEventListener("click", (event) => toggleTools(false))
    document.getElementById("ch_tools_container").addEventListener("click", (event) => toggleTools())
    document.getElementById("ch_tools").addEventListener("click", event => event.stopPropagation())

    addPositionInputTriggerListener(jumpToPage)

    document.bookId = getMeta("bookId")
    document.bookTitle = getMeta("bookTitle")
    document.comicMaximumPages = num(getMeta("size"))
    document.imageSettings = {}
    setZoom(1.0)

    initSettings()
    initFullscreenButton()
    initBookCollectionLinks()

    document.lastPageChange = new Date()
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