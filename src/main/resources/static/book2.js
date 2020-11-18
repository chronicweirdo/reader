var data = "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam bibendum dapibus tempus. Duis fringilla nibh sed turpis malesuada, quis vestibulum dui tincidunt. Nam in facilisis risus, a vehicula libero. Fusce at vestibulum tellus. Quisque et posuere nunc. Sed est mi, scelerisque non massa in, dictum gravida augue. Praesent vel aliquam velit, euismod varius ipsum. Nulla pretium odio mauris, quis tempus magna rhoncus nec. In tincidunt velit non quam ultrices faucibus. Phasellus sit amet dictum magna. Etiam tincidunt purus est, at tincidunt orci mollis congue. Ut interdum accumsan ligula non molestie. Ut feugiat quam suscipit porttitor viverra. Praesent a massa ipsum.\nNam in tempus elit. Ut iaculis lorem id quam rutrum interdum. Morbi est enim, aliquet vitae rhoncus ac, cursus quis risus. Nam non mi sit amet nulla consequat dictum. Praesent vitae laoreet quam, a gravida ligula. Duis volutpat mi risus, vel blandit orci lacinia tincidunt. Sed congue est id quam dignissim molestie. Duis dui orci, fermentum ac dolor a, hendrerit facilisis leo. Aenean ultrices faucibus augue vel convallis. Nullam nec laoreet nunc.</p><p>Duis elementum sapien ut est feugiat imperdiet. Nullam mi ex, vehicula at ultrices ac, tincidunt id odio. Ut dolor dui, semper eget purus sed, fermentum ornare felis. Vivamus sit amet nisi non dui euismod cursus. Maecenas cursus leo nisi, quis rutrum purus mollis eget. Mauris orci augue, rhoncus vel luctus a, accumsan vel ligula. In eget tellus vestibulum, molestie dolor at, maximus nisl. Sed rutrum mi eu massa varius, non mollis lacus congue. Vivamus id nisl neque.</p><p>Aliquam a arcu ullamcorper, pretium lacus vitae, tristique nunc. Nullam pellentesque blandit commodo. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nulla placerat finibus vestibulum. Phasellus finibus tristique enim a feugiat. Nam cursus scelerisque ante, in congue nibh tempus quis. Donec dictum, lectus a lobortis porttitor, nisi felis gravida augue, a ullamcorper turpis lorem sed massa.</p><p>Cras turpis mi, placerat dictum risus quis, aliquam dictum risus. Sed at metus sed urna facilisis dictum sit amet et mauris. Fusce massa justo, congue quis porta laoreet, sagittis consequat enim. Donec lobortis, ante a iaculis dignissim, eros purus ultricies lacus, a ultricies ex libero at eros. Vestibulum sit amet pulvinar ante. Proin molestie iaculis dolor, interdum sodales libero congue consectetur. Integer pulvinar sodales dolor, faucibus posuere augue vehicula laoreet. Duis commodo placerat egestas. Morbi pharetra, magna vitae fringilla bibendum, massa nunc sodales est, ut scelerisque arcu est non tortor. Pellentesque vel semper augue, ut maximus augue. Quisque ut lectus luctus elit vehicula faucibus ac eu metus. Sed et consectetur metus, at malesuada erat. Suspendisse aliquet ullamcorper gravida. Proin iaculis dictum eleifend. Vestibulum dapibus diam at lectus gravida egestas vitae nec arcu.</p>"

function scrollNecessary(el) {
    return el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth
}

function getZoom() {
    if (document.bookZoom) {
        return document.bookZoom
    } else {
        var zoom = parseFloat(getMeta("bookZoom"))
        document.bookZoom = zoom
        return document.bookZoom
    }
}

function getPagesKey() {
    return getMeta("bookId") + "_" + getViewportWidth() + "_" + getViewportHeight() + "_" + getZoom()
}

function getPageFor(position) {
    var pagesKey = getPagesKey()
    //var savedPages = window.localStorage.getItem(pagesKey)
    var savedPages = document.savedPages
    if (savedPages != null) {
        // search for page
        for (var i = 0; i < savedPages.length; i++) {
            if (savedPages[i].start <= position && position <= savedPages[i].end) {
                // we found the page
                return savedPages[i]
            }
        }
    }
    // no page available
    return null
}

function computePagesFor(position) {
    if (! document.computationQueue.includes(position)) {
        document.computationQueue.push(position)
    }
    if (document.computationQueue.length > 0) {
        console.log("triggerring computation")
        window.setTimeout(function() {
            startComputation()
        }, 100)
    }
}

function loadPageFor(position, callback) {
    console.log("loading page for " + position)
    var page = getPageFor(position)
    if (page == null) {
        // trigger page computation
        computePagesFor(position)

        // keep trying to retrieve the page periodically, until we have it
        window.setTimeout(function() {
            loadPageFor(position, callback)
        }, 100)
    } else {
        console.log("found page for " + position)
        var data = loadDataFor(page.start, page.end)
        callback(data)
    }
}

function loadDataFor(start, end) {
    return data.substring(start, end+1)
}

function displayPageFor(position) {
    // show spinner
    loadPageFor(position, function(page) {
        var content = document.getElementById("content")
        content.innerHTML = page
        // hide spinner
    })
}

function getSectionFor(position) {
    return data
}

function startComputation() {
    console.log("starting computation")
    if (document.computationQueue.length > 0) {
        var position = document.computationQueue.shift()
        console.log("computing page for position " + position)
        var section = getSectionFor(position)
        var shadowContent = document.getElementById("shadowContent")
        shadowContent.innerHTML = ""

        var previousEnd = position + 1
        var end = position + 1
        while (scrollNecessary(shadowContent) == false && end < section.length) {
            console.log("growing content")
            previousEnd = end
            end = end + 1
            // grow content
            shadowContent.innerHTML = section.substring(position, end)
        }

        // store page
        var pagesKey = getPagesKey()
        //var savedPages = window.localStorage.getItem(pagesKey)
        if (document.savedPages == null) {
            document.savedPages = []
        }
        document.savedPages.push({start: position, end: previousEnd})
        //window.localStorage.setItem(pagesKey, savedPages)


        // if we did not finish finding pages in section, request computation of next page
        if (previousEnd < section.length - 1) {
            computePagesFor(previousEnd + 1)
            /*window.setTimeout(function() {
                startComputation()
            }, 100)*/
        }
    }
}

window.onload = function() {
    var startPosition = num(getMeta("startPosition"))
    console.log("start position: " + startPosition)

    document.computationQueue = []
    //startComputation()
    displayPageFor(startPosition)
}