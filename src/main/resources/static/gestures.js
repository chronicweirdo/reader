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
    }
}