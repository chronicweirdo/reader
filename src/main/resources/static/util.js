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
    // scroll tools to max position on start
    tools.scrollTop = tools.scrollHeight
    if (prepareToolsView != null) {
        prepareToolsView()
    }
}