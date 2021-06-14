var FIN_ID = "fin"
var COLLECTION_CONTAINER_CLASS = "collection-container"
var COLLECTION_TITLE_CLASS = "collection-title"

function showSpinner() {
    var spinner = document.getElementById("spinner")
    spinner.style.display = "block"
}

function hideSpinner() {
    var spinner = document.getElementById("spinner")
    spinner.style.display = "none"
}

function addPagenum(image, page, totalPages) {
    var span = document.createElement("span")
    span.innerText = page + " / " + totalPages
    span.classList.add("pagenum")
    image.parentElement.appendChild(span)
}

function getSvgCheck() {
    let svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg")
    svg.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink")
    svg.setAttribute("style", "margin:auto;display:block;")
    svg.setAttribute("viewBox", "0 0 12 10")
    svg.setAttribute("preserveAspectRatio", "xMidYMid")

    let path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
    path.setAttribute("d", "M 3 3 L 1 5 L 5 9 L 11 3 L 9 1 L 5 5 Z ")

    svg.appendChild(path)
    return svg
}

function getSvgCross() {
    let svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
    svg.setAttribute("xmlns", "http://www.w3.org/2000/svg")
    svg.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink")
    svg.setAttribute("style", "margin:auto;display:block;")
    svg.setAttribute("viewBox", "0 0 12 10")
    svg.setAttribute("preserveAspectRatio", "xMidYMid")

    let path = document.createElementNS('http://www.w3.org/2000/svg', 'path')
    path.setAttribute("d", "M 0 1 L 1 2 L 0 3 L 1 4 L 2 3 L 3 4 L 4 3 L 3 2 L 4 1 L 3 0 L 2 1 L 1 0 Z")

    svg.appendChild(path)
    return svg
}

function applyTitles() {

    /*let titleDisplay = getComputedStyle(document.documentElement).getPropertyValue('--title-display')
    console.log(titleDisplay)*/

    if (getSetting(SETTING_LIBRARY_DISPLAY_TITLE)) {
        // we show titles
        document.documentElement.style.setProperty('--title-display', 'inline-block')
        document.getElementById("toggleTitles").classList.add("selected")
    } else {
        // we hide titles
        document.documentElement.style.setProperty('--title-display', 'none')
        document.getElementById("toggleTitles").classList.remove("selected")
    }

    /*if (titleDisplay == "none") {
        document.documentElement.style.setProperty('--title-display', 'inline-block')
        document.getElementById("toggleTitles").classList.add("selected")
    } else {
        document.documentElement.style.setProperty('--title-display', 'none')
        document.getElementById("toggleTitles").classList.remove("selected")
    }*/
}

function toggleTitles() {
    if (getSetting(SETTING_LIBRARY_DISPLAY_TITLE)) {
        putSetting(SETTING_LIBRARY_DISPLAY_TITLE, false)
    } else {
        putSetting(SETTING_LIBRARY_DISPLAY_TITLE, true)
    }
    applyTitles()
}

/*function addProgress(image, page, totalPages, downloaded) {
    var span = document.createElement("span")
    if (page < totalPages - 1) {
        span.classList.add("progressbar")
        var percent = ((page + 1) / parseFloat(totalPages)) * 100
        span.title = "read " + Math.floor(percent) + "%"
        var prog = document.createElement("span")
        prog.classList.add("read")
        prog.style.width = (page / (totalPages-1) * 100) + "%"
        span.appendChild(prog)
    } else {
        //span.innerText = '<path fill="#ffffff" stroke="#000000" d="M 3 3 L 1 5 L 5 9 L 11 3 L 9 1 L 5 5 Z " stroke-width="1"></path></svg>'
        span.appendChild(getSvgCheck())
        span.classList.add("progresscheck")
    }
    if (downloaded) {
        span.classList.add("downloaded")
    }
    image.parentElement.appendChild(span)
}*/
/*function addTitle(image, title) {
    var span = document.createElement("span")
    span.classList.add("title")
    span.innerHTML = title
    image.parentElement.appendChild(span)
}*/

/*function scaleImage(image, page, totalPages, downloaded, title) {
    if (page >= 0) {
        addProgress(image, page, totalPages, downloaded)
    }
    if (getSetting(SETTING_LIBRARY_DISPLAY_TITLE)) {
        addTitle(image, title)
    }
    var imageContainer = document.getElementsByClassName("imgdiv")[0]
    var expectedHeight = imageContainer.offsetHeight
    var expectedWidth = imageContainer.offsetWidth
    if (image.naturalWidth / image.naturalHeight > expectedWidth / expectedHeight) {
        image.style.height = "100%"
    } else {
        image.style.width = "100%"
    }
    var differenceWidth = image.offsetWidth - expectedWidth
    var differenceHeight = image.offsetHeight - expectedHeight
    image.style.left = (- differenceWidth / 2) + "px"
    image.style.top = (- differenceHeight / 2) + "px"
}*/

