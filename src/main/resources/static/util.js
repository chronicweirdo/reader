var REFRESH_PAGE_TIME_DIFFERENCE = 15*60*1000

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

function onIOS() {
    var isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) && !window.MSStream
    return isIOS
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

    return undefined;
}

function setMeta(metaName, value) {
    document.querySelector('meta[name="' + metaName + '"]').setAttribute("content", value);
}

function setStatusBarColor(color) {
    setMeta('theme-color', color)
    if (onIOS()) {
        let appropriateColor = getAppropriateStatusBarColor(color)
        document.documentElement.style.setProperty('--status-bar-color', appropriateColor)
    } else {
        document.documentElement.style.setProperty('--status-bar-color', SETTING_BACKGROUND_COLOR.get())
    }
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

function fixControlSizes() {
    let bookEdgeHorizontal = SETTING_BOOK_EDGE_HORIZONTAL.get()
    let bookEdgeVertical = SETTING_BOOK_EDGE_VERTICAL.get()
    let bookToolsHeight = SETTING_BOOK_TOOLS_HEIGHT.get()

    var height = getViewportHeight()
    var width = getViewportWidth()
    var contentTop = height * bookEdgeVertical
    var contentHeight = height - (2 * contentTop)
    var toolWidth = bookEdgeHorizontal * width
    var chContentWidth = width - (2 * toolWidth)
    if (document.getElementById("content") != null) {
        document.getElementById("content").style.top = 0 + "px"
        document.getElementById("content").style.height = height + "px"
        document.getElementById("content").style.width = width + "px"
    }
    if (document.getElementById("ch_content") != null) {
        document.getElementById("ch_content").style.top = contentTop + "px"
        document.getElementById("ch_content").style.height = contentHeight + "px"
        document.getElementById("ch_content").style.width = chContentWidth + "px"
        document.getElementById("ch_content").style.left = toolWidth + "px"
        document.getElementById("ch_shadow_content").style.top = contentTop + "px"
        document.getElementById("ch_shadow_content").style.height = contentHeight + "px"
        document.getElementById("ch_shadow_content").style.width = chContentWidth + "px"
        document.getElementById("ch_shadow_content").style.left = toolWidth + "px"
    }

    var pageControlsTop = 0
    var pageControlsHeight = height - height * bookToolsHeight
    document.getElementById("ch_prev").style.top = pageControlsTop + "px"
    document.getElementById("ch_prev").style.height = pageControlsHeight + "px"
    document.getElementById("ch_prev").style.width = toolWidth + "px"
    document.getElementById("ch_next").style.top = pageControlsTop + "px"
    document.getElementById("ch_next").style.height = pageControlsHeight + "px"
    document.getElementById("ch_next").style.width = toolWidth + "px"

    var toolsControlsHeight = height * bookToolsHeight
    document.getElementById("ch_tools_left").style.height = toolsControlsHeight + "px"
    document.getElementById("ch_tools_left").style.width = toolWidth + "px"
    document.getElementById("ch_tools_right").style.height = toolsControlsHeight + "px"
    document.getElementById("ch_tools_right").style.width = toolWidth + "px"

    document.getElementById("ch_tools").style.width = chContentWidth + "px"
    document.getElementById("ch_tools").style.marginLeft = toolWidth + "px"
    document.getElementById("ch_tools").style.marginRight = toolWidth + "px"

    var spinnerDimension = Math.min(height * .4, width * .4)
    document.getElementById("ch_spinner_svg").style.width = spinnerDimension + "px"
    document.getElementById("ch_spinner_svg").style.height = spinnerDimension + "px"
    document.getElementById("ch_spinner").style.paddingTop = ((height - spinnerDimension) / 2) + "px"
}

function goHome() {
    window.location = "/"
}

function saveProgress(bookId, position, callback) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status > 400 && this.status < 500) {
                window.location.href = "/logout"
            } else {
                // mark on device
                let onDeviceHeader = this.getResponseHeader("ondevice")
                if (onDeviceHeader == "true") {
                    let coverElement = document.getElementById("ch_cover")
                    coverElement.classList.add("ondevice")
                }
                if (callback) callback()
            }
        }
    }
    xhttp.open("PUT", "markProgress?id=" + bookId + "&position=" + (position))
    xhttp.send()
}

