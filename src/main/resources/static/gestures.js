function disableEventHandlers(el) {
    var events = ['onclick', 'onmousedown', 'onmousemove', 'onmouseout', 'onmouseover', 'onmouseup', 'ondblclick', 'onfocus', 'onblur']

    events.forEach(function (event) {
        el[event] = function () {
            return false;
        }
    })
}

function enableTouchGestures(element, pinchStartAction, pinchAction, panAction) {
    disableEventHandlers(element)
    var hammertime = new Hammer(element, {domEvents: true})
    hammertime.get('pan').set({ direction: Hammer.DIRECTION_ALL, threshold: 0 });
    hammertime.get('pinch').set({ enable: true });

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
    })

    hammertime.on('pan', function(ev) {
        if (! pinching) {
            var currentDeltaX = ev.deltaX - panPreviousDeltaX
            var currentDeltaY = ev.deltaY - panPreviousDeltaY
            panAction(currentDeltaX, currentDeltaY)
            panPreviousDeltaX = ev.deltaX
            panPreviousDeltaY = ev.deltaY
        }
    })
    hammertime.on('panend', function(ev) {
        if (! pinching) {
            panPreviousDeltaX = 0
            panPreviousDeltaY = 0
        }
        // a pinch always ends with a pan
        pinching = false
    })
}

function mouseWheelScroll(event, callback) {
    console.log("mouse scroll")
    var scrollCenterX = event.clientX
    var scrollCenterY = event.clientY
    var scrollValue = event.deltaY

    if (callback) callback(scrollCenterX, scrollCenterY, scrollValue)
}

var gestures = {
    mouseDownX: null,
    mouseDownY: null,
    mousePressed: false
}

function mouseDown(event, callback) {
    event.preventDefault()
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
    document.clickTimestamp = timestamp
    window.setTimeout(function() {
        if (document.clickTimestamp && document.clickTimestamp != null) {
            if (document.clickTimestamp > timestamp) {
                //there has been aother click in the meantime, double click
                if (doubleClickAction != null) doubleClickAction(event.clientX, event.clientY)
            } else {
                // simple click
                if (clickAction != null) clickAction(event.clientX, event.clientY)
            }
            document.clickTimestamp = null
        }
    }, 250)
}

function mouseMove(event, callback) {
    if (callback) callback(gestures.mousePressed, event.movementX, event.movementY)
}

// supported actions:
// clickAction(mouseX, mouseY)
// doubleClickAction(mouseX, mouseY)
// mouseMoveAction(mouseButtonPressed, deltaX, deltaY)
// scrollAction(scrollCenterX, scrollCenterY, scrollValue)
// pinchStartAction(pinchCenterX, pinchCenterY)
// pinchAction(currentZoom, pinchCenterX, pinchCenterY)
// panAction(deltaX, deltaY)

function enableGesturesOnElement(
    element,
    clickAction, 
    doubleClickAction,
    mouseMoveAction,
    scrollAction,
    pinchStartAction,
    pinchAction,
    panAction
    ) {
    enableTouchGestures(element, pinchStartAction, pinchAction, panAction)
    element.addEventListener("wheel", (event) => mouseWheelScroll(event, scrollAction))
    element.addEventListener("mousedown", mouseDown)
    element.addEventListener("mouseup", (event) => mouseUp(event, clickAction, doubleClickAction))
    element.addEventListener("mouseout", mouseUp)
    element.addEventListener("mousemove", (event) => mouseMove(event, mouseMoveAction))
}