function getCollectionId(collection) {
    if (collection.length == 0) return "default"
    else return encodeURIComponent(collection)
}

/*function getCollectionHtml(collection) {
    var collectionId = getCollectionId(collection)
    var div = document.createElement("div")
    div.id = collectionId
    div.classList.add("collection-container")
    if (collection.length > 0) {
        var h1 = document.createElement("h1")
        addCollectionLinkTokens(h1, collection, '/', triggerSearchBuildHrefFunction)
        div.appendChild(h1)
    }
    return div
}*/



function insertCollectionHtml(collection) {
    if (collection.length > 0) {
        let collectionId = getCollectionId(collection)

        let title = document.createElement("h1")
        title.classList.add(COLLECTION_TITLE_CLASS)
        addCollectionLinkTokens(title, collection, '/', triggerSearchBuildHrefFunction)

        let container = document.createElement("ul")
        container.classList.add(COLLECTION_CONTAINER_CLASS)
        container.id = collectionId

        let fin = document.getElementById(FIN_ID)
        if (fin != null) {
            document.body.insertBefore(title, fin)
            document.body.insertBefore(container, fin)
        } else {
            document.body.appendChild(title)
            document.body.appendChild(container)
        }
    }
}

/*function insertOfflineMessage() {
    var tools = document.getElementById("tools")
    var offlineMessageHtml = document.createElement("h1")
    offlineMessageHtml.id = 'fin'
    offlineMessageHtml.innerHTML = "~ The application is in offline mode, only the latest read books are available. ~"
    document.body.insertBefore(offlineMessageHtml, tools)
    tools.remove()
}*/

function addCollections(collections) {
    for (var i = 0; i < collections.length; i++) {
        var collectionId = getCollectionId(collections[i])
        if (document.getElementById(collectionId) == null) {
            insertCollectionHtml(collections[i])
        }
    }
}

function getBookHtml(book) {
    /*var a = document.createElement("a")
    a.classList.add("imgdiv")
    a.href = book.type + "?id=" + book.id
    a.setAttribute("bookid", book.id)
    var img = document.createElement("img")
    img.onload = function() {
        scaleImage(img, book.progress, book.pages, book.downloaded, book.title)
    }
    img.src = book.cover
    img.title = book.title
    a.appendChild(img)
    return a*/

    // <li bookid="1" size="10" progress="3" downloaded="true"><a><span class="cover"><img src="01 - Warp Tour (2017).jpg" onload="formatImage(this)"></span><span class="title">01 - Warp Tour (2017)</span></a></li>
    let li = document.createElement("li")
    li.setAttribute("bookid", book.id)
    li.setAttribute("size", book.pages)
    li.setAttribute("progress", book.progress)
    li.setAttribute("downloaded", book.downloaded)

    let a = document.createElement("a")
    a.href = book.type + "?id=" + book.id

    let cover = document.createElement("span")
    cover.classList.add("cover")

    let img = document.createElement("img")
    img.onload = function() {
        formatImage(this)
    }
    img.src = book.cover
    img.title = book.title

    cover.appendChild(img)
    a.appendChild(cover)

    let title = document.createElement("span")
    title.classList.add("title")
    title.innerHTML = book.title

    a.appendChild(title)

    li.appendChild(a)
    return li
}

function addBooks(books) {
    for (let i = 0; i < books.length; i++) {
        let book = books[i]
        let collectionId = getCollectionId(book.collection)
        let container = document.getElementById(collectionId)
        if (container != null) {
            container.appendChild(getBookHtml(book))
        }
    }
}

function setCurrentPage(currentPage) {
    document.currentPage = currentPage
}

function getCurrentPage() {
    if (document.currentPage) {
        return document.currentPage
    } else {
        document.currentPage = 0
        return document.currentPage
    }
}

function setEndOfCollection() {
    if (! getEndOfCollection()) {
        var fin = document.createElement("h1")
        fin.id = FIN_ID
        fin.innerHTML = "~ Fin ~"
        document.body.appendChild(fin)
    }
}

function getEndOfCollection() {
    return document.getElementById(FIN_ID) != null
}