function loadProgress(callback) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == this.HEADERS_RECEIVED) {
            var contentType = this.getResponseHeader("Content-Type");
            if (contentType != "application/json") {
                this.abort();
                window.location.href = "/logout"
            }
        } else if (this.readyState == this.DONE) {
            if (this.status == 200) {
                var currentPosition = parseInt(this.responseText)
                if (callback != null) {
                    callback(currentPosition)
                } else {
                    callback(undefined)
                }
            } else if (this.status > 400 && this.status < 500) {
                window.location.href = "/logout"
            } else {
                callback(undefined)
            }
        }
    }
    xhttp.open("GET", "loadProgress?id=" + getMeta("bookId"))
    xhttp.setRequestHeader('Accept', 'application/json');
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

function markAsRead() {
    let endPosition = null
    if (getMeta("bookEnd")) {
        endPosition = getMeta("bookEnd")
    } else if (getMeta("size")) {
        endPosition = getMeta("size")
    }
    if (endPosition) {
        saveProgress(getMeta("bookId"), endPosition, function() {
            window.location = "/"
        })
    }
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

function toggleSettings() {
    let settings = document.getElementById('ch_settings')
    if (settings) {
        if (window.getComputedStyle(settings).display == 'none') {
            settings.style.display = 'inline-block'
            //alignSettingWidths()
        } else {
            settings.style.display = 'none'
        }
    }
}

// to remove?
function appendAll(parent, children) {
    if (children instanceof Setting) {
        parent.appendChild(children.label)
        parent.appendChild(children.controller)
        if (children.output) {
            parent.appendChild(children.output)
        } else {
            parent.appendChild(document.createElement('span'))
        }
    } else if (children) {
        for (let i = 0; i < children.length; i++) {
            parent.appendChild(children[i])
        }
    }
}

function reportError(message) {
    let errorPanel = document.getElementById('ch_errorPanel')
    errorPanel.innerHTML = message
}

function getRemoveProgressButton() {
    let label = document.createElement('span')
    label.innerHTML = ""
    let button = document.createElement('a')
    button.innerHTML = 'remove progress'

    let removeProgressFunction = (event) => {
        removeProgress()
    }
    let confirmationRequestFunction = (event) => {
        console.log(event)
        label.innerHTML = "are you sure?"
        button.onclick = removeProgressFunction
        button.classList.add('critical')
        window.setTimeout(function() {
            label.innerHTML = ""
            button.onclick = confirmationRequestFunction
            button.classList.remove('critical')
        }, 2500)
    }

    button.onclick = confirmationRequestFunction

    let controller = document.createElement('div')
    controller.classList.add('setting')
    controller.appendChild(label)
    controller.appendChild(button)
    return controller
}

function getMarkAsReadButton() {
    let label = document.createElement('span')
    label.innerHTML = ""
    let button = document.createElement('a')
    button.innerHTML = 'mark as read'

    let markAsReadFunction = (event) => {
        markAsRead()
    }
    let confirmationRequestFunction = (event) => {
        console.log(event)
        label.innerHTML = "are you sure?"
        button.onclick = markAsReadFunction
        button.classList.add('critical')
        window.setTimeout(function() {
            label.innerHTML = ""
            button.onclick = confirmationRequestFunction
            button.classList.remove('critical')
        }, 2500)
    }

    button.onclick = confirmationRequestFunction
    let controller = document.createElement('div')
    controller.classList.add('setting')
    controller.appendChild(label)
    controller.appendChild(button)
    return controller
}

function getBookId(bookPagesKey) {
    let tokens = bookPagesKey.split("_")
    if (tokens.length > 2) {
        return parseInt(tokens[1])
    } else {
        return undefined
    }
}

function cleanupBookPages(booksToKeep) {
    let keysToDelete = Object.keys(window.localStorage).filter(k => k.startsWith("bookPages_")).filter(k => ! booksToKeep.includes(getBookId(k)))
    for (let i = 0; i < keysToDelete.length; i++) {
        window.localStorage.removeItem(keysToDelete[i])
    }
}

function fullscreenAvailable() {
    return document.fullscreen != undefined
     || document.mozFullScreen != undefined
     || document.webkitIsFullScreen != undefined
     || document.msFullscreenElement != undefined
}

function toggleFullscreen() {
    var d = document.documentElement
    console.log(d)
    if (document.fullscreen || document.mozFullScreen || document.webkitIsFullScreen || document.msFullscreenElement) {
        if (document.exitFullscreen) {
            document.exitFullscreen();
        }
        else if (document.mozCancelFullScreen) {
            document.mozCancelFullScreen();
        }
        else if (document.webkitCancelFullScreen) {
            document.webkitCancelFullScreen();
        }
        else if (document.msExitFullscreen) {
            document.msExitFullscreen();
        }
    } else {
        if (d.requestFullscreen) {
            console.log("requestFullscreen")
            d.requestFullscreen()
        }
        else if (d.mozRequestFullScreen) {
            console.log("mozRequestFullscreen")
            d.mozRequestFullScreen()
        }
        else if (d.webkitRequestFullScreen) {
            console.log("webkitRequestFullscreen")
            d.webkitRequestFullScreen()
        }
        else if (d.msRequestFullscreen) {
            console.log("msRequestFullscreen")
            d.msRequestFullscreen()
        }
    }
}

function initFullscreenButton() {
    if (fullscreenAvailable()) {
        let p = document.createElement('p')
        let a = document.createElement('a')
        a.innerHTML = 'fullscreen'
        a.onclick = toggleFullscreen
        p.appendChild(a)
        let tools = document.getElementById('ch_tools')
        let backButton = document.getElementById('ch_back')
        if (backButton) {
            tools.insertBefore(p, backButton)
        } else {
            tools.appendChild(p)
        }
    }
}

function triggerSearchBuildHrefFunction(currentSearch) {
    return "javascript:triggerSearch('" + currentSearch.replaceAll("'", "\\'") + "')"
}

function searchLinkBuildHrefFunction(currentSearch) {
    return "/?search=" + encodeURIComponent(currentSearch)
}

function addCollectionLinkTokens(parent, collection, searchSeparator, buildHrefFunction) {
    let tokens = collection.split('/')
    let currentSearch = ""
    for (let i = 0; i < tokens.length; i++) {
        if (i > 0) {
            let slash = document.createElement("span")
            slash.innerHTML = "/"
            parent.appendChild(slash)
            currentSearch += searchSeparator
        }
        let a = document.createElement("a")
        currentSearch += tokens[i]
        a.href = buildHrefFunction(currentSearch)
        a.innerHTML = tokens[i]
        parent.appendChild(a)
    }
}

function initBookCollectionLinks() {
    let collectionParagraph = document.getElementById('ch_collection')
    if (collectionParagraph) {
        let collection = collectionParagraph.firstChild.text
        collectionParagraph.innerHTML = ''
        addCollectionLinkTokens(collectionParagraph, collection, '/', searchLinkBuildHrefFunction)
    }
}

function getCssProperty(name) {
    return getComputedStyle(document.documentElement).getPropertyValue(name)
}

function setCssProperty(name, value) {
    document.documentElement.style.setProperty(name, value)
}

function getDocumentHeight() {
    let body = document.body
    let html = document.documentElement

    let height = Math.max(body.scrollHeight, body.offsetHeight, html.clientHeight, html.scrollHeight, html.offsetHeight)
    return height
}

function getRGB(hexCode) {
    if (hexCode.startsWith("#")) {
        hexCode = hexCode.substring(1)
    }
    let components = hexCode.match(/.{1,2}/g)
    let rgb = [
        parseInt(components[0], 16),
        parseInt(components[1], 16),
        parseInt(components[2], 16)
    ]
    return rgb
}

function intToHex(i) {
    let h = i.toString(16)
    if (h.length < 2) {
        h = "0" + h
    }
    return h
}

function getHexCode(rgb) {
    return "#" + intToHex(rgb[0]) + intToHex(rgb[1]) + intToHex(rgb[2])
}

function computeLuminance(rgb) {
    let R = rgb[0]
    let G = rgb[1]
    let B = rgb[2]
    return Math.sqrt(0.299*R*R + 0.587*G*G + 0.114*B*B)
}

function reduceLuminanceTo(rgb, luminanceThreshold) {
    let currentLuminance = computeLuminance(rgb)
    while (currentLuminance > luminanceThreshold && (rgb[0] > 0 || rgb[1] > 0 || rgb[2] > 0)) {
        if (rgb[0] > 0) rgb[0] = rgb[0] - 1
        if (rgb[1] > 0) rgb[1] = rgb[1] - 1
        if (rgb[2] > 0) rgb[2] = rgb[2] - 1
        currentLuminance = computeLuminance(rgb)
    }
    return rgb
}

function getAppropriateStatusBarColor(originalColor) {
    let rgb = getRGB(originalColor)
    let newColor = reduceLuminanceTo(rgb, SETTING_DESIRED_STATUS_BAR_LUMINANCE.get())
    let newColorHex = getHexCode(newColor)
    return newColorHex
}