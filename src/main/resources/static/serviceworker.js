importScripts('bookNode.js')

var CACHE_NAME = 'chronic-reader-cache-1'
var DATABASE_NAME = 'chronic-reader-db'
var DATABASE_VERSION = 1
var REQUESTS_TABLE = 'requests'
var PROGRESS_TABLE = 'progress'
var BOOKS_TABLE = 'books'
var WORKER_TABLE = 'worker'
var ID_INDEX = 'id'

var downloadingBook = new Set()

var db

function getDb() {
    return new Promise((resolve, reject) => {
        if (! db) {
            console.log("initializing db")
            const request = indexedDB.open(DATABASE_NAME, DATABASE_VERSION)
            request.onerror = function(event) {
                console.log(event)
                reject()
            }
            request.onsuccess = function(event) {
                db = event.target.result
                resolve(event.target.result)
            }
            request.onupgradeneeded = function(event) {
                console.log("upgrade db")
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

getDb().then(db => console.log("initialized database"))

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
    '/hammer.min.js',
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
    '/Roboto/Roboto-ThinItalic.ttf'
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
            console.log("single function already running")
            return
        }
    }

    let methodId = new Date().getTime()
    console.log("starting single function " + methodId)
    await databaseDeleteAll(WORKER_TABLE)
    await databaseSave(WORKER_TABLE, {'id': methodId})
    while (true) {
        console.log("single function " + methodId)
        await databaseSave(WORKER_TABLE, {'id': methodId})
        await sleep(1000)
    }
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
    } else if (url.pathname === '/imageData' || url.pathname === '/comic' || url.pathname === '/bookResource' || url.pathname === '/book') {
        e.respondWith(handleDataRequest(e.request))
    } else if (url.pathname === '/bookSection') {
        e.respondWith(handleBookSectionRequest(e.request))
    } else if (url.pathname === '/') {
        e.respondWith(handleRootRequest(e.request))
    } else if (url.pathname === '/search') {
        e.respondWith(handleSearchRequest(e.request))
    } else if (filesToCache.includes(url.pathname)) {
        e.respondWith(handleWebResourceRequest(e.request))
    } else {
        e.respondWith(fetch(e.request))
    }
})

self.addEventListener('message', event => {
    if (event.data.type === 'storeBook') {
        var bookId = parseInt(event.data.bookId)
        if (! downloadingBook.has(bookId)) {
            var maxPositions = event.data.maxPositions
            var type = event.data.kind
            saveToDevice(bookId, type, maxPositions)
        }
    } else if (event.data.type === 'deleteBook') {
        deleteBookFromDatabase(event.data.bookId)
    } else if (event.data.type === 'reset') {
        resetApplication()
    }
})

async function resetApplication() {
    // delete all data from cache
    await caches.delete(CACHE_NAME)

    // delete all data from database
    await databaseDeleteAll(REQUESTS_TABLE)
    await databaseDeleteAll(BOOKS_TABLE)
    await databaseDeleteAll(PROGRESS_TABLE)
    await databaseDeleteAll(WORKER_TABLE)

    // unregister service worker
    self.registration.unregister()
}

