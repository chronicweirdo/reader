importScripts('bookNode.js')

var CACHE_NAME = 'chronic-reader-cache-1'
var DATABASE_NAME = 'chronic-reader-db'
var DATABASE_VERSION = 1
var REQUESTS_TABLE = 'requests'
var PROGRESS_TABLE = 'progress'
var BOOKS_TABLE = 'books'
var ID_INDEX = 'id'

const request = indexedDB.open(DATABASE_NAME, DATABASE_VERSION)
var db
request.onerror = function(event) {
    console.log(event)
}
request.onsuccess = function(event) {
    db = event.target.result
}

request.onupgradeneeded = function(event) {
    var db = event.target.result

    var requestsStore = db.createObjectStore(REQUESTS_TABLE, {keyPath: 'url'})
    requestsStore.createIndex(ID_INDEX, ID_INDEX, { unique: false })

    var progressStore = db.createObjectStore(PROGRESS_TABLE, {keyPath: 'id'})

    var booksStore = db.createObjectStore(BOOKS_TABLE, {keyPath: 'id'})
}

var filesToCache = [
    '/form.css',
    '/favicon.ico',
    '/library.css',
    '/library.js',
    '/book.css',
    '/tools.css',
    '/hammer.min.js',
    '/gestures.js',
    '/util.js',
    '/bookNode.js',
    '/book.js',
    '/comic.css',
    '/comic.js',
    '/settings.js',
    '/fonts.css',
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
        console.log("adding all to cache")
        cache.addAll(filesToCache)
    })
}

self.addEventListener('activate', e => {
    self.clients.claim()
})

self.addEventListener('fetch', e => {
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
        var maxPositions = event.data.maxPositions
        var type = event.data.kind
        saveToDevice(bookId, type, maxPositions)
    } else if (event.data.type === 'deleteBook') {
        deleteBookFromDatabase(event.data.bookId)
    } else if (event.data.type === 'reset') {
        resetApplication()
    }
})

async function resetApplication() {
    // delete all data from cache
    await caches.delete(CACHE_NAME)
    await initCache()

    // delete all data from database
    await databaseDeleteAll(REQUESTS_TABLE)
    await databaseDeleteAll(BOOKS_TABLE)
    await databaseDeleteAll(PROGRESS_TABLE)
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
        let text = await blob.text()
        let json = JSON.parse(text)

        // decide what needs downloading and start downloads
        let booksToKeep = new Set(json.map(e => e.id))
        let booksInDatabase = await databaseLoadDistinct(REQUESTS_TABLE, ID_INDEX)
        let booksToDelete = [...booksInDatabase].filter(id => id && !booksToKeep.has(id))
        booksToDelete.forEach(id => deleteBookFromDatabase(id))
        let booksToDownload = json.filter(book => ! booksInDatabase.has(book.id))
        booksToDownload.forEach(book => saveToDevice(book.id, book.type, book.pages))
        json.forEach(book => databaseSave(PROGRESS_TABLE, {id: book.id, position: book.progress, synced: true}))

        // find out what has been completely downloaded
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

        //await updateLatestReadInformation(serverResponse.clone())
        //return serverResponse
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

function saveToDevice(bookId, type, maxPositions) {
    if (type === 'comic') {
        saveComicToDevice(bookId, maxPositions)
    } else if (type === 'book') {
        saveBookToDevice(bookId, maxPositions)
    }
}

function saveBookToDevice(id, size) {
    let url = '/book?id=' + id
    databaseLoad(REQUESTS_TABLE, url)
        .then(entity => {
            if (!entity) fetch(url).then(response => saveActualResponseToDatabase(response))
        })
        .finally(() => saveBookSectionToDevice(id, size, 0))
}

function saveComicToDevice(id, size) {
    let url = '/comic?id=' + id
    databaseLoad(REQUESTS_TABLE, url)
        .then(response => {
            if (!response) fetch(url).then(response => saveActualResponseToDatabase(response))
        })
        .finally(() => saveComicPageToDevice(id, size, 0))
}

function saveBookSectionToDevice(id, size, position) {
    return new Promise((resolve, reject) => {
        if (position < size) {
            let url = '/bookSection?id=' + id + "&position=" + position
            databaseLoad(REQUESTS_TABLE, url)
                .then(entity => new Promise((resolve, reject) => {
                    if (entity) {
                        resolve(entity)
                    } else {
                        fetch(url)
                            .then(response => saveActualResponseToDatabase(response))
                            .then(entity => {
                                saveBookResourcesToDevice(entity)
                                resolve(entity)
                            })
                    }
                }))
                .then(section => {
                    let nextPosition = parseInt(section.headers['sectionend']) + 1
                    saveBookSectionToDevice(id, size, nextPosition)
                    resolve()
                })
        } else {
            databaseSave(BOOKS_TABLE, {
                'id': id,
                'date': new Date()
            }).then(() => {
                resolve()
            })
        }
    })
}

function saveBookResourcesToDevice(section) {
    section.response.text().then(text => {
        var structure = JSON.parse(text)
        var node = convert(structure)
        var resources = node.getResources()
        resources
            .filter(resource => resource.startsWith('bookResource'))
            .forEach(resource => {
                let url = '/' + resource
                fetch(url).then(response => saveActualResponseToDatabase(response))
            })
    })
}

function saveComicPageToDevice(id, size, position) {
    return new Promise((resolve, reject) => {
        if (position < size) {
            var url = '/imageData?id=' + id + '&page=' + position
            databaseLoad(REQUESTS_TABLE, url)
                .then(entity => new Promise((resolve, reject) => {
                    if (entity) {
                        resolve(entity)
                    } else {
                        fetch(url)
                            .then(response => saveActualResponseToDatabase(response))
                            .then(savedResponse => resolve(savedResponse))
                    }
                }))
                .finally(() => {
                    saveComicPageToDevice(id, size, position + 1)
                    resolve()
                })
        } else {
            databaseSave(BOOKS_TABLE, {
                'id': id
            }).then(() => {
                resolve()
            })
        }
    })
}


//////////////////////////////////////////////////////////////////////////////////////////////////// database operations

function databaseFindFirst(matchFunction, table, indexName = undefined, indexValue = undefined) {
    return new Promise((resolve, reject) => {
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
}

function databaseLoad(table, key) {
    return new Promise((resolve, reject) => {
        let transaction = db.transaction([table])
        let objectStore = transaction.objectStore(table)
        let dbRequest = objectStore.get(key)
        dbRequest.onsuccess = function(event) {
            resolve(event.target.result)
        }
    })
}

function databaseDeleteAll(table) {
     return new Promise((resolve, reject) => {
        let transaction = db.transaction([table], "readwrite")
        let objectStore = transaction.objectStore(table)
        let deleteRequest = objectStore.clear()
        deleteRequest.onsuccess = event => {
            resolve()
        }
    })
}

function databaseDelete(matchFunction, table, indexName = undefined, indexValue = undefined) {
    return new Promise((resolve, reject) => {
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
}

function databaseSave(table, value) {
    return new Promise((resolve, reject) => {
        let transaction = db.transaction([table], "readwrite")
        transaction.oncomplete = function(event) {
            resolve(value)
        }
        let objectStore = transaction.objectStore(table)
        value['date'] = new Date()
        let addRequest = objectStore.put(value)
    })
}

function databaseLoadDistinct(table, column) {
    return new Promise((resolve, reject) => {
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
}