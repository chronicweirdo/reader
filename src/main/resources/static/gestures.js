var gestures = {
    mouseDownX: null,
    mouseDownY: null,
    mousePressed: false,
    clickCount: 0
}

function disableEventHandlers(el) {
    var events = ['onclick', 'onmousedown', 'onmousemove', 'onmouseout', 'onmouseover', 'onmouseup', 'ondblclick', 'onfocus', 'onblur']

    events.forEach(function (event) {
        el[event] = function () {
            return false;
        }
    })
}

function enableTouchGestures(element, pinchStartAction, pinchAction, pinchEndAction, panStartAction, panAction, panEndAction) {
    disableEventHandlers(element)
    var hammertime = new Hammer(element, {domEvents: true})
    hammertime.get('pan').set({ direction: Hammer.DIRECTION_ALL, threshold: 0 });
    hammertime.get('pinch').set({ enable: true });

    var panStartX = null
    var panStartY = null

    var panPreviousDeltaX = 0
    var panPreviousDeltaY = 0

    var pinching = false
    var pinchCenterX = null
    var pinchCenterY = null

    hammertime.on('pinchstart', function(ev) {
        ev.preventDefault();
        pinching = true
        pinchCenterX = ev.center.x
        pinchCenterY = ev.center.y
        if (pinchStartAction) pinchStartAction(ev.center.x, ev.center.y)
    })
    hammertime.on('pinch', function(ev) {
        ev.preventDefault()
        var currentZoom = ev.scale
        if (pinchAction) pinchAction(currentZoom, pinchCenterX, pinchCenterY)
        var currentDeltaX = ev.deltaX - panPreviousDeltaX
        var currentDeltaY = ev.deltaY - panPreviousDeltaY
        if (panAction) panAction(currentDeltaX, currentDeltaY)
        panPreviousDeltaX = ev.deltaX
        panPreviousDeltaY = ev.deltaY
    });
    hammertime.on('pinchend', function(ev) {
        panPreviousDeltaX = 0
        panPreviousDeltaY = 0
        if (pinchEndAction) pinchEndAction(ev.scale, pinchCenterX, pinchCenterY)
    })

    hammertime.on('panstart', function(ev) {
        if (! pinching) {
            panStartX = ev.center.x
            panStartY = ev.center.y
            if (panStartAction) panStartAction(panStartX, panStartY)
        }
    })
    hammertime.on('pan', function(ev) {
        if (! pinching) {
            var currentDeltaX = ev.deltaX - panPreviousDeltaX
            var currentDeltaY = ev.deltaY - panPreviousDeltaY
            if (panAction) panAction(currentDeltaX, currentDeltaY, ev.center.x, ev.center.y)
            panPreviousDeltaX = ev.deltaX
            panPreviousDeltaY = ev.deltaY
        }
    })
    hammertime.on('panend', function(ev) {
        if (! pinching) {
            panPreviousDeltaX = 0
            panPreviousDeltaY = 0

            var dx = panStartX - ev.center.x
            var dy = panStartY - ev.center.y
            if (panEndAction) panEndAction(dx, dy)
        }
        // a pinch always ends with a pan
        pinching = false
    })
}

function mouseWheelScroll(event, scrollAction, scrollEndAction) {
    var scrollCenterX = event.clientX
    var scrollCenterY = event.clientY
    var scrollValue = event.deltaY

    if (scrollAction) scrollAction(scrollCenterX, scrollCenterY, scrollValue)

    var timestamp = + new Date()
    gestures.scrollTimestamp = timestamp
    delayed(function() {
        if (gestures.scrollTimestamp && gestures.scrollTimestamp != null && gestures.scrollTimestamp == timestamp) {
            if (scrollEndAction) scrollEndAction(scrollCenterX, scrollCenterY, scrollValue)
        }
    })
}

function mouseDown(event, callback) {
    if (event.button == 0) {
        gestures.mouseDownX = event.clientX
        gestures.mouseDownY = event.clientY
        gestures.mousePressed = true

        if (callback) callback(event.clientX, event.clientY)
    }
}