async function handleWebResourceRequest(request) {
    // first try to get from cache
    let cacheResponse = await caches.match(request)

    if (cacheResponse) {
        return cacheResponse
    }

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

async function handleSearchRequest(request) {
    let serverResponse
    try {
        serverResponse = await fetch(request)
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
        serverResponse = await fetch(request)
    } catch (error) {
        serverResponse = undefined
    }

    if (serverResponse) {
        if (serverResponse.status == 200 && !serverResponse.redirected) {
            saveActualResponseToDatabase(serverResponse.clone())
        }
        return serverResponse
    } else {
        let databaseResponse = await databaseLoad(REQUESTS_TABLE, '/')
        return databaseEntityToResponse(databaseResponse)
    }
}

async function handleDataRequest(request) {
    let url = new URL(request.url)
    let key = url.pathname + url.search
    // always try to load from db first
    let databaseResponse = await databaseLoad(REQUESTS_TABLE, key)

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
    let url = new URL(request.url)
    let id = parseInt(url.searchParams.get("id"))

    // always get progress from database
    let databaseProgress = await databaseLoad(PROGRESS_TABLE, id)

    // if nothing, try to grab from backend
    if (databaseProgress) {
        return new Response(databaseProgress.position)
    } else {
        let progressResponse = await fetch(request)
        return progressResponse
    }
}

async function handleMarkProgress(request) {
    let url = new URL(request.url)
    let id = parseInt(url.searchParams.get("id"))
    let position = parseInt(url.searchParams.get("position"))
    let dbProgress = await databaseSave(PROGRESS_TABLE, {id: id, position: position, synced: true})

    let markProgressResponse
    try {
        markProgressResponse = await fetch(request)
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

    return markProgressResponse
}

async function handleLatestReadRequest(request) {
    let serverResponse
    try {
        serverResponse = await fetch(request)
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
            url: '/latestRead',
            response: blob,
            headers: Object.fromEntries(serverResponse.headers.entries())
        })
        downloadNext()

        let text = await blob.text()
        let json = JSON.parse(text)

        // trigger download of everything in latest read, including progress
        json.forEach(book => {
            //saveToDevice(book.id, book.type, book.pages)
            databaseSave(PROGRESS_TABLE, {id: book.id, position: book.progress, synced: true})
        })

        // find out what we need to delete
        let booksToKeep = new Set(json.map(e => e.id))
        let booksInDatabase = await databaseLoadDistinct(REQUESTS_TABLE, ID_INDEX)
        let booksToDelete = [...booksInDatabase].filter(id => id && !booksToKeep.has(id))
        booksToDelete.forEach(id => deleteBookFromDatabase(id))

        // mark completely downloaded books in response
        let completelyDownloadedBooks = await databaseLoadDistinct(BOOKS_TABLE, "id")
        let responseJson = json.map(book => {
            if (completelyDownloadedBooks.has(book.id)) {
                book["downloaded"] = true
            } else {
                book["downloaded"] = false
            }
            return book
        })
        let responseText = JSON.stringify(responseJson)
        return new Response(responseText, {headers: new Headers(savedEntity.headers)})
    } else {
        let databaseResponse = await databaseLoad(REQUESTS_TABLE, '/latestRead')
        let responseText = await databaseResponse.response.text()
        let responseJson = JSON.parse(responseText)
        for (var i = 0; i < responseJson.length; i++) {
            let book = responseJson[i]
            let latestProgress = await databaseLoad(PROGRESS_TABLE, book.id)
            book.progress = latestProgress.position
        }
        let newResponseText = JSON.stringify(responseJson)

        return new Response(newResponseText, {headers: new Headers(databaseResponse.headers)})
    }
}



