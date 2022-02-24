
/////////////////////////////////////////////////////////////////////////////////////////////////////////////// COMIC ↓

class Comic {
    constructor(id, title, size) {
        this.id = id
        this.title = title
        this.size = size
        this.lastPageChange = Date.now()
    }
    setPage(page) {
        if (page < 1) page = 1
        if (page > this.size) page = this.size
        updatePositionInput(page)
    }
    setDocumentTitle(value) {
        document.title = value
    }
    updateDownloadUrl() {
        let url = "downloadPage?id=" + this.id + "&page=" + (getPositionInput()-1)
        let downloadLink = document.getElementById("downloadPageButton")
        downloadLink.href = url
    }
    displayPage(page, callback) {
        let self = this
        let displayPageInternal = function(page, callback) {
            self.lastPageChange = timestamp
            self.pageDisplayTimestamp = timestamp

            var displayPageInternalCallback = function(data) {
                if (self.pageDisplayTimestamp == timestamp) {
                    self.pageDisplayTimestamp = null
                    hideSpinner()
                    var img = getImage().image
                    img.onload = function() {
                        document.getElementById("content").style.background = getHexCode(data.color)
                        setStatusBarColor(getHexCode(data.color))
                        self.setPage(page)
                        saveProgress(self.id, page-1)
                        self.setDocumentTitle(page + "/" + self.size + " - " + self.title)
                        getImage().reset()
                        self.updateDownloadUrl()
                        checkAndUpdateTheme()
                        if (callback != null) {
                            callback()
                        }
                    }
                    img.src = data.image
                }
            }
            self.downloadImageData(page, displayPageInternalCallback)
            window.setTimeout(function() {
                if (self.pageDisplayTimestamp == timestamp) {
                    showSpinner()
                }
            }, 100)
        }

        var timestamp = + new Date()
        if (self.lastPageChange == undefined) {
            window.location.reload()
        }
        let difference = timestamp - self.lastPageChange
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
    downloadImageData(page, callback) {
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
        xhttp.open("GET", "imageData?id=" + this.id + "&page=" + (page - 1))
        xhttp.send()
    }
    goToNextPage() {
        let currentPosition = getPositionInput()
        if (currentPosition < this.size) {
            this.displayPage(currentPosition + 1, function() {
                if (SETTING_FIT_COMIC_TO_SCREEN.get()) {
                    getImage().fitPageToScreen()
                } else {
                    getImage().update()
                }
            })
        }
    }
    goToPreviousPage(proposedLeft = undefined, proposedTop = undefined) {
        let currentPosition = getPositionInput()
        if (currentPosition > 1) {
            this.displayPage(currentPosition - 1, function() {
                if (SETTING_FIT_COMIC_TO_SCREEN.get()) {
                    getImage().fitPageToScreen()
                } else {
                    if (proposedLeft) getImage().setLeft(proposedLeft)
                    if (proposedTop) getImage().setTop(proposedTop)
                    getImage().update()
                }
            })
        }
    }
    jumpToPage(page) {
        this.displayPage(page, function() {
            getImage().update()
        })
    }
}

function getComic() {
    if (document.comic) {
        return document.comic
    } else {
        document.comic = new Comic(getMeta("bookId"), getMeta("bookTitle"), num(getMeta("size")))
        return document.comic
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////// COMIC ↑


/////////////////////////////////////////////////////////////////////////////////////////////////////////////// IMAGE ↓

class Image {
    constructor(element) {
        this.image = element
        this.zoomValue = 1
        this.swipeNextPossible = false
        this.swipePreviousPossible = false
    }
    setWidth(width) {
        this.image.width = width
    }
    getWidth() {
        return this.image.width
    }
    setHeight(height) {
        this.image.height = height
    }
    getHeight() {
        return this.image.height
    }
    getOriginalWidth() {
        return this.image.naturalWidth
    }
    getOriginalHeight() {
        return this.image.naturalHeight
    }
    setLeft(left) {
        this.image.style.left = left + "px"
    }
    addLeft(x) {
        this.setLeft(this.getLeft() + x)
    }
    getLeft() {
        return num(this.image.style.left, 0)
    }
    setTop(top) {
        this.image.style.top = top + "px"
    }
    addTop(y) {
        this.setTop(this.getTop() + y)
    }
    getTop() {
        return num(this.image.style.top, 0)
    }
    setZoom(zoom) {
        this.zoomValue = zoom
    }
    getZoom() {
        return this.zoomValue
    }
    // minimum zoom is determined by image and viewport dimensions
    updateMinimumZoom() {
        this.minimumZoom = Math.min(getViewportHeight() / this.getOriginalHeight(), getViewportWidth() / this.getOriginalWidth())
    }
    getMinimumZoom() {
        return this.minimumZoom
    }
    getZoomForFitToScreen() {
        return Math.min(getViewportHeight() / this.getOriginalHeight(), getViewportWidth() / this.getOriginalWidth())
    }
    fitPageToScreen() {
        this.setZoom(this.getZoomForFitToScreen())
        this.update()
    }
    getRowThreshold() {
        return this.getWidth() * SETTING_COMIC_ROW_THRESHOLD.get()
    }
    getColumnThreshold() {
        return this.getHeight() * SETTING_COMIC_COLUMN_THRESHOLD.get()
    }
    isEndOfRow() {
        return (this.getWidth() <= getViewportWidth()) || approx(this.getLeft() + this.getWidth(), getViewportWidth(), this.getRowThreshold())
    }
    isBeginningOfRow() {
        return (this.getWidth() <= getViewportWidth()) || approx(this.getLeft(), 0, this.getRowThreshold())
    }
    isEndOfColumn() {
        return (this.getHeight() <= getViewportHeight()) || approx(this.getTop() + this.getHeight(), getViewportHeight(), this.getColumnThreshold())
    }
    isBeginningOfColumn() {
        return (this.getHeight() <= getViewportHeight()) || approx(this.getTop(), 0, this.getColumnThreshold())
    }
    update() {
        if (this.getZoom() < this.getMinimumZoom()) this.setZoom(this.getMinimumZoom())

        let newWidth = this.getOriginalWidth() * this.getZoom()
        let newHeight = this.getOriginalHeight() * this.getZoom()
        this.setWidth(newWidth)
        this.setHeight(newHeight)

        let minimumLeft = (newWidth < getViewportWidth()) ? (getViewportWidth() / 2) - (newWidth / 2) : Math.min(0, getViewportWidth() - newWidth)
        let maximumLeft = (newWidth < getViewportWidth()) ? (getViewportWidth() / 2) - (newWidth / 2) : Math.max(0, getViewportWidth() - newWidth)
        let minimumTop = (newHeight < getViewportHeight()) ? (getViewportHeight() / 2) - (newHeight / 2) : Math.min(0, getViewportHeight() - newHeight)
        let maximumTop = (newHeight < getViewportHeight()) ? (getViewportHeight() / 2) - (newHeight / 2) : Math.max(0, getViewportHeight() - newHeight)

        if (this.getLeft() < minimumLeft) this.setLeft(minimumLeft)
        if (this.getLeft() > maximumLeft) this.setLeft(maximumLeft)
        if (this.getTop() < minimumTop) this.setTop(minimumTop)
        if (this.getTop() > maximumTop) this.setTop(maximumTop)
    }
    zoom(zoom, centerX, centerY, withImageUpdate) {
        let sideLeft = centerX - this.getLeft()
        let ratioLeft = sideLeft / (this.getWidth() * this.getZoom())
        let newSideLeft = (this.getWidth() * zoom) * ratioLeft
        this.setLeft(centerX - newSideLeft)

        let sideTop = centerY - this.getTop()
        let ratioTop = sideTop / (this.getHeight() * this.getZoom())
        let newSideTop = (this.getHeight() * zoom) * ratioTop
        this.setTop(centerY - newSideTop)

        this.setZoom(zoom)
        SETTING_ZOOM_JUMP.put(zoom)
        if (withImageUpdate) this.update()
    }
    #getLastPosition(imageDimension, viewportDimension, imageValue, viewportJumpPercentage, threshold) {
        return viewportDimension - imageDimension
    }
    #getNextPosition(imageDimension, viewportDimension, imageValue, viewportJumpPercentage, threshold) {
        if (approx(imageValue, viewportDimension - imageDimension, threshold)) return 0
        var proposedNextPosition = (imageValue - viewportDimension * viewportJumpPercentage) | 0
        if (proposedNextPosition < viewportDimension - imageDimension) return viewportDimension - imageDimension
        return proposedNextPosition
    }
    #getPreviousPosition(imageDimension, viewportDimension, imageValue, viewportJumpPercentage, threshold) {
        if (approx(imageValue, 0, threshold)) return viewportDimension - imageDimension
        var proposedPreviousPosition = (imageValue + viewportDimension * viewportJumpPercentage) | 0
        if (proposedPreviousPosition > 0) return 0
        return proposedPreviousPosition
    }
    goToNextView() {
        if (this.isEndOfRow()) {
            if (this.isEndOfColumn()) {
                getComic().goToNextPage()
            } else {
                this.setLeft(this.#getNextPosition(this.getWidth(), getViewportWidth(), this.getLeft(), SETTING_COMIC_HORIZONTAL_JUMP.get(), this.getRowThreshold()))
                this.setTop(this.#getNextPosition(this.getHeight(), getViewportHeight(), this.getTop(), SETTING_COMIC_VERTICAL_JUMP.get(), this.getColumnThreshold()))
                this.update()
            }
        } else {
            this.setLeft(this.#getNextPosition(this.getWidth(), getViewportWidth(), this.getLeft(), SETTING_COMIC_HORIZONTAL_JUMP.get(), this.getRowThreshold()))
            this.update()
        }
    }
    goToPreviousView() {
        if (this.isBeginningOfRow()) {
            if (this.isBeginningOfColumn()) {
                let lastLeft = this.#getLastPosition(this.getWidth(), getViewportWidth(), this.getLeft(), SETTING_COMIC_HORIZONTAL_JUMP.get(), this.getRowThreshold())
                let lastTop = this.#getLastPosition(this.getHeight(), getViewportHeight(), this.getTop(), SETTING_COMIC_VERTICAL_JUMP.get(), this.getColumnThreshold())
                getComic().goToPreviousPage(lastLeft, lastTop)
            } else {
                this.setLeft(this.#getPreviousPosition(this.getWidth(), getViewportWidth(), this.getLeft(), SETTING_COMIC_HORIZONTAL_JUMP.get(), this.getRowThreshold()))
                this.setTop(this.#getPreviousPosition(this.getHeight(), getViewportHeight(), this.getTop(), SETTING_COMIC_VERTICAL_JUMP.get(), this.getColumnThreshold()))
                this.update()
            }
        } else {
            this.setLeft(this.#getPreviousPosition(this.getWidth(), getViewportWidth(), this.getLeft(), SETTING_COMIC_HORIZONTAL_JUMP.get(), this.getRowThreshold()))
            this.update()
        }
    }
    resetPan() {
        if (this.isEndOfRow() && this.isEndOfColumn()) {
            this.swipeNextPossible = true
        } else {
            this.swipeNextPossible = false
        }
        if (this.isBeginningOfRow() && this.isBeginningOfColumn()) {
            this.swipePreviousPossible = true
        } else {
            this.swipePreviousPossible = false
        }
    }
    /* returns true if pan should be disabled / when moving to a different page */
    pan(x, y, totalDeltaX, totalDeltaY, pinching) {
        if (SETTING_SWIPE_PAGE.get() && (this.swipeNextPossible || this.swipePreviousPossible) && (!pinching)) {
            let horizontalThreshold = getViewportWidth() * SETTING_SWIPE_LENGTH.get()
            let swipeParameters = computeSwipeParameters(totalDeltaX, totalDeltaY)
            let verticalMoveValid = swipeParameters.angle < SETTING_SWIPE_ANGLE_THRESHOLD.get()
            if (this.swipeNextPossible && x > 0 ) this.swipeNextPossible = false
            if (this.swipePreviousPossible && x < 0 ) this.swipePreviousPossible = false
            if (verticalMoveValid && totalDeltaX < -horizontalThreshold && this.swipeNextPossible) {
                this.swipeNextPossible = false
                this.swipePreviousPossible = false
                this.goToNextView()
                return true
            } else if (verticalMoveValid && totalDeltaX > horizontalThreshold && this.swipePreviousPossible) {
                this.swipeNextPossible = false
                this.swipePreviousPossible = false
                this.goToPreviousView()
                return true
            } else {
                this.addLeft(x)
                this.addTop(y)
                this.update()
                return false
            }
        } else {
            this.addLeft(x)
            this.addTop(y)
            this.update()
            return false
        }
    }
    zoomJump(x, y) {
        if (SETTING_FIT_COMIC_TO_SCREEN.get()) {
            SETTING_FIT_COMIC_TO_SCREEN.put(false)
            this.zoom(SETTING_ZOOM_JUMP.get(), x, y, true)
        } else {
            SETTING_FIT_COMIC_TO_SCREEN.put(true)
            this.fitPageToScreen()
        }
    }
    reset() {
        this.setWidth(this.getOriginalWidth())
        this.setHeight(this.getOriginalHeight())
        this.setLeft(0)
        this.setTop(0)
        this.updateMinimumZoom()
    }
}

function getImage() {
    if (document.image) {
        return document.image
    } else {
        document.image = new Image(document.getElementsByTagName("img")[0])
        return document.image
    }
}

/////////////////////////////////////////////////////////////////////////////////////////////////////////////// IMAGE ↑

function approx(val1, val2, threshold = 1) {
    return Math.abs(val1 - val2) < threshold
}

function handleResize() {
    fixControlSizes()
    getImage().updateMinimumZoom()
    getImage().update()
}

function mouseGestureScroll(scrollCenterX, scrollCenterY, scrollValue) {
    var zoomDelta = 1 + scrollValue * SETTING_COMIC_SCROLL_SPEED.get() * (SETTING_COMIC_INVERT_SCROLL.get() ? 1 : -1)
    var newZoom = getImage().getZoom() * zoomDelta
    getImage().zoom(newZoom, scrollCenterX, scrollCenterY, true)
}

function downloadComicToDevice() {
    if('serviceWorker' in navigator) {
        var bookId = getMeta("bookId")
        var pages = num(getMeta("size"))
        navigator.serviceWorker.controller.postMessage({type: 'storeBook', bookId: bookId, maxPositions: pages, kind: 'comic'})
    }
}

function getDownloadPageButton() {
    let button = document.createElement('a')
    button.id = 'downloadPageButton'
    button.innerHTML = 'download'
    button.style.gridColumnStart = '1'
    button.style.gridColumnEnd = '3'

    let controller = document.createElement('div')
    controller.classList.add('setting')
    controller.appendChild(button)
    return controller
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////// GESTURES ↓

class Gestures {
    constructor(element, resetSwipeFunction, getZoomFunction, setZoomFunction, panFunction, singleClickFunction, doubleClickFunction, mouseScrollFunction) {
        this.element = element
        this.clickCache = []
        this.DOUBLE_CLICK_THRESHOLD = 400
        this.CLICK_DISTANCE_THRESHOLD = 5
        this.resetSwipe = resetSwipeFunction
        this.getZoom = getZoomFunction
        this.setZoom = setZoomFunction
        this.pan = panFunction
        this.singleClick = singleClickFunction
        this.doubleClick = doubleClickFunction
        this.mouseScroll = mouseScrollFunction

        if (this.isTouchEnabled()) {
            this.element.addEventListener("touchstart", this.getTouchStartHandler(), false)
            this.element.addEventListener("touchmove", this.getTouchMoveHandler(), false)
            this.element.addEventListener("touchend", this.getTouchEndHandler(), false)
        } else {
            this.element.addEventListener("pointerdown", this.getTouchStartHandler(), false)
            this.element.addEventListener("pointermove", this.getTouchMoveHandler(), false)
            this.element.addEventListener("pointerup", this.getTouchEndHandler(), false)
            this.element.addEventListener("wheel", this.getMouseWheelScrollHandler(), false)
            this.element.addEventListener("contextmenu", this.getContextMenuHandler(), false)
        }
    }
    getContextMenuHandler() {
        let self = this
        function contextMenuHandler(event) {
            self.disableEventNormalBehavior(event)
            return false
        }
        return contextMenuHandler
    }
    getMouseWheelScrollHandler() {
        let self = this
        function mouseWheelScrollHandler(event) {
            let scrollCenterX = event.clientX
            let scrollCenterY = event.clientY
            let scrollValue = event.deltaY

            if (self.mouseScroll) self.mouseScroll(scrollCenterX, scrollCenterY, scrollValue)
        }
        return mouseWheelScrollHandler
    }
    isTouchEnabled() {
        return window.matchMedia("(pointer: coarse)").matches
    }
    disableEventNormalBehavior(event) {
        event.preventDefault()
        event.stopPropagation()
    }
    pushClick(timestamp) {
        this.clickCache.push(timestamp)
        while (this.clickCache.length > 2) {
            this.clickCache.shift()
        }
    }
    getTouchStartHandler() {
        let self = this
        function touchStartHandler(event) {
            self.disableEventNormalBehavior(event)
            self.pushClick(Date.now())
            self.panEnabled = true

            if (self.getTouchesCount(event) >= 1) {
                self.originalCenter = self.computeCenter(event)
                self.previousCenter = self.originalCenter
                if (self.resetSwipe) self.resetSwipe()
            }
            if (self.getTouchesCount(event) == 2) {
                self.originalPinchSize = self.computeDistance(event)
                if (self.getZoom) self.originalZoom = self.getZoom()
            }
            return false
        }
        return touchStartHandler
    }
    getTouchesCount(event) {
        if (event.type.startsWith("touch")) {
            return event.targetTouches.length
        } else {
            if (event.buttons > 0) {
                return 1
            } else {
                return 0
            }
        }
    }
    computeDistance(pinchTouchEvent) {
        if (pinchTouchEvent.targetTouches.length == 2) {
            return this.computePointsDistance({
                x: pinchTouchEvent.targetTouches[0].clientX,
                y: pinchTouchEvent.targetTouches[0].clientY
            }, {
                x: pinchTouchEvent.targetTouches[1].clientX,
                y: pinchTouchEvent.targetTouches[1].clientY
            })
        } else {
            return null
        }
    }
    computePointsDistance(p1, p2) {
        return Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2))
    }
    computeCenter(event) {
        if (event.type.startsWith("touch")) {
            let centerX = 0
            let centerY = 0
            for (let i = 0; i < event.targetTouches.length; i++) {
                centerX = centerX + event.targetTouches[i].clientX
                centerY = centerY + event.targetTouches[i].clientY
            }
            centerX = centerX / event.targetTouches.length
            centerY = centerY / event.targetTouches.length
            return {
                x: centerX,
                y: centerY
            }
        } else if (event.type.startsWith("pointer")) {
            return {
                x: event.clientX,
                y: event.clientY
            }
        } else {
            return null
        }
    }
    getTouchMoveHandler() {
        let self = this
        function touchMoveHandler(ev) {
            self.disableEventNormalBehavior(ev)
            if (self.getTouchesCount(ev) == 2) {
                self.pinching = true
                let pinchSize = self.computeDistance(ev)
                let currentZoom = pinchSize / self.originalPinchSize
                let newZoom = self.originalZoom * currentZoom
                if (self.setZoom) self.setZoom(newZoom, self.originalCenter.x, self.originalCenter.y)
            } else if (self.getTouchesCount(ev) == 1) {
                self.pinching = false
            }
            if (self.panEnabled && self.getTouchesCount(ev) > 0 && self.getTouchesCount(ev) <= 2) {
                let currentCenter = self.computeCenter(ev)
                let deltaX = currentCenter.x - self.previousCenter.x
                let deltaY = currentCenter.y - self.previousCenter.y
                let totalDeltaX = currentCenter.x - self.originalCenter.x
                let totalDeltaY = currentCenter.y - self.originalCenter.y
                self.previousCenter = currentCenter
                if (self.pan) {
                    let stopPan = self.pan(deltaX * SETTING_COMIC_PAN_SPEED.get(), deltaY * SETTING_COMIC_PAN_SPEED.get(), totalDeltaX, totalDeltaY, self.pinching)
                    if (stopPan) self.panEnabled = false
                }
            }
            return false
        }
        return touchMoveHandler
    }
    isDoubleClick() {
        if (this.clickCache.length >= 2) {
            let timeDifference = this.clickCache[this.clickCache.length - 1] - this.clickCache[this.clickCache.length - 2]
            return timeDifference < this.DOUBLE_CLICK_THRESHOLD
        } else {
            return false
        }
    }
    isLastClickRelevant() {
        if (this.clickCache.length >= 1) {
            let clickNotTooOld = Date.now() - this.clickCache[this.clickCache.length - 1] < this.DOUBLE_CLICK_THRESHOLD
            let panNotTooLarge = this.computePointsDistance(this.originalCenter, this.previousCenter) < this.CLICK_DISTANCE_THRESHOLD
            return clickNotTooOld && panNotTooLarge && this.panEnabled
        } else {
            return false
        }
    }
    getTouchEndHandler() {
        let self = this
        function touchEndHandler(ev) {
            self.disableEventNormalBehavior(ev)

            if (self.getTouchesCount(ev) >= 1) {
                self.originalCenter = self.computeCenter(ev)
                self.previousCenter = self.originalCenter
            }
            if (self.isLastClickRelevant()) {
                if (self.isDoubleClick()) {
                    if (self.doubleClick) self.doubleClick(self.originalCenter.x, self.originalCenter.y)
                } else {
                    if (self.singleClick) self.singleClick(self.originalCenter.x, self.originalCenter.y)
                }
            }
            return false
        }
        return touchEndHandler
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////// GESTURES ↑

function initSettings() {
    let settingsWrapper = document.getElementById('ch_settings')
    settingsWrapper.appendChild(getDownloadPageButton())
    settingsWrapper.appendChild(SETTING_COMIC_HORIZONTAL_JUMP.controller)
    settingsWrapper.appendChild(SETTING_COMIC_VERTICAL_JUMP.controller)
    settingsWrapper.appendChild(getRemoveProgressButton())
    settingsWrapper.appendChild(getMarkAsReadButton())
}

window.onload = function() {
    checkAndUpdateTheme()

    fixControlSizes()
    enableKeyboardGestures({
        "upAction": () => pan(0, getViewportHeight() / 2),
        "downAction": () => pan(0, - (getViewportHeight() / 2)),
        "leftAction": () => getImage().goToPreviousView(),
        "rightAction": () => getImage().goToNextView(),
        "escapeAction": () => toggleTools(true)
    })

    let getZoomFunction = function() {
        return getImage().getZoom()
    }
    let zoomFunction = function(val, cx, cy, withUpdate) {
        getImage().zoom(val, cx, cy, withUpdate)
    }
    let panFunction = function(x, y, totalDeltaX, totalDeltaY, pinching) {
        return getImage().pan(x, y, totalDeltaX, totalDeltaY, pinching)
    }
    new Gestures(document.getElementById("ch_canv"), () => getImage().resetPan(), getZoomFunction, zoomFunction, panFunction, null, (x, y) => getImage().zoomJump(x, y), mouseGestureScroll)
    new Gestures(document.getElementById("ch_prev"), () => getImage().resetPan(), getZoomFunction, zoomFunction, panFunction, () => getImage().goToPreviousView(), null, mouseGestureScroll)
    new Gestures(document.getElementById("ch_next"), () => getImage().resetPan(), getZoomFunction, zoomFunction, panFunction, () => getImage().goToNextView(), null, mouseGestureScroll)

    document.getElementById("ch_tools_left").addEventListener("click", (event) => toggleTools(true))
    document.getElementById("ch_tools_right").addEventListener("click", (event) => toggleTools(false))
    document.getElementById("ch_tools_container").addEventListener("click", (event) => toggleTools())
    document.getElementById("ch_tools").addEventListener("click", event => event.stopPropagation())

    addPositionInputTriggerListener((page) => getComic().jumpToPage(page))

    initSettings()
    initFullscreenButton()
    initBookCollectionLinks()

    document.lastPageChange = new Date()
    loadProgress(currentPosition => {
        var startPage = currentPosition + 1
        getComic().displayPage(startPage, () => {
            SETTING_FIT_COMIC_TO_SCREEN.put(true)
            getImage().fitPageToScreen()
        })
    })

    window.addEventListener("focus", checkAndUpdateTheme, false)

    downloadComicToDevice()
}