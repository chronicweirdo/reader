importScripts('bookNode.js')

var CACHE_NAME = 'chronic-reader-cache-1'
var DATABASE_NAME = 'chronic-reader-db'
var DATABASE_VERSION = 1
var REQUESTS_TABLE = 'requests'
var PROGRESS_TABLE = 'progress'
var BOOKS_TABLE = 'books'
var WORKER_TABLE = 'worker'
var ID_INDEX = 'id'
var SERVER_REQUEST_TIMEOUT = 1000

var downloadingBook = new Set()

var db

function getDb() {
    return new Promise((resolve, reject) => {
        if (! db) {
            const request = indexedDB.open(DATABASE_NAME, DATABASE_VERSION)
            request.onerror = function(event) {
                reject()
            }
            request.onsuccess = function(event) {
                db = event.target.result
                resolve(event.target.result)
            }
            request.onupgradeneeded = function(event) {
                let localDb = event.target.result
                var requestsStore = localDb.createObjectStore(REQUESTS_TABLE, {keyPath: 'url'})
                requestsStore.createIndex(ID_INDEX, ID_INDEX, { unique: false })
                var progressStore = localDb.createObjectStore(PROGRESS_TABLE, {keyPath: 'id'})
                var booksStore = localDb.createObjectStore(BOOKS_TABLE, {keyPath: 'id'})
                var workerStore = localDb.createObjectStore(WORKER_TABLE, {keyPath: 'id'})
            }
        } else {
            resolve(db)
        }
    })
}

function deleteDb() {
    return new Promise((resolve, reject) => {
        var req = indexedDB.deleteDatabase(DATABASE_NAME)
        req.onsuccess = function () {
            console.log("Deleted database successfully")
            resolve()
        }
        req.onerror = function () {
            console.log("Couldn't delete database")
            reject()
        }
        req.onblocked = function () {
            console.log("Couldn't delete database due to the operation being blocked");
            reject()
        }
    })
}

getDb()

var downloadStack = []

function pushToDownloadStack(o) {
    let existingIndex = downloadStack.findIndex(u => {
        return u.id == o.id && u.kind == o.kind && u.position == o.position && u.size == o.size && u.url == o.url
    })
    if (existingIndex >= 0) {
        downloadStack.splice(existingIndex, 1)
    }
    downloadStack.unshift(o)
}

function popFromDownloadStack() {
    let prioIndex = downloadStack.findIndex(e => e.prioritary)
    if (prioIndex >= 0) {
        let result = downloadStack[prioIndex]
        downloadStack.splice(prioIndex, 1)
        return result
    } else {
        return downloadStack.shift()
    }
}

var filesToCache = [
    '/book.css',
    '/book.js',
    '/bookNode.js',
    '/comic.css',
    '/comic.js',
    '/favicon.ico',
    '/fonts.css',
    '/form.css',
    '/gestures.js',
    '/gold_logo.png',
    '/library.css',
    '/library.js',
    '/login.css',
    '/serviceworker.js',
    '/settings.js',
    '/tools.css',
    '/util.js',
    '/Merriweather/Merriweather-Black.ttf',
    '/Merriweather/Merriweather-BlackItalic.ttf',
    '/Merriweather/Merriweather-Bold.ttf',
    '/Merriweather/Merriweather-BoldItalic.ttf',
    '/Merriweather/Merriweather-Italic.ttf',
    '/Merriweather/Merriweather-Light.ttf',
    '/Merriweather/Merriweather-LightItalic.ttf',
    '/Merriweather/Merriweather-Regular.ttf',
    '/Roboto/Roboto-Black.ttf',
    '/Roboto/Roboto-BlackItalic.ttf',
    '/Roboto/Roboto-Bold.ttf',
    '/Roboto/Roboto-BoldItalic.ttf',
    '/Roboto/Roboto-Italic.ttf',
    '/Roboto/Roboto-Light.ttf',
    '/Roboto/Roboto-LightItalic.ttf',
    '/Roboto/Roboto-Medium.ttf',
    '/Roboto/Roboto-MediumItalic.ttf',
    '/Roboto/Roboto-Regular.ttf',
    '/Roboto/Roboto-Thin.ttf',
    '/Roboto/Roboto-ThinItalic.ttf',
    '/shortcut.png',
    '/shortcut16.png',
    '/shortcut24.png',
    '/shortcut32.png',
    '/shortcut48.png',
    '/shortcut64.png',
    '/shortcut152.png',
    '/shortcut167.png',
    '/shortcut180.png',
    '/shortcut192.png',
    '/manifest.json'
]