function mouseUp(event, clickAction, doubleClickAction) {
    if (event.button == 0) {
        gestures.mousePressed = false
        if (gestures.mouseDownX == event.clientX && gestures.mouseDownY == event.clientY && (clickAction != null || doubleClickAction != null)) {
            // the mouse did not move, it's a click
            click(event, clickAction, doubleClickAction)
        }
    }
}

function click(event, clickAction, doubleClickAction) {
    event.preventDefault()
    gestures.clickCount += 1
    delayed(function() {
        if (gestures.clickCount == 2) {
            gestures.clickCount = 0
            if (doubleClickAction != null) doubleClickAction(event.clientX, event.clientY)
        } else if (gestures.clickCount == 1) {
            gestures.clickCount = 0
            if (clickAction != null) clickAction(event.clientX, event.clientY)
        }
        if (gestures.clickCount > 0) gestures.clickCount -= 1
    })
}

function delayed(callback) {
    window.setTimeout(callback, 250)
}

function mouseMove(event, callback) {
    if (callback) callback(gestures.mousePressed, event.movementX, event.movementY)
}

function enableGesturesOnElement(element, actions) {
    enableTouchGestures(
        element,
        actions.pinchStartAction,
        actions.pinchAction,
        actions.pinchEndAction,
        actions.panStartAction,
        actions.panAction,
        actions.panEndAction
    )
    element.addEventListener("wheel", (event) => mouseWheelScroll(event, actions.scrollAction, actions.scrollEndAction))
    element.addEventListener("mousedown", mouseDown)
    element.addEventListener("mouseup", (event) => mouseUp(event, actions.clickAction, actions.doubleClickAction))
    element.addEventListener("mouseout", mouseUp)
    element.addEventListener("mousemove", (event) => mouseMove(event, actions.mouseMoveAction))
}

function enableKeyboardGestures(actions) {
    document.onkeydown = function(e) {
        if (e.keyCode == '38' || e.keyCode == '87') {
            // up arrow or w
            if (actions.upAction) actions.upAction()
        }
        else if (e.keyCode == '40' || e.keyCode == '83') {
            // down arrow or s
            if (actions.downAction) actions.downAction()
        }
        else if (e.keyCode == '37' || e.keyCode == '65') {
            // left arrow or a
            if (actions.leftAction) actions.leftAction()
        }
        else if (e.keyCode == '39' || e.keyCode == '68') {
            // right arrow or d
            if (actions.rightAction) actions.rightAction()
        }
        else if (e.keyCode == '27') {
            // escape
            if (actions.escapeAction) actions.escapeAction()
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////////////// GESTURES ↓

class Gestures {
    constructor(element, resetSwipeFunction, getZoomFunction, setZoomFunction, panFunction, singleClickFunction, doubleClickFunction, mouseScrollFunction) {
        this.element = element
        this.clickCache = []
        this.DOUBLE_CLICK_THRESHOLD = 200
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
        return ('ontouchstart' in window)/* ||
            (navigator.maxTouchPoints > 0) ||
            (navigator.msMaxTouchPoints > 0)*/
    }
    disableEventNormalBehavior(event) {
        event.preventDefault()
        event.stopPropagation()
        //event.stopImmediatePropagation()
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
            if (self.getTouchesCount(ev) > 0 && self.getTouchesCount(ev) <= 2) {
                let currentCenter = self.computeCenter(ev)
                let deltaX = currentCenter.x - self.previousCenter.x
                let deltaY = currentCenter.y - self.previousCenter.y
                let totalDeltaX = currentCenter.x - self.originalCenter.x
                let totalDeltaY = currentCenter.y - self.originalCenter.y
                self.previousCenter = currentCenter
                if (self.pan) self.pan(deltaX * getPanSpeed(), deltaY * getPanSpeed(), totalDeltaX, totalDeltaY, self.pinching)
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
            let panNotTooLarge = this.computePointsDistance(this.originalCenter, this.previousCenter) < 1
            return clickNotTooOld && panNotTooLarge
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