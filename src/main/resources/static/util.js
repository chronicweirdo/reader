function onMobile() {
    if (navigator.userAgent.match(/Android/i)
        || navigator.userAgent.match(/webOS/i)
        || navigator.userAgent.match(/iPhone/i)
        || navigator.userAgent.match(/iPad/i)
        || navigator.userAgent.match(/iPod/i)
        || navigator.userAgent.match(/BlackBerry/i)
        || navigator.userAgent.match(/Windows Phone/i)) {
        return true
    } else {
        return false
    }
}

function num(s, def) {
    var patt = /[\-]?[0-9\.]+/
    var match = patt.exec(s)
    if (match != null && match.length > 0) {
        var n = match[0]
        if (n.indexOf('.') > -1) {
            return parseFloat(n)
        } else {
            return parseInt(n)
        }
    }
    return def
}

function getMeta(metaName) {
    const metas = document.getElementsByTagName('meta');

    for (let i = 0; i < metas.length; i++) {
        if (metas[i].getAttribute('name') === metaName) {
        return metas[i].getAttribute('content');
        }
    }

    return '';
}

function getViewportWidth() {
    return Math.max(document.documentElement.clientWidth, window.innerWidth || 0)
}

function getViewportHeight() {
    return Math.max(document.documentElement.clientHeight, window.innerHeight || 0)
}

function showSpinner() {
    var spinner = document.getElementById("ch_spinner")
    spinner.style.display = "block"
}

function hideSpinner() {
    var spinner = document.getElementById("ch_spinner")
    spinner.style.display = "none"
}

function hideTools() {
    var tools = document.getElementById("ch_tools_container")
    tools.style.display = "none"
}

function toggleTools(left, prepareToolsView) {
    var tools = document.getElementById("ch_tools")
    if (left) {
        tools.className = "left"
    } else {
        tools.className = "right"
    }
    var toolsContainer = document.getElementById("ch_tools_container")
    if (toolsContainer.style.display == "block") {
        toolsContainer.style.display = "none"
    } else {
        toolsContainer.style.display = "block"
    }
    if (prepareToolsView != null) {
        prepareToolsView()
    }
    // scroll tools to max position on start
    tools.scrollTop = tools.scrollHeight
}

function fixComponentHeights() {
    var height = getViewportHeight()
    var width = getViewportWidth()
    var contentTop = height * .05
    var contentHeight = height * .9
    if (document.getElementById("ch_content") != null) {
        document.getElementById("ch_content").style.top = contentTop + "px"
        document.getElementById("ch_content").style.height = contentHeight + "px"
        document.getElementById("ch_shadow_content").style.top = contentTop + "px"
        document.getElementById("ch_shadow_content").style.height = contentHeight + "px"
    }

    var pageControlsTop = 0
    var pageControlsHeight = height * .9
    document.getElementById("ch_prev").style.top = pageControlsTop + "px"
    document.getElementById("ch_prev").style.height = pageControlsHeight + "px"
    document.getElementById("ch_next").style.top = pageControlsTop + "px"
    document.getElementById("ch_next").style.height = pageControlsHeight + "px"

    var toolsControlsHeight = height - pageControlsHeight
    document.getElementById("ch_tools_left").style.height = toolsControlsHeight + "px"
    document.getElementById("ch_tools_right").style.height = toolsControlsHeight + "px"

    var spinnerDimension = Math.min(height * .2, width * .2)
    document.getElementById("ch_spinner_svg").style.width = spinnerDimension + "px"
    document.getElementById("ch_spinner_svg").style.height = spinnerDimension + "px"
    document.getElementById("ch_spinner").style.paddingTop = ((height - spinnerDimension) / 2) + "px"
}

function goHome() {
    window.location = "/"
}

function saveProgress(bookId, position) {
    var xhttp = new XMLHttpRequest()
    xhttp.open("PUT", "markProgress?id=" + bookId + "&position=" + (position))
    xhttp.send()
}

function removeProgress() {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                window.location = "/"
            }
        }
    }
    xhttp.open("DELETE", "removeProgress?id=" + getMeta("bookId"))
    xhttp.send()
}

function updatePositionInput(position) {
    var el = document.getElementById("positionInput")
    if (el.tagName == "INPUT") {
        el.value = position
    } else {
        el.innerHTML = position
    }
}

function getPositionInput() {
    return num(document.getElementById("positionInput").value)
}

function addPositionInputTriggerListener(loadPositionFunction) {
    var positionInput = document.getElementById("positionInput")
    var max = positionInput.max
    var min = positionInput.min

    var loadPosition = function() {
        var desiredPosition = getPositionInput()
        if (desiredPosition < min) loadPositionFunction(min)
        else if (desiredPosition > max) loadPositionFunction(max)
        else loadPositionFunction(desiredPosition)
    }

    positionInput.addEventListener('keyup', function (e) {
        e.preventDefault()
        if (document.pageChangeTimeout && document.pageChangeTimeout != null) {
            window.clearTimeout(document.pageChangeTimeout)
            document.pageChangeTimeout = null
        }
        if (e.keyCode === 13) {
            // if enter, search
            loadPosition()
        } else {
            // if other key, wait to see if finished typing
            document.pageChangeTimeout = window.setTimeout(loadPosition, 1000)
        }
    })
    positionInput.addEventListener('click', function (e) {
        e.stopPropagation()
    })
    positionInput.addEventListener('mouseup', function (e) {
        e.preventDefault()
        loadPosition()
    })
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