self.addEventListener('install', e => {
    e.waitUntil(initCache())
    e.waitUntil(
        caches.keys().then(function(cacheNames) {
            return Promise.all(
                cacheNames.filter(function(cacheName) {
                    return cacheName != CACHE_NAME
                }).map(function(cacheName) {
                    return caches.delete(cacheName)
                })
            )
        })
    )
})

function initCache() {
    return caches.open(CACHE_NAME).then(cache => {
        cache.addAll(filesToCache)
    })
}

self.addEventListener('activate', e => {
    self.clients.claim()
})

function sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

async function singleFunctionRunning() {
    let existingWorker = await databaseFindFirst(() => true, WORKER_TABLE)
    let now = new Date()
    if (existingWorker) {
        // check if stale
        let timeDifference = Math.abs(existingWorker.date.getTime() - now.getTime())
        if (timeDifference < 60 * 1000) {
            return
        }
    }

    let methodId = now.getTime()
    await databaseDeleteAll(WORKER_TABLE)
    await databaseSave(WORKER_TABLE, {'id': methodId})
    let running = true
    while (running) {
        running = await downloadFromStack()
        await databaseSave(WORKER_TABLE, {'id': methodId})
    }
    await databaseDeleteAll(WORKER_TABLE)
}

self.addEventListener('fetch', e => {
    singleFunctionRunning()
    var url = new URL(e.request.url)

    if (url.pathname === '/markProgress') {
        e.respondWith(handleMarkProgress(e.request))
    } else if (url.pathname === '/loadProgress') {
        e.respondWith(handleLoadProgress(e.request))
    } else if (url.pathname === '/latestRead') {
        e.respondWith(handleLatestReadRequest(e.request))
    } else if (url.pathname === '/latestAdded') {
        e.respondWith(handleLatestAddedRequest(e.request))
    } else if (url.pathname === '/imageData' || url.pathname === '/comic' || url.pathname === '/bookResource' || url.pathname === '/book') {
        e.respondWith(handleDataRequest(e.request))
    } else if (url.pathname === '/bookSection') {
        e.respondWith(handleBookSectionRequest(e.request))
    } else if (url.pathname === '/') {
        e.respondWith(handleRootRequest(e.request))
    } else if (url.pathname === '/search') {
        e.respondWith(handleSearchRequest(e.request))
    } else if ((url.pathname === '/login' && e.request.method == 'POST') || (url.pathname === '/logout')) {
        e.respondWith(handleLoginLogout(e.request))
    } else if (filesToCache.includes(url.pathname)) {
        e.respondWith(handleWebResourceRequest(e.request))
    } else {
        e.respondWith(fetch(e.request))
    }
})

self.addEventListener('message', event => {
    if (event.data.type === 'storeBook') {
        var id = parseInt(event.data.bookId)
        var size = parseInt(event.data.maxPositions)
        triggerStoreBook(id, event.data.kind, size)
    } else if (event.data.type === 'deleteBook') {
        deleteBookFromDatabase(event.data.bookId)
    } else if (event.data.type === 'reset') {
        resetApplication()
    }
})

async function triggerStoreBook(id, kind, size) {
    let storedBook = await databaseLoad(BOOKS_TABLE, id)
    if (! storedBook) {
        pushToDownloadStack({
            'kind': kind,
            'id': id,
            'size': size,
            'prioritary': true
        })
    }
}

async function resetApplication() {
    // delete all data from cache
    await caches.delete(CACHE_NAME)

    // delete all data from database
    await databaseDeleteAll(REQUESTS_TABLE)
    await databaseDeleteAll(BOOKS_TABLE)
    await databaseDeleteAll(PROGRESS_TABLE)
    await databaseDeleteAll(WORKER_TABLE)

    // unregister service worker
    await self.registration.unregister()
}

