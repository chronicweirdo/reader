var FIN_ID = "fin"
var CLEAR_SEARCH_ID = "clearsearch"
var SEARCH_ID = "search"
var SPINNER_ID = "spinner"
var TOGGLE_TITLES_ID = "toggletitles"
var COLLECTION_CONTAINER_CLASS = "collection-container"
var COLLECTION_TITLE_CLASS = "collection-title"
var ACTIVE_CLASS = "active"
var ENTER_KEY_CODE = 13
var SCROLL_THRESHOLD = 20
var RELOAD_LIBRARY_MESSAGE = "Reload Library"

function getSpinner() {
    return document.getElementsByClassName("spinner")[0]
}

function showSpinner() {
    let collections = document.getElementsByTagName("ul")
    let lastCollection = collections[collections.length-1]
    if (lastCollection.id != "ch_latestRead") {
        let spinner = getSpinner().cloneNode(true)
        spinner.classList.add("active-spinner")
        spinner.style.display = "inline-block"
        let cover = document.createElement("span")
        cover.classList.add("cover")
        cover.appendChild(spinner)
        let li = document.createElement("li")
        li.appendChild(cover)
        lastCollection.appendChild(li)
    }
}

function hideSpinner() {
    let spinners = document.getElementsByClassName("active-spinner")
    while (spinners.length > 0) {
        spinners[0].closest("li").remove()
    }
}

function getToggleTitles() {
    return document.getElementById(TOGGLE_TITLES_ID)
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

function applyTitles() {
    let toggleTitles = getToggleTitles()
    if (SETTING_LIBRARY_DISPLAY_TITLE.get()) {
        setCssProperty('--title-display', 'block')
        toggleTitles.classList.add(ACTIVE_CLASS)
        toggleTitles.title = "Hide Book Titles"
    } else {
        setCssProperty('--title-display', 'none')
        toggleTitles.classList.remove(ACTIVE_CLASS)
        toggleTitles.title = "Show Book Titles"
    }
}

function toggleTitles() {
    if (SETTING_LIBRARY_DISPLAY_TITLE.get()) {
        SETTING_LIBRARY_DISPLAY_TITLE.put(false)

    } else {
        SETTING_LIBRARY_DISPLAY_TITLE.put(true)
    }
    applyTitles()
}

function getCollectionId(collection) {
    if (collection.length == 0) return "default"
    else return encodeURIComponent(collection)
}

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

function addCollections(collections) {
    for (var i = 0; i < collections.length; i++) {
        var collectionId = getCollectionId(collections[i])
        if (document.getElementById(collectionId) == null) {
            insertCollectionHtml(collections[i])
        }
    }
}

function getBookHtml(book) {
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

function cleanupLatestAddedSection() {
    let hideLatestAdded = getTerm() != ""
    if (hideLatestAdded || document.getElementById("ch_latestAdded").getElementsByTagName('img').length == 0) {
        document.getElementById("ch_latestAdded").style.display = "none"
        document.getElementById("ch_latestAddedTitle").style.display = "none"
    } else {
        document.getElementById("ch_latestAdded").style.display = "grid"
        document.getElementById("ch_latestAddedTitle").style.display = "block"
    }
}

function getSearch() {
    return document.getElementById(SEARCH_ID)
}

function getClearSearch() {
    return document.getElementById(CLEAR_SEARCH_ID)
}

function getTerm() {
    let search = getSearch()
    if (search) {
        return search.value
    }
    return null
}

function clearTerm() {
    var search = getSearch()
    if (search) {
        if (search.value === "") {
            window.location = "/"
        } else {
            search.value = ""
            search.dispatchEvent(new Event('keyup'))
            search.focus()
        }
    } else {
        window.location = "/"
    }
}

function triggerSearch(text) {
    let search = getSearch()
    search.value = text
    updateClearSearch()
    searchForTerm()
}

function searchForTerm() {
    removeExistingBooks()
    cleanupLatestAddedSection()
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
}

function formatImage(img) {
    let parent = img.parentElement

    if (img.naturalWidth / img.naturalHeight > parent.offsetWidth / parent.offsetHeight) {
        let newWidth = img.naturalWidth * (parent.offsetHeight / img.naturalHeight)
        let differenceWidth = parent.offsetWidth - newWidth
        let differencePercentage = (differenceWidth / parent.offsetWidth) * 100
        img.style.height = "100%"
        img.style.left = (differencePercentage / 2) + "%"
    } else {
        let newHeight = img.naturalHeight * (parent.offsetWidth / img.naturalWidth)
        let differenceHeight = parent.offsetHeight - newHeight
        let differencePercentage = (differenceHeight / parent.offsetHeight) * 100
        img.style.width = "100%"
        img.style.top = (differencePercentage / 2) + "%"
    }
    let bookElement = img.closest('li')
    let size = parseInt(bookElement.getAttribute("size"))
    let progress = parseInt(bookElement.getAttribute("progress"))
    let downloaded = bookElement.getAttribute("downloaded") == "true"
    if (size != NaN && size > 0 && progress != NaN && progress != -1) {
        addProgress(img, progress, size, downloaded)
    }
}

function loadLatestRead() {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                var collectionContainer = document.getElementById("ch_latestRead")
                collectionContainer.innerHTML = ""
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
                    document.getElementById("ch_latestReadTitle").style.display = "block"
                    collectionContainer.style.display = "grid"
                    cleanupBookPages(bookIds)
                } else {
                    document.getElementById("ch_latestRead").style.display = "none"
                    document.getElementById("ch_latestReadTitle").style.display = "none"
                }
            }
        }
    }
    xhttp.open("GET", "latestRead?limit=" + SETTING_LATEST_READ_LIMIT.get())
    xhttp.send()
}