function removeExistingBooks() {
    let collections = document.getElementsByClassName(COLLECTION_CONTAINER_CLASS)
    while (collections.length > 0) {
        document.body.removeChild(collections.item(0))
    }
    let collectionTitles = document.getElementsByClassName(COLLECTION_TITLE_CLASS)
    while (collectionTitles.length > 0) {
        document.body.removeChild(collectionTitles.item(0))
    }
    let fin = document.getElementById(FIN_ID)
    if (fin != null) {
        document.body.removeChild(fin)
    }
}

function getSearch() {
    return document.getElementById("search")
}

function getTerm() {
    return getSearch().value
}

function clearTerm() {
    var search = getSearch()
    if (search.value === "") {
        window.location = "/"
    } else {
        search.value = ""
        search.dispatchEvent(new Event('keyup'))
        search.focus()
    }
}

function triggerSearch(text) {
    let search = getSearch()
    search.value = text
    searchForTerm()
}

function searchForTerm() {
    removeExistingBooks()
    setCurrentPage(-1)
    loadNextPage(loadUntilPageFull)
}

function addProgress(image, progress, size, downloaded) {
    var span = document.createElement("span")
    if (progress < size - 1) {
        span.classList.add("progressbar")
        var percent = ((progress + 1) / parseFloat(size)) * 100
        span.title = "read " + Math.floor(percent) + "%"
        var prog = document.createElement("span")
        prog.classList.add("read")
        prog.style.width = (progress / (size-1) * 100) + "%"
        span.appendChild(prog)
    } else {
        span.appendChild(getSvgCheck())
        span.classList.add("progresscheck")
    }
    if (downloaded) {
        span.classList.add("downloaded")
    }
    image.parentElement.appendChild(span)
    /*let title = image.closest('a').querySelector('.title')
    console.log(title)
    image.closest('a').insertBefore(span, title)*/
}

function formatImage(img) {
    /*console.log(img)
    console.log("natural height: " + img.naturalHeight)
    console.log("natural width: " + img.naturalWidth)
    console.log("container height: " + img.offsetHeight)
    console.log("container width: " + img.offsetWidth)*/
    let parent = img.parentElement
    //console.log("parent: " + parent)


    if (img.naturalWidth / img.naturalHeight > parent.offsetWidth / parent.offsetHeight) {
        let newWidth = img.naturalWidth * (parent.offsetHeight / img.naturalHeight)
        //console.log("new width: " + newWidth)
        let differenceWidth = parent.offsetWidth - newWidth
        //console.log("width difference: " + differenceWidth)
        let differencePercentage = (differenceWidth / parent.offsetWidth) * 100
        //console.log("width difference percentage: " + differencePercentage)

        img.style.height = "100%"
        //img.style.width = newWidth
        img.style.left = (differencePercentage / 2) + "%"
    } else {
        let newHeight = img.naturalHeight * (parent.offsetWidth / img.naturalWidth)
        //console.log("new height: " + newHeight)
        let differenceHeight = parent.offsetHeight - newHeight
        //console.log("height difference: " + differenceHeight)
        let differencePercentage = (differenceHeight / parent.offsetHeight) * 100

        //img.style.height = newHeight
        img.style.width = "100%"
        img.style.top = (differencePercentage / 2) + "%"
    }
    let bookElement = img.closest('li')
    let size = parseInt(bookElement.getAttribute("size"))
    //console.log("size: " + size)
    let progress = parseInt(bookElement.getAttribute("progress"))
    //console.log("progress: " + progress)
    let downloaded = bookElement.getAttribute("downloaded") == "true"
    //console.log("downloaded: " + downloaded)
    if (size != NaN && size > 0 && progress != NaN) {
        addProgress(img, progress, size, downloaded)
    }
}

function loadLatestRead() {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                var books = JSON.parse(this.responseText)
                if (books.length > 0) {
                    let bookIds = []
                    var collectionContainer = document.getElementById("ch_latestRead")
                    for (var i = 0; i < books.length; i++) {
                        var book = books[i]
                        bookIds.push(book.id)

                        if (collectionContainer != null) {
                            collectionContainer.appendChild(getBookHtml(book))
                        }
                    }
                    collectionContainer.style.display = "grid"
                    cleanupBookPages(bookIds)
                }
            }
        }
    }
    xhttp.open("GET", "latestRead?limit=" + getSetting(SETTING_LATEST_READ_LIMIT))
    xhttp.send()
}