async function handleLoginLogout(request) {
    let serverResponse
    try {
        serverResponse = await fetch(request)
        await resetApplication()
    } catch (error) {
        serverResponse = undefined
    }
    return serverResponse
}

async function updateResourceInCache(request) {
    // then try to get from server
    let serverResponse
    try {
        serverResponse = await fetch(request)
    } catch (error) {
        serverResponse = undefined
    }

    if (serverResponse) {
        let cache = await caches.open(CACHE_NAME)
        cache.put(request, serverResponse.clone())
        return serverResponse
    } else {
        let notFoundResponse = new Response()
        notFoundResponse.status = 404
        return notFoundResponse
    }
}

async function handleWebResourceRequest(request) {
    // first try to get from cache
    let cacheResponse = await caches.match(request)

    if (cacheResponse) {
        // always update resource in cache asynchronously
        updateResourceInCache(request)
        return cacheResponse
    } else {
        let serverResponse = await updateResourceInCache(request)
        return serverResponse
    }
}

async function handleLatestAddedRequest(request) {
    let serverResponse
    try {
        serverResponse = await fetchWithTimeout(request, SERVER_REQUEST_TIMEOUT)
    } catch (error) {
        serverResponse = undefined
    }

    if (serverResponse) {
        let text = await serverResponse.text()
        let json = JSON.parse(text)
        // find out what has been completely downloaded
        let completelyDownloadedBooks = await databaseLoadDistinct(BOOKS_TABLE, "id")
        let decoratedBooks = json.map(book => {
            if (completelyDownloadedBooks.has(book.id)) {
                book["downloaded"] = true
            } else {
                book["downloaded"] = false
            }
            return book
        })
        let responseText = JSON.stringify(decoratedBooks)

        return new Response(responseText, {headers: new Headers(serverResponse.headers)})
    } else {
        return new Response('{"offline": true}')
    }
}

async function handleSearchRequest(request) {
    let serverResponse
    try {
        serverResponse = await fetchWithTimeout(request, SERVER_REQUEST_TIMEOUT)
    } catch (error) {
        serverResponse = undefined
    }

    if (serverResponse) {
        let text = await serverResponse.text()
        let json = JSON.parse(text)
        // find out what has been completely downloaded
        let completelyDownloadedBooks = await databaseLoadDistinct(BOOKS_TABLE, "id")
        let decoratedBooks = json.books.map(book => {
            if (completelyDownloadedBooks.has(book.id)) {
                book["downloaded"] = true
            } else {
                book["downloaded"] = false
            }
            return book
        })
        json.books = decoratedBooks
        let responseText = JSON.stringify(json)

        return new Response(responseText, {headers: new Headers(serverResponse.headers)})
    } else {
        return new Response('{"offline": true}')
    }
}

async function handleRootRequest(request) {
    let serverResponse
    try {
        serverResponse = await fetchWithTimeout(request, SERVER_REQUEST_TIMEOUT)
    } catch (error) {
        serverResponse = undefined
    }

    if (serverResponse) {
        if (serverResponse.status == 200 && !serverResponse.redirected) {
            saveActualResponseToDatabase(serverResponse.clone())
        }
        return serverResponse
    } else {
        let databaseResponse = await databaseLoad(REQUESTS_TABLE, request.url)
        return databaseEntityToResponse(databaseResponse)
    }
}

async function handleDataRequest(request) {
    // always try to load from db first
    let databaseResponse = await databaseLoad(REQUESTS_TABLE, request.url)

    if (databaseResponse) {
        return databaseEntityToResponse(databaseResponse)
    } else {
        return fetch(request)
    }
}

async function handleBookSectionRequest(request) {
    let url = new URL(request.url)
    let id = parseInt(url.searchParams.get("id"))
    let position = parseInt(url.searchParams.get("position"))

    const matchFunction = value => {
        let sectionStart = value.headers["sectionstart"]
        let sectionEnd = value.headers["sectionend"]
        return id == value.id && sectionStart && sectionEnd && parseInt(sectionStart) <= position && position <= parseInt(sectionEnd)
    }
    let databaseResponse = await databaseFindFirst(matchFunction, REQUESTS_TABLE)

    if (databaseResponse) {
        return databaseEntityToResponse(databaseResponse)
    } else {
        return fetch(request)
    }
}