function loadLatestAdded() {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                var books = JSON.parse(this.responseText)
                if (books.length > 0) {
                    let bookIds = []
                    var collectionContainer = document.getElementById("ch_latestAdded")
                    for (var i = 0; i < books.length; i++) {
                        var book = books[i]
                        bookIds.push(book.id)

                        if (collectionContainer != null) {
                            collectionContainer.appendChild(getBookHtml(book))
                        }
                    }
                    cleanupBookPages(bookIds)
                }
                cleanupLatestAddedSection()
            }
        }
    }
    let latestAddedLimit = SETTING_LATEST_ADDED_LIMIT.get()
    if (latestAddedLimit > 0) {
        xhttp.open("GET", "latestAdded?limit=" + latestAddedLimit)
        xhttp.send()
    }
}

function setToOfflineMode() {
    let search = getSearch()
    if (search) {
        let offlineMessage = document.createElement("span")
        offlineMessage.classList.add("offline-message")
        offlineMessage.innerHTML = "The application is in offline mode, only the latest read books are available."
        document.getElementById("search").replaceWith(offlineMessage)

        let moreElement = document.getElementById("more")
        moreElement.classList.remove(ACTIVE_CLASS)
        moreElement.href = "/"
        moreElement.title = RELOAD_LIBRARY_MESSAGE
    }
}

function loadNextPage(callback) {
    if (getSearch() && (document.searchTimestamp === undefined || document.searchTimestamp == null)) {
        var pagenum = getCurrentPage() + 1
        var term = getTerm()
        var xhttp = new XMLHttpRequest();
        var timestamp = + new Date()
        xhttp.onreadystatechange = function() {
            if (this.readyState == 4 && document.searchTimestamp == timestamp) {
                document.searchTimestamp = null
                if (this.status == 200) {
                    setCurrentPage(pagenum)
                    var response = JSON.parse(this.responseText)
                    if (response.offline && response.offline == true) {
                        setToOfflineMode()
                    } else if (response.books.length > 0) {
                        hideSpinner()
                        addCollections(response.collections)
                        addBooks(response.books)
                        if (callback != null) callback()
                    } else {
                        hideSpinner()
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

function updateClearSearch() {
    let clearSearch = getClearSearch()
    if (getSearch().value.length > 0) {
        clearSearch.classList.add(ACTIVE_CLASS)
        clearSearch.title = "Clear Search Field"
    } else {
        clearSearch.classList.remove(ACTIVE_CLASS)
        clearSearch.title = RELOAD_LIBRARY_MESSAGE
    }
}

function addSearchTriggerListener() {
    var search = getSearch()
    search.addEventListener('keyup', function (e) {
        updateClearSearch()
        if (document.searchTimeout && document.searchTimeout != null) {
            window.clearTimeout(document.searchTimeout)
            document.searchTimeout = null
        }
        if (e.keyCode === ENTER_KEY_CODE) {
            searchForTerm()
        } else {
            // if other key, wait to see if finished typing
            document.searchTimeout = window.setTimeout(searchForTerm, 1000)
        }
    })
}

function getSearchUrlParameter() {
    let urlString = window.location.href
    let url = new URL(urlString)
    let search = url.searchParams.get(SEARCH_ID)
    if (search == null) return null
    else return decodeURIComponent(search)
}

window.onload = function() {
    if('serviceWorker' in navigator) {
        navigator.serviceWorker.getRegistrations().then(registrations => {
            navigator.serviceWorker.register('/serviceworker.js').then(function(registration) {
                // we load latest read both on success and on failure (when offline)
                registration.update().then(loadLatestRead,loadLatestRead)
            }, function(error) {
                console.log("service worker registration failed: ", error)
            })
        });
    }
    loadLatestRead()
    loadLatestAdded()

    var searchParameter = getSearchUrlParameter()
    if (searchParameter != null) {
        getSearch().value = searchParameter
    }
    addSearchTriggerListener()
    searchForTerm()
    updateClearSearch()

    applyTitles()
    setCssProperty('--accent-color', SETTING_ACCENT_COLOR.get())
    setStatusBarColor(SETTING_ACCENT_COLOR.get())
    setCssProperty('--foreground-color', SETTING_FOREGROUND_COLOR.get());
    setCssProperty('--background-color', SETTING_BACKGROUND_COLOR.get());
}

window.onscroll = function(ev) {
    if ((getViewportHeight() + getScrollTop()) >= getDocumentHeight() - SCROLL_THRESHOLD) {
        if (! getEndOfCollection()) {
            loadNextPage(null)
        }
    }
}