async function downloadNext() {
    //todo: clear downloading book that is older than some date
    return
    if (downloadingBook.size == 0) {
        let databaseResponse = await databaseLoad(REQUESTS_TABLE, '/latestRead')
        if (databaseResponse) {
            let responseText = await databaseResponse.response.text()
            let latestReadBooks = JSON.parse(responseText)
            let booksInDatabase = await databaseLoadDistinct(REQUESTS_TABLE, ID_INDEX)
            let nextToDownload = latestReadBooks.find(book => ! booksInDatabase.has(book.id) && ! downloadingBook.has(book.id))
            if (nextToDownload) {
                console.log("next to download is: ")
                console.log(nextToDownload)
                downloadingBook.add(nextToDownload.id)
                saveToDevice(nextToDownload.id, nextToDownload.type, nextToDownload.pages)
            } else {
                console.log("nothing left to download")
            }
        } else {
            console.log("latest read not in database")
        }
    } else {
        console.log("already downloading something")
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
}

function saveActualResponseToDatabase(response) {
    return new Promise((resolve, reject) => {
        var url = new URL(response.url)
        var key = url.pathname + url.search
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

async function saveToDevice(bookId, type, maxPositions) {
    console.log("save " + type + " " + bookId + " to device")
    let entity = await databaseLoad(BOOKS_TABLE, bookId)
    if (! entity) {
        if (type === 'comic') {
            saveComicToDevice(bookId, maxPositions)
        } else if (type === 'book') {
            saveBookToDevice(bookId, maxPositions)
        }
    } else {
        console.log(type + " " + bookId + " already on device")
        downloadNext()
    }
}

async function saveBookToDevice(id, size) {
    console.log("downloading books")
    console.log(downloadingBook)
    {
        let url = '/book?id=' + id
        let entity = await databaseLoad(REQUESTS_TABLE, url)
        if (! entity) {
            console.log("saving " + url)
            let response = await fetch(url)
            let savedResponse = await saveActualResponseToDatabase(response)
        } else {
            console.log(url + " already saved")
        }
    }
    let position = 0
    while (position < size) {
        let url = '/bookSection?id=' + id + "&position=" + position
        let entity = await databaseLoad(REQUESTS_TABLE, url)
        if (entity) {
            console.log(url + " already saved")
        } else {
            console.log("saving " + url)
            let response = await fetch(url)
            entity = await saveActualResponseToDatabase(response)
        }

        // save section resources
        let text = await entity.response.text()
        let structure = JSON.parse(text)
        let node = convert(structure)
        let resources = node.getResources()
        for (let i = 0; i < resources.length; i++) {
            let resource = resources[i]
            let resourceUrl = '/' + resource
            let resourceEntity = await databaseLoad(REQUESTS_TABLE, resourceUrl)
            if (! resourceEntity) {
                console.log("saving " + resourceUrl)
                let resourceResponse = await fetch(resourceUrl)
                await saveActualResponseToDatabase(resourceResponse)
            } else {
                console.log(resourceUrl + " already saved")
            }
        }

        let nextPosition = parseInt(entity.headers['sectionend']) + 1
        position = nextPosition
    }

    let savedBook = await databaseSave(BOOKS_TABLE, {'id': id})
    console.log("finished saving book " + id)
    downloadingBook.delete(id)
    downloadNext()
}

async function saveComicToDevice(id, size) {
    console.log("downloading books")
    console.log(downloadingBook)
    {
        let url = '/comic?id=' + id
        let entity = await databaseLoad(REQUESTS_TABLE, url)
        if (! entity) {
            console.log("saving " + url)
            let response = await fetch(url)
            let savedResponse = await saveActualResponseToDatabase(response)
        } else {
            console.log(url + " already saved")
        }
    }
    let position = 0
    while (position < size) {
        let url = '/imageData?id=' + id + '&page=' + position
        let entity = await databaseLoad(REQUESTS_TABLE, url)
        if (entity) {
            console.log(url + " already on device")
        } else {
            console.log("saving " + url)
            let response = await fetch(url)
            let savedResponse = await saveActualResponseToDatabase(response)
        }
        position += 1
    }

    let savedBook = await databaseSave(BOOKS_TABLE, {'id': id})
    console.log("finished saving comic " + id)
    downloadingBook.delete(id)
    downloadNext()
}

/*async function saveBookSectionToDevice(id, size, position) {
    if (position < size) {
        let url = '/bookSection?id=' + id + "&position=" + position
        let entity = await databaseLoad(REQUESTS_TABLE, url)
        if (entity) {
            console.log(url + " already saved")
        } else {
            console.log("saving " + url)
            let response = await fetch(url)
            entity = await saveActualResponseToDatabase(response)
            saveBookResourcesToDevice(entity)
        }
        let nextPosition = parseInt(entity.headers['sectionend']) + 1
        saveBookSectionToDevice(id, size, nextPosition)
    } else {
        let savedBook = await databaseSave(BOOKS_TABLE, {
            'id': id,
            'date': new Date()
        })
        console.log("finished saving book " + id)
        downloadNext()
    }
}*/

/*function saveBookResourcesToDevice(section) {
    section.response.text().then(text => {
        var structure = JSON.parse(text)
        var node = convert(structure)
        var resources = node.getResources()
        resources
            .filter(resource => resource.startsWith('bookResource'))
            .forEach(resource => {
                let url = '/' + resource
                console.log("saving " + url)
                fetch(url).then(response => saveActualResponseToDatabase(response))
            })
    })
}*/

/*async function saveComicPageToDevice(id, size, position) {
    if (position < size) {
        var url = '/imageData?id=' + id + '&page=' + position
        let entity = await databaseLoad(REQUESTS_TABLE, url)
        if (entity) {
            console.log(url + " already on device")
        } else {
            console.log("saving " + url)
            let response = await fetch(url)
            let savedResponse = await saveActualResponseToDatabase(response)
        }
        saveComicPageToDevice(id, size, position + 1)
    } else {
        let savedBook = await databaseSave(BOOKS_TABLE, {'id': id})
        console.log("finished saving comic " + id)
        downloadingBook.delete(id)
        downloadNext()
    }
}*/

///////////////////////////////////////////////////////////////////////////////////////////////// new download framework

/*
- need a queue for downloads, to make sure a single resource is downloaded at once
- as a resource is processed, a new download request may be added to the queue
*/




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