async function handleLoadProgress(request) {
    await syncProgressInDatabase()

    let url = new URL(request.url)
    let id = parseInt(url.searchParams.get("id"))

    // always get progress from backend - otherwise this doesn't sync well with the server
    let serverProgress
    try {
        serverProgress = await fetchWithTimeout(request, SERVER_REQUEST_TIMEOUT)
    } catch (error) {
        serverProgress = undefined
    }

    // if nothing, try to grab from database
    if (serverProgress) {
        return serverProgress
    } else {
        let databaseProgress = await databaseLoad(PROGRESS_TABLE, id)
        let databaseResponse = new Response(databaseProgress.position, {headers: {'Content-Type': 'application/json'}})
        return databaseResponse
    }
}

async function handleMarkProgress(request) {
    let url = new URL(request.url)
    let id = parseInt(url.searchParams.get("id"))
    let position = parseInt(url.searchParams.get("position"))
    let dbProgress = await databaseSave(PROGRESS_TABLE, {id: id, position: position, synced: true})

    let savedBook = await databaseLoad(BOOKS_TABLE, id)
    let bookOnDevice = savedBook != null && savedBook != undefined

    let markProgressResponse
    try {
        markProgressResponse = await fetchWithTimeout(request, SERVER_REQUEST_TIMEOUT)
    } catch (error) {
        markProgressResponse = undefined
    }

    if (markProgressResponse) {
        // we have internet connection
        let syncedProgressCount = await syncProgressInDatabase()
    } else {
        // fetch from db and mark progress as unsynced
        let unsyncedDbProgress = await databaseSave(PROGRESS_TABLE, {id: id, position: position, synced: false})
        markProgressResponse = new Response()
    }

    let blob = await markProgressResponse.blob()
    var newHeaders = new Headers();
    for (var kv of markProgressResponse.headers.entries()) {
        newHeaders.append(kv[0], kv[1]);
    }
    newHeaders.append('ondevice', bookOnDevice)

    let annotatedResponse = new Response(blob, {
        status: markProgressResponse.status,
        statusText: markProgressResponse.statusText,
        headers: newHeaders
    })

    return annotatedResponse
}

function fetchWithTimeout(request, timeoutMillis) {
    let didTimeOut = false
    return new Promise(function(resolve, reject) {
       const timeout = setTimeout(function() {
           didTimeOut = true
           reject(new Error('request timed out'))
       }, timeoutMillis)

       fetch(request)
       .then(function(response) {
           // Clear the timeout as cleanup
           clearTimeout(timeout)
           if (! didTimeOut) {
               resolve(response)
           }
       })
       .catch(function(err) {
           // Rejection already happened with setTimeout
           if (didTimeOut) return
           // Reject with error
           reject(err)
       })
   })
}

