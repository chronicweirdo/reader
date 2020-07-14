var gestures = {
    mouseDownX: null,
    mouseDownY: null,
    mousePressed: false
}

function disableEventHandlers(el) {
    var events = ['onclick', 'onmousedown', 'onmousemove', 'onmouseout', 'onmouseover', 'onmouseup', 'ondblclick', 'onfocus', 'onblur']

    events.forEach(function (event) {
        el[event] = function () {
            return false;
        }
    })
}

function enableTouchGestures(element, pinchStartAction, pinchAction, pinchEndAction, panAction, panEndAction) {
    disableEventHandlers(element)
    var hammertime = new Hammer(element, {domEvents: true})
    hammertime.get('pan').set({ direction: Hammer.DIRECTION_ALL, threshold: 0 });
    hammertime.get('pinch').set({ enable: true });

    var panStartX = null
    var panStartY = null

    var panPreviousDeltaX = 0
    var panPreviousDeltaY = 0

    var pinching = false
    //var originalZoom = null
    var pinchCenterX = null
    var pinchCenterY = null

    hammertime.on('pinchstart', function(ev) {
        ev.preventDefault();
        pinching = true
        //originalZoom = 1
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
        }
    })
    hammertime.on('pan', function(ev) {
        if (! pinching) {
            var currentDeltaX = ev.deltaX - panPreviousDeltaX
            var currentDeltaY = ev.deltaY - panPreviousDeltaY
            if (panAction) panAction(currentDeltaX, currentDeltaY)
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
    //event.preventDefault()
    gestures.mouseDownX = event.clientX
    gestures.mouseDownY = event.clientY
    gestures.mousePressed = true

    if (callback) callback(event.clientX, event.clientY)
}

function mouseUp(event, clickAction, doubleClickAction) {
    gestures.mousePressed = false
    if (gestures.mouseDownX == event.clientX && gestures.mouseDownY == event.clientY) {
        // the mouse did not move, it's a click
        click(event, clickAction, doubleClickAction)
    }
}

function click(event, clickAction, doubleClickAction) {
    event.preventDefault()
    var timestamp = + new Date()
    gestures.clickTimestamp = timestamp
    delayed(function() {
        if (gestures.clickTimestamp && gestures.clickTimestamp != null) {
            if (gestures.clickTimestamp > timestamp) {
                //there has been aother click in the meantime, double click
                if (doubleClickAction != null) doubleClickAction(event.clientX, event.clientY)
            } else {
                // simple click
                if (clickAction != null) clickAction(event.clientX, event.clientY)
            }
            gestures.clickTimestamp = null
        }
    })
    // fixing click on links
    var href = event.target.getAttribute("href")
    if (href) {
        window.location = href
    }
}

function delayed(callback) {
    window.setTimeout(callback, 250)
}

function mouseMove(event, callback) {
    if (callback) callback(gestures.mousePressed, event.movementX, event.movementY)
}

// supported actions:
// clickAction(mouseX, mouseY)
// doubleClickAction(mouseX, mouseY)
// mouseMoveAction(mouseButtonPressed, deltaX, deltaY)
// scrollAction(scrollCenterX, scrollCenterY, scrollValue)
// scrollEndAction(scrollCenterX, scrollCenterY, scrollValue)
// pinchStartAction(pinchCenterX, pinchCenterY)
// pinchAction(currentZoom, pinchCenterX, pinchCenterY)
// pinchEndAction(currentZoom, pinchCenterX, pinchCenterY)
// panAction(deltaX, deltaY)
// panEndAction(deltaX, deltaY)

function enableGesturesOnElement(element, actions) {
    enableTouchGestures(
        element,
        actions.pinchStartAction,
        actions.pinchAction,
        actions.pinchEndAction,
        actions.panAction,
        actions.panEndAction
    )
    element.addEventListener("wheel", (event) => mouseWheelScroll(event, actions.scrollAction, actions.scrollEndAction))
    element.addEventListener("mousedown", mouseDown)
    element.addEventListener("mouseup", (event) => mouseUp(event, actions.clickAction, actions.doubleClickAction))
    element.addEventListener("mouseout", mouseUp)
    element.addEventListener("mousemove", (event) => mouseMove(event, actions.mouseMoveAction))
}

// supported actions:
// upAction()
// downAction()
// leftAction()
// rightAction()

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