function loadNextPage(callback) {
    if (document.searchTimestamp === undefined || document.searchTimestamp == null) {
        var pagenum = getCurrentPage() + 1
        var term = getTerm()
        var xhttp = new XMLHttpRequest();
        var timestamp = + new Date()
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && document.searchTimestamp == timestamp) {
                hideSpinner()
                document.searchTimestamp = null
                if (this.status == 200) {
                    setCurrentPage(pagenum)
                    var response = JSON.parse(this.responseText)
                    if (response.offline && response.offline == true) {
                        insertOfflineMessage()
                    } else if (response.books.length > 0) {
                        addCollections(response.collections)
                        addBooks(response.books)
                        if (callback != null) callback()
                    } else {
                        setEndOfCollection()
                        if (callback != null) callback()
                    }

                }
            }
        }
        xhttp.open("GET", "search?term=" + encodeURIComponent(term) + "&page=" + pagenum)
        document.searchTimestamp = timestamp
        showSpinner()
        xhttp.send()
    }
}

function loadCollections(callback) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4 && this.status == 200) {
            var collections = JSON.parse(this.responseText)
            if (callback != null) {
                callback(collections)
            }
        }
    }
    xhttp.open("GET", "collections")
    xhttp.send()
}

function createCollectionsUl(collections) {
    var ul = document.createElement("ul")

    collections.forEach(c => {
        var li = document.createElement("li")
        li.innerText = c
        ul.appendChild(li)
    })

    return ul
}

function showCollectionsDialog() {
    loadCollections(function(collections) {
        var collectionsUl = createCollectionsUl(collections)
        var collectionsDialog = document.getElementById("collections")
        var collectionsContent = collectionsDialog.getElementsByTagName("p")[0]
        collectionsContent.appendChild(collectionsUl)
        collectionsDialog.classList.add("visible")
    })
}

function hideCollectionsDialog() {
    var collectionsDialog = document.getElementById("collections")
    var collectionsContent = collectionsDialog.getElementsByTagName("p")[0]
    collectionsContent.innerHTML = ""
    collectionsDialog.classList.remove("visible")
}

function getViewportHeight() {
    return Math.max(document.documentElement.clientHeight, window.innerHeight || 0)
}

function getScrollTop() {
    return document.documentElement.scrollTop
}

function getDocumentHeight() {
    return document.body.offsetHeight
}

function loadUntilPageFull() {
    if ((document.body.offsetHeight <= getViewportHeight()) && ! getEndOfCollection()) {
        loadNextPage(loadUntilPageFull)
    }
}

function addSearchTriggerListener() {
    var search = document.getElementById("search")
    search.addEventListener('keyup', function (e) {
        if (document.searchTimeout && document.searchTimeout != null) {
            window.clearTimeout(document.searchTimeout)
            document.searchTimeout = null
        }
        if (e.keyCode === 13) {
            // if enter, search
            searchForTerm()
        } else {
            // if other key, wait to see if finished typing
            document.searchTimeout = window.setTimeout(searchForTerm, 1000)
        }
    })
}

function getSearchUrlParameter() {
    var urlString = window.location.href
    var url = new URL(urlString)
    var search = url.searchParams.get("search")
    if (search == null) return null
    else return decodeURIComponent(search)
}

var scrollThreshold = 20

window.onload = function() {
    if('serviceWorker' in navigator) {
        navigator.serviceWorker.getRegistrations().then(registrations => {
            navigator.serviceWorker.register('/serviceworker.js').then(function(registration) {
                registration.update().then(() => {
                    loadLatestRead()
                })
            }, function(error) {
                console.log("service worker registration failed: ", error)
            })
        });
    } else {
        loadLatestRead()
    }

    var searchParameter = getSearchUrlParameter()
    if (searchParameter != null) {
        getSearch().value = searchParameter
    }
    addSearchTriggerListener()
    searchForTerm()

    applyTitles()
    document.documentElement.style.setProperty('--accent-color', getSetting(SETTING_ACCENT_COLOR));
    setStatusBarColor(getSetting(SETTING_ACCENT_COLOR));
    document.documentElement.style.setProperty('--foreground-color', getSetting(SETTING_FOREGROUND_COLOR));
    document.documentElement.style.setProperty('--background-color', getSetting(SETTING_BACKGROUND_COLOR));
}

window.onscroll = function(ev) {
    if ((getViewportHeight() + getScrollTop()) >= getDocumentHeight() - scrollThreshold) {
        if (! getEndOfCollection()) {
            loadNextPage(null)
        }
    }
}