async function handleLatestReadRequest(request) {
    let serverResponse
    try {
        serverResponse = await fetchWithTimeout(request, SERVER_REQUEST_TIMEOUT)
    } catch(e) {
        serverResponse = undefined
    }
    if (serverResponse) {
        let syncedProgressCount = await syncProgressInDatabase()
        if (syncedProgressCount > 0) {
            try {
                serverResponse = await fetch(request)
            } catch(e) {
                serverResponse = undefined
            }
        }
    }

    if (serverResponse) {
        let blob = await serverResponse.blob()
        // cache response
        let savedEntity = await databaseSave(REQUESTS_TABLE, {
            url: request.url,
            response: blob,
            headers: Object.fromEntries(serverResponse.headers.entries())
        })

        let text = await blob.text()
        let booksToKeep = new Set()
        let json
        if (text && text.length > 0) {
            json = JSON.parse(text)
            // trigger download of next book
            await queueNextDownload()

            // save progress
            for (let i = 0; i < json.length; i++) {
                let book = json[i]
                await databaseSave(PROGRESS_TABLE, {id: book.id, position: book.progress, synced: true})
            }
            singleFunctionRunning()
            booksToKeep = new Set(json.map(e => e.id))
        }

        // find out what we need to delete
        let booksInDatabase = await databaseLoadDistinct(REQUESTS_TABLE, ID_INDEX)
        let booksToDelete = [...booksInDatabase].filter(id => id && !booksToKeep.has(id))
        booksToDelete.forEach(id => deleteBookFromDatabase(id))

        // mark completely downloaded books in response
        let responseText = ""
        if (json) {
            let completelyDownloadedBooks = await databaseLoadDistinct(BOOKS_TABLE, "id")
            let responseJson = json.map(book => {
                if (completelyDownloadedBooks.has(book.id)) {
                    book["downloaded"] = true
                } else {
                    book["downloaded"] = false
                }
                return book
            })

            responseText = JSON.stringify(responseJson)
        }
        return new Response(responseText, {headers: new Headers(savedEntity.headers)})
    } else {
        let databaseResponse = await databaseLoad(REQUESTS_TABLE, request.url)
        let responseText = await databaseResponse.response.text()
        let responseJson = JSON.parse(responseText)
        let completelyDownloadedBooks = await databaseLoadDistinct(BOOKS_TABLE, "id")
        let booksOnDevice = []
        for (var i = 0; i < responseJson.length; i++) {
            let book = responseJson[i]
            if (completelyDownloadedBooks.has(book.id)) {
                let latestProgress = await databaseLoad(PROGRESS_TABLE, book.id)
                book.progress = latestProgress.position
                booksOnDevice.push(book)
            }
        }
        let newResponseText = JSON.stringify(booksOnDevice)

        return new Response(newResponseText, {headers: new Headers(databaseResponse.headers)})
    }
}

function databaseEntityToResponse(entity) {
    if (entity) {
        return new Response(entity.response, {headers: new Headers(entity.headers)})
    } else {
        return undefined
    }
}

async function deleteBookFromDatabase(bookId) {
    let deleted = 0
    deleted += await databaseDelete(() => true, REQUESTS_TABLE, ID_INDEX, bookId)
    deleted += await databaseDelete(progress => progress.id == bookId, PROGRESS_TABLE)
    deleted += await databaseDelete(book => book.id == bookId, BOOKS_TABLE)
    return deleted
}

function syncProgressInDatabase() {
    return new Promise((resolve, reject) => {
        getUnsyncedProgress().then(unsyncedProgress => {
            Promise.all(unsyncedProgress.map(p => new Promise((resolve, reject) => {
                fetch('/markProgress?id=' + p.id + '&position=' + p.position, {'method': 'PUT'}).then(() => {
                    databaseSave(PROGRESS_TABLE, {id: p.id, position: p.position, synced: true})
                        .then(() => {
                            resolve()
                        })
                })
            })))
            .then(resolve(unsyncedProgress.length))
        })
    })
}

function getUnsyncedProgress() {
    return new Promise((resolve, reject) => {
        getDb().then(db => {
            var transaction = db.transaction(PROGRESS_TABLE, "readwrite")
            var objectStore = transaction.objectStore(PROGRESS_TABLE)
            var unsyncedProgress = []
            objectStore.openCursor().onsuccess = event => {
                var cursor = event.target.result
                if (cursor) {
                    if (cursor.value.synced == false) {
                        unsyncedProgress.push(cursor.value)
                    }
                    cursor.continue()
                } else {
                    resolve(unsyncedProgress)
                }
            }
        })
    })
}

function saveActualResponseToDatabase(response) {
    return new Promise((resolve, reject) => {
        var url = new URL(response.url)
        var key = response.url
        var bookId = parseInt(url.searchParams.get("id"))
        var headers = Object.fromEntries(response.headers.entries())
        response.blob().then(responseBlob => {
            var entry = {
                url: key,
                response: responseBlob,
                headers: Object.fromEntries(response.headers.entries()),
                id: bookId
            }
            databaseSave(REQUESTS_TABLE, entry)
                .then(() => resolve(entry))
        })
    })
}

