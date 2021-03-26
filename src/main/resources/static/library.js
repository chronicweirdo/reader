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
function addProgress(image, page, totalPages, downloaded) {
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
        span.innerText = "\u2713"
        span.classList.add("progresscheck")
    }
    if (downloaded) {
        span.classList.add("downloaded")
    }
    image.parentElement.appendChild(span)
}
function scaleImage(image, page, totalPages, downloaded) {
    if (page >= 0) {
        addProgress(image, page, totalPages, downloaded)
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
}
function getCollectionId(collection) {
    if (collection.length == 0) return "default"
    else return encodeURIComponent(collection)
}
function getCollectionHtml(collection) {
    var collectionId = getCollectionId(collection)
    var div = document.createElement("div")
    div.id = collectionId
    div.classList.add("collection-container")
    var h1 = document.createElement("h1")
    h1.innerHTML = collection
    div.appendChild(h1)
    return div
}
function insertCollectionHtml(collectionHtml) {
    var fin = document.getElementById("fin")
    if (fin != null) {
        document.body.insertBefore(collectionHtml, fin)
    } else {
        document.body.appendChild(collectionHtml)
    }
}

function insertOfflineMessage() {
    var tools = document.getElementById("tools")
    var offlineMessageHtml = document.createElement("p")
    offlineMessageHtml.innerHTML = "The application is in offline mode, only the latest read books are available."
    document.body.insertBefore(offlineMessageHtml, tools)
    tools.remove()
}

function addCollections(collections) {
    for (var i = 0; i < collections.length; i++) {
        var collectionId = getCollectionId(collections[i])
        if (document.getElementById(collectionId) == null) {
            insertCollectionHtml(getCollectionHtml(collections[i]))
        }
    }
}

function getBookHtml(book) {
    var a = document.createElement("a")
    a.classList.add("imgdiv")
    a.href = book.type + "?id=" + book.id
    a.setAttribute("bookid", book.id)
    var img = document.createElement("img")
    img.onload = function() {
        scaleImage(img, book.progress, book.pages, book.downloaded)
    }
    img.src = book.cover
    img.title = book.title
    a.appendChild(img)
    return a
}

function addBooks(books) {
    for (var i = 0; i < books.length; i++) {
        var book = books[i]
        var collectionId = getCollectionId(book.collection)
        var collectionDiv = document.getElementById(collectionId)
        if (collectionDiv != null) {
            collectionDiv.appendChild(getBookHtml(book))
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
        fin.id = "fin"
        fin.innerHTML = "~ Fin ~"
        document.body.appendChild(fin)
    }
}
function getEndOfCollection() {
    return document.getElementById("fin") != null
}
function removeExistingBooks() {
    var collections = document.getElementsByClassName("collection-container")
    while (collections.length > 0) {
        document.body.removeChild(collections.item(0))
    }
    var fin = document.getElementById("fin")
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
function searchForTerm() {
    removeExistingBooks()
    setCurrentPage(-1)
    loadNextPage(loadUntilPageFull)
}

function loadLatestRead() {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                var books = JSON.parse(this.responseText)
                if (books.length > 0) {
                    for (var i = 0; i < books.length; i++) {
                        var book = books[i]
                        var collectionDiv = document.getElementById("ch_latestRead")
                        if (collectionDiv != null) {
                            collectionDiv.appendChild(getBookHtml(book))
                        }
                    }
                }
            }
        }
    }
    xhttp.open("GET", "latestRead")
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
                    console.log(response)
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
        navigator.serviceWorker.register('/serviceworker.js').then(function(registration) {
            console.log("service worker registered successfully: ", registration)
            registration.update()
        }, function(error) {
            console.log("service worker registration failed: ", error)
        })

    }

    loadLatestRead()

    var searchParameter = getSearchUrlParameter()
    if (searchParameter != null) {
        getSearch().value = searchParameter
    }
    addSearchTriggerListener()
    searchForTerm()
}

window.onscroll = function(ev) {
    if ((getViewportHeight() + getScrollTop()) >= getDocumentHeight() - scrollThreshold) {
        if (! getEndOfCollection()) {
            loadNextPage(null)
        }
    }
}