function saveResponseToDatabase(url, bookId) {
    return new Promise((resolve, reject) => {
        fetch(url).then(response => saveActualResponseToDatabase(response)).then(entity => resolve(entity))
    })
}

/////////////////////////////////////////////////////////////////////////////////////////////////////// saving to device



async function downloadFromStack() {
    let o = popFromDownloadStack()
    if (o) {
        try {
            if (o.kind === 'book') await downloadBook(o)
            else if (o.kind === 'bookResource') await downloadBookResource(o)
            else if (o.kind === 'bookSection') await downloadBookSection(o)
            else if (o.kind === 'comic') await downloadComic(o)
            else if (o.kind === 'imageData') await downloadImageData(o)
        } catch (error) {
            if (o.kind === 'book' || o.kind === 'bookSection' || o.kind === 'comic' || o.kind === 'imageData') {
                pushToDownloadStack(o)
                return false
            } else {
                // just ignore issue and continue
                return true
            }

        }
    }
    return o
}

async function downloadBook(o) {
    if (! o.kind === 'book') return

    let url = self.registration.scope + 'book?id=' + o.id
    let entity = await databaseLoad(REQUESTS_TABLE, url)
    if (! entity) {
        let response = await fetch(url)
        let savedResponse = await saveActualResponseToDatabase(response)
    }

    pushToDownloadStack({
        'kind': 'bookSection',
        'id': o.id,
        'size': o.size,
        'position': 0
    })
}

async function downloadBookSection(o) {
    if (! o.kind === 'bookSection') return

    let url = self.registration.scope + 'bookSection?id=' + o.id + "&position=" + o.position
    let entity = await databaseLoad(REQUESTS_TABLE, url)
    if (! entity) {
        let response = await fetch(url)
        entity = await saveActualResponseToDatabase(response)
    }

    // add next section, if exists
    let nextPosition = parseInt(entity.headers['sectionend']) + 1
    if (nextPosition < o.size) {
        pushToDownloadStack({
            'kind': 'bookSection',
            'id': o.id,
            'size': o.size,
            'position': nextPosition
        })
    }

    // add resources download, if they exist
    let text = await entity.response.text()
    let structure = JSON.parse(text)
    let node = convert(structure)
    let resources = node.getResources()
    for (let i = 0; i < resources.length; i++) {
        let resource = resources[i]
        pushToDownloadStack({
            'kind': 'bookResource',
            'url': resource
        })
    }

    if (nextPosition >= o.size) {
        let savedBook = await databaseSave(BOOKS_TABLE, {'id': o.id})
        await queueNextDownload()
    }
}

async function downloadBookResource(o) {
    if (! o.kind === 'bookResource') return

    let resourceEntity = await databaseLoad(REQUESTS_TABLE, o.url)
    if (! resourceEntity) {
        let resourceResponse = await fetch(o.url)
        await saveActualResponseToDatabase(resourceResponse)
    }
}

async function downloadComic(o) {
    if (! o.kind === 'comic')  return

    let url = self.registration.scope + 'comic?id=' + o.id
    let entity = await databaseLoad(REQUESTS_TABLE, url)
    if (! entity) {
        let response = await fetch(url)
        let savedResponse = await saveActualResponseToDatabase(response)
    }
    pushToDownloadStack({
        'kind': 'imageData',
        'id': o.id,
        'size': o.size,
        'position': 0
    })
}

async function downloadImageData(o) {
    if (! o.kind === 'imageData') return

    let url = self.registration.scope + 'imageData?id=' + o.id + '&page=' + o.position
    let entity = await databaseLoad(REQUESTS_TABLE, url)
    if (! entity) {
        let response = await fetch(url)
        let savedResponse = await saveActualResponseToDatabase(response)
    }

    let nextPosition = o.position + 1
    if (nextPosition < o.size) {
        pushToDownloadStack({
            'kind': 'imageData',
            'id': o.id,
            'size': o.size,
            'position': nextPosition
        })
    } else {
        let savedBook = await databaseSave(BOOKS_TABLE, {'id': o.id})
        await queueNextDownload()
    }
}

async function queueNextDownload() {
    // load books in latest read
    let latestReadMatchFunction = (response) => {
        return response.url.includes(self.registration.scope + "latestRead")
    }
    let databaseResponse = await databaseFindFirst(latestReadMatchFunction, REQUESTS_TABLE)
    if (databaseResponse) {
        let responseText = await databaseResponse.response.text()
        let responseJson = JSON.parse(responseText)
        // load books table
        let completelyDownloadedBooks = await databaseLoadDistinct(BOOKS_TABLE, "id")
        // find first book id that is not in books table
        for (var i = 0; i < responseJson.length; i++) {
            let book = responseJson[i]
            if (! completelyDownloadedBooks.has(book.id)) {
                await triggerStoreBook(book.id, book.type, parseInt(book.pages))
                return
            }
        }
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////// database operations

function databaseFindFirst(matchFunction, table, indexName = undefined, indexValue = undefined) {
    return new Promise((resolve, reject) => {
        getDb().then(db => {
            let transaction = db.transaction(table)
            let objectStore = transaction.objectStore(table)
            let cursorRequest
            if (indexName) {
                let index = objectStore.index(indexName)
                cursorRequest = index.openCursor(IDBKeyRange.only(indexValue))
            } else {
                cursorRequest = objectStore.openCursor()
            }
            cursorRequest.onsuccess = event => {
                let cursor = event.target.result
                if (cursor) {
                    if (matchFunction(cursor.value)) {
                        resolve(cursor.value)
                    } else {
                        cursor.continue()
                    }
                } else {
                    resolve()
                }
            }
        })
    })
}

function databaseLoad(table, key) {
    return new Promise((resolve, reject) => {
        getDb().then(db => {
            let transaction = db.transaction([table])
            let objectStore = transaction.objectStore(table)
            let dbRequest = objectStore.get(key)
            dbRequest.onsuccess = function(event) {
                resolve(event.target.result)
            }
        })
    })
}

function databaseDeleteAll(table) {
     return new Promise((resolve, reject) => {
        getDb().then(db => {
            let transaction = db.transaction([table], "readwrite")
            let objectStore = transaction.objectStore(table)
            let deleteRequest = objectStore.clear()
            deleteRequest.onsuccess = event => {
                resolve()
            }
        })
    })
}

function databaseDelete(matchFunction, table, indexName = undefined, indexValue = undefined) {
    return new Promise((resolve, reject) => {
        getDb().then(db => {
            let transaction = db.transaction([table], "readwrite")
            let objectStore = transaction.objectStore(table)

            let cursorRequest
            if (indexName) {
                let index = objectStore.index(indexName)
                cursorRequest = index.openCursor(IDBKeyRange.only(indexValue))
            } else {
                cursorRequest = objectStore.openCursor()
            }

            let deletedCount = 0
            cursorRequest.onsuccess = event => {
                let cursor = event.target.result
                if (cursor) {
                    if (matchFunction(cursor.value)) {
                        objectStore.delete(cursor.primaryKey)
                        deletedCount += 1
                    }
                    cursor.continue()
                } else {
                    resolve(deletedCount)
                }
            }
        })
    })
}

function databaseSave(table, value) {
    return new Promise((resolve, reject) => {
        getDb().then(db => {
            let transaction = db.transaction([table], "readwrite")
            transaction.oncomplete = function(event) {
                resolve(value)
            }
            let objectStore = transaction.objectStore(table)
            value['date'] = new Date()
            let addRequest = objectStore.put(value)
        })
    })
}

function databaseLoadDistinct(table, column) {
    return new Promise((resolve, reject) => {
        getDb().then(db => {
            let transaction = db.transaction([table])
            let objectStore = transaction.objectStore(table)
            let cursorRequest = objectStore.openCursor()
            let distinctValues = new Set()
            cursorRequest.onsuccess = event => {
                let cursor = event.target.result
                if (cursor) {
                    distinctValues.add(cursor.value[column])
                    cursor.continue()
                } else {
                    resolve(distinctValues)
                }
            }
            cursorRequest.onerror = event => reject()
        })
    })
}