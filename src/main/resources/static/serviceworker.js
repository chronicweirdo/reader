importScripts('bookNode.js')

var workerVersion = "1"
var cacheName = 'chronic-reader-cache-' + workerVersion
var dbName = 'chronic-reader-db'
var dbVersion = 1
var REQUESTS_TABLE = 'requests'
var PROGRESS_TABLE = 'progress'

const request = indexedDB.open(dbName, dbVersion);
var db;
request.onerror = function(event) {
    console.log('error opening db')
    console.log(event)
}
request.onsuccess = function(event) {
    console.log('db opened successfully')
    console.log(event)
    db = event.target.result
}
request.onupgradeneeded = function(event) {
    var db = event.target.result
    var requestsStore = db.createObjectStore(REQUESTS_TABLE, {keyPath: 'url'})
    requestsStore.createIndex('bookId', 'bookId', { unique: false })
    requestsStore.transaction.oncomplete = function(event) {
        console.log('created requests store')
        /*var customerObjectStore = db.transaction("customers", "readwrite").objectStore("customers");
        customerData.forEach(function(customer) {
          customerObjectStore.add(customer);
        });*/
    };
    var progressStore = db.createObjectStore(PROGRESS_TABLE, {keyPath: 'id'})
    progressStore.transaction.oncomplete = function(event) {
            console.log('created progress store')
    }
}

var filesToCache = [
    'https://fonts.googleapis.com/css2?family=Merriweather:ital,wght@0,300;0,400;0,700;1,300;1,400;1,700&display=swap',
    'https://fonts.googleapis.com/css2?family=Roboto:ital,wght@0,300;0,400;0,700;1,300;1,400;1,700&display=swap',

    //'/',
    '/form.css',
    '/favicon.ico',

    '/library.css',
    '/library.js',

    //'/book',
    '/book.css',
    '/tools.css',
    '/hammer.min.js',
    '/gestures.js',
    '/util.js',
    '/bookNode.js',
    '/book.js',

    //'/comic',
    '/comic.css',
    '/comic.js',

    '/'
]

self.addEventListener('install', e => {
    console.log("service worker processing install")
    e.waitUntil(
        caches.open(cacheName).then(cache => cache.addAll(filesToCache))
        //self.skipWaiting() // TODO: does this activate the service worker immediately?
    )
})

self.addEventListener('activate', e => {
    console.log("service worker activating")
    self.clients.claim()
})

self.addEventListener('fetch', e => {
    var url = new URL(e.request.url)
    console.log("fetch url: " + url.pathname)

    if (url.pathname === '/markProgress') {
        // we always store progress to local database as well
        storeToProgressDatabase(e.request, url, true)
        e.respondWith(fetch(e.request).then(response => {
            syncProgressInDatabase()
            return response
        }).catch(() => storeToProgressDatabase(e.request, url, false)))
    } else if (url.pathname === '/loadProgress') {
        e.respondWith(fetch(e.request).then(response => {
            syncProgressInDatabase()
            return response
        }).catch(() => getProgressFromDatabase(e.request, url)))
    } else if (url.pathname === '/latestRead') {
        e.respondWith(fetch(e.request)
            .then(response => {
                updateLatestReadInformation(response.clone())
                syncProgressInDatabase()
                return response
            })
            .catch(() => fetchResponseFromDatabase('/latestRead'))
        )
    } else if (url.pathname === '/imageData' || url.pathname === '/comic' || url.pathname === '/bookResource' || url.pathname === '/book') {
        var key = url.pathname + url.search
        e.respondWith(fetchResponseFromDatabase(key).then(response => {
            if (response) {
                return response
            } else {
                console.log("failed to find in database")
                return fetch(e.request)
            }
        }))
    } else if (url.pathname === '/bookSection') {
        var id = url.searchParams.get("id")
        var position = url.searchParams.get("position")
        e.respondWith(
            translatePositionToSectionUrl(parseInt(id), position).then(sectionUrl => {
                if (sectionUrl) {
                    return fetchResponseFromDatabase(sectionUrl).then(response => {
                        if (response) {
                            console.log('found book section in database')
                            return response
                        } else {
                            console.log("failed to find in database")
                            return fetch(e.request)
                        }
                    })
                } else {
                    console.log("failed to find section in database")
                    return fetch(e.request)
                }
            })
        )
    } else if (url.pathname === '/') {
        e.respondWith(fetch(e.request).then(response => {
            console.log("response is redirected: " + response.redirected)
            if (response.status == 200 && !response.redirected) {
                saveActualResponseToDatabase(response.clone())
            }
            return response
        }).catch(() => fetchResponseFromDatabase('/')))
    } else {
        e.respondWith(fetch(e.request).catch(() => fetchFromCache(e.request, url)))
    }
})

function translatePositionToSectionUrl(bookId, position) {
    return new Promise((resolve, reject) => {
        console.log('translating book ' + bookId + ' position ' + position + ' to section url')
        var transaction = db.transaction(REQUESTS_TABLE)
        var objectStore = transaction.objectStore(REQUESTS_TABLE)
        var index = objectStore.index('bookId')
        index.openCursor(IDBKeyRange.only(bookId)).onsuccess = event => {
            var cursor = event.target.result
            if (cursor) {
                var sectionStart = cursor.value.headers["sectionstart"]
                var sectionEnd = cursor.value.headers["sectionend"]
                console.log(sectionStart)
                console.log(sectionEnd)
                if (sectionStart && sectionEnd && parseInt(sectionStart) <= position && position <= parseInt(sectionEnd)) {
                    resolve(cursor.value.url)
                } else {
                    cursor.continue()
                }
            } else {
                resolve()
            }
        }
    })
}

function loadFromDatabase(table, key) {
    return new Promise((resolve, reject) => {
        var transaction = db.transaction([table])
        var objectStore = transaction.objectStore(table)
        var dbRequest = objectStore.get(key)
        dbRequest.onerror = function(event) {
            console.log("failed to load from table " + table + " key " + key)
            reject()
        }
        dbRequest.onsuccess = function(event) {
            resolve(event.target.result)
        }
    })
}

function findDistinctValues(table, column) {
    return new Promise((resolve, reject) => {
        var transaction = db.transaction([table])
        var objectStore = transaction.objectStore(table)
        var cursorRequest = objectStore.openCursor()
        var distinctValues = new Set()
        cursorRequest.onsuccess = event => {
            //console.log('processing cursor event')
            var cursor = event.target.result
            if (cursor) {
                //console.log(cursor.value)
                distinctValues.add(cursor.value[column])
                cursor.continue()
            } else {
                //console.log('Exhausted all documents')
                //console.log(distinctValues)
                resolve(distinctValues)
            }
        }
        cursorRequest.onerror = event => reject()
    })
}

function fetchResponseFromDatabase(key) {
    return new Promise((resolve, reject) => {
        loadFromDatabase(REQUESTS_TABLE, key)
            .then(result => {
                if (result) {
                    resolve(new Response(result.response, {headers: new Headers(result.headers)}))
                } else {
                    resolve(undefined)
                }
            })
    })
}

function updateLatestReadInformation(response) {
    console.log("handling latest read")
    console.log(response)
    response.blob().then(blob => {
        var value = {
            url: '/latestRead',
            response: blob,
            headers: Object.fromEntries(response.headers.entries())
        }
        saveToDatabase(REQUESTS_TABLE, value)
        return blob.text()
    }).then(text => {
        var json = JSON.parse(text)

        var booksToKeep = new Set()
        for (var i = 0; i < json.length; i++) {
            var book = json[i]
            booksToKeep.add(book.id)
        }
        findDistinctValues(REQUESTS_TABLE, 'bookId')
            .then(booksInDatabase => {
                console.log('books to keep:')
                console.log(booksToKeep)
                console.log('books in database')
                console.log(booksInDatabase)
                //var booksToDeleteFromDb = difference(booksInDatabase, booksToKeep)
                for (let bookId of booksInDatabase) {
                    if (bookId && ! booksToKeep.has(bookId)) {
                        deleteFromDatabaseForIndex(REQUESTS_TABLE, 'bookId', bookId)
                        //self.controller.postMessage({type: 'deleteBook', bookId: bookId}) // can't do this
                        // todo: delete progress as well
                    }
                }
                //var booksToDownload = difference(booksToKeep, booksInDatabase)
                for (var i = 0; i < json.length; i++) {
                    var book = json[i]
                    if (!booksInDatabase.has(book.id)) {
                        saveToDevice(book.id, book.type, book.pages)
                    }
                    fetchAndSaveProgress(book.id)
                }
            })
    })
}

function fetchAndSaveProgress(bookId) {
    fetch('/loadProgress?id=' + bookId)
        .then(response => {
            console.log(response)
            console.log("got a response containing progress for " + bookId)
            return response.json()
        })
        .then(position => {
            console.log("retrieved position " + position + " for id " + bookId)
            saveToDatabase(PROGRESS_TABLE, {id: String(bookId), position: String(position), synced: true})
        })
}

function storeToProgressDatabase(request, url, synced) {
    return new Promise((resolve, reject) => {
        console.log("storing progress to db for later: " + request.url)
        var id = url.searchParams.get("id")
        var position = url.searchParams.get("position")
        saveToDatabase(PROGRESS_TABLE, {id: id, position: position, synced: synced})
            .then(() => resolve(new Response()))
            .catch(() => reject())
    })
}

function syncProgressInDatabase() {
    console.log("syncing progress to backend")
    getUnsyncedProgress().then(unsyncedProgress => {
        unsyncedProgress.forEach(p => {
            fetch('/markProgress?id=' + p.id + '&position=' + p.position).then(() => {
                console.log("marking progress as synced")
                console.log(p)
                saveToDatabase(PROGRESS_TABLE, {id: p.id, position: p.position, synced: true})
            })
        })
    })
}

function getUnsyncedProgress() {
    return new Promise((resolve, reject) => {
        console.log("retrieving unsynced progress")
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

function getProgressFromDatabase(request, url) {
    return new Promise((resolve, reject) => {
        var id = url.searchParams.get("id")
        loadFromDatabase(PROGRESS_TABLE, id)
            .then(result => {
                if (result) {
                    resolve(new Response(result.position))
                } else {
                    reject()
                }
            })
            .catch(() => reject())
    })
}

async function fetchFromCache(request, url) {
    var response = await caches.match(request)

    console.log("for request: " + request.url)
    if (response) {
        console.log("response in cache")
        console.log(response)
        return response
    } else {
        console.log("special offline response")
        var rObject = new Response()
        rObject.status = 404
        rObject.body = "am offline"
        return rObject
    }
}

self.addEventListener('message', event => {
    console.log("received message")
    console.log(event)
    if (event.data.type === 'storeBook') {
        console.log("received store book message")
        console.log(event)
        var bookId = parseInt(event.data.bookId)
        var maxPositions = event.data.maxPositions
        var type = event.data.kind
        saveToDevice(bookId, type, maxPositions)
    } else if (event.data.type === 'deleteBook') {
        console.log("received delete book message")
        console.log(event)
        deleteFromDatabaseForIndex(REQUESTS_TABLE, 'bookId', event.data.bookId)
    }
})

function clearFromCache(booksToKeep) {
    caches.open(cacheName).then(cache => {
        cache.keys().then(keys => {
            //var ids = new Set()
            keys.forEach(function (request, index, array) {
                var url = new URL(request.url)
                if (url.pathname === '/imageData' || url.pathname === '/comic') {
                    var id = url.searchParams.get("id")
                    if (! booksToKeep.has(id)) {
                        cache.delete(request)
                    }
                }
            })
        })
    })
}



function saveToDatabase(table, value) {
    return new Promise((resolve, reject) => {
        console.log("saving to database")
        console.log(value)
        var transaction = db.transaction([table], "readwrite")
        transaction.oncomplete = function(event) {
            console.log("transaction complete")
            resolve()
        }
        transaction.onerror = function(event) {
            console.log("transaction error")
            console.log(event)
            reject()
        }
        var objectStore = transaction.objectStore(table);
        var addRequest = objectStore.put(value)
        addRequest.onsuccess = function(event) {
            console.log('add request successful')
        }
        addRequest.onerror = function(event) {
            console.log("failed to save")
            console.log(event)
        }
    })
}

function saveActualResponseToDatabase(response) {
    return new Promise((resolve, reject) => {
        var url = new URL(response.url)
        var key = url.pathname + url.search
        var bookId = url.searchParams.get("id")
        var headers = Object.fromEntries(response.headers.entries())
        response.blob().then(responseBlob => {
            var entry = {
                url: key,
                response: responseBlob,
                headers: Object.fromEntries(response.headers.entries()),
                bookId: bookId
            }
            saveToDatabase(REQUESTS_TABLE, entry)
                .then(() => resolve(entry))
        })
    })
}

function saveResponseToDatabase(url, bookId) {
    return new Promise((resolve, reject) => {
        /*fetch(url).then(response => response.blob().then(responseBlob => {
            console.log(response.headers)
            var entry = {
                url: url,
                response: responseBlob,
                headers: Object.fromEntries(response.headers.entries()),
                bookId: bookId
            }
            saveToDatabase(REQUESTS_TABLE, entry).then(() => resolve(entry))
                .catch(() => reject())
        }))*/
        fetch(url).then(response => saveActualResponseToDatabase(response)).then(entity => resolve(entity))
    })
}

function saveToDevice(bookId, type, maxPositions) {
    console.log('saving to device')
    if (type === 'comic') {
        saveComicToDevice(bookId, maxPositions)
    } else if (type === 'book') {
        saveBookToDevice(bookId, maxPositions)
    }
}

function saveBookToDevice(bookId, maxPositions) {
    var url = '/book?id=' + bookId
    console.log('downloading book ' + url)
    fetchResponseFromDatabase(url)
        .then(response => {
            if (response) saveBookSectionToDevice(bookId, maxPositions, 0)
            else saveResponseToDatabase(url, bookId).then(() => saveBookSectionToDevice(bookId, maxPositions, 0))
        })
}

function saveComicToDevice(comicId, pages) {
    var url = '/comic?id=' + comicId
    console.log("downloading comic to cache " + url)
    fetchResponseFromDatabase(url)
        .then(response => {
            if (response) saveComicPageToDevice(comicId, pages, 0)
            else saveResponseToDatabase(url, comicId).then(() => saveComicPageToDevice(comicId, pages, 0))
        })
}

function saveBookSectionToDevice(bookId, maxPositions, position) {
    return new Promise((resolve, reject) => {
        if (position < maxPositions) {
            var url = '/bookSection?id=' + bookId + "&position=" + position
            console.log('downloading book section ' + url)
            fetchResponseFromDatabase(url)
                .then(response => {
                    if (response) {
                        var nextPosition = parseInt(response.headers['sectionend']) + 1
                        saveBookSectionToDevice(bookId, maxPositions, nextPosition)
                    } else saveResponseToDatabase(url, bookId).then(response => {
                        saveBookResourcesToDevice(bookId, response.response)
                        var nextPosition = parseInt(response.headers['sectionend']) + 1
                        saveBookSectionToDevice(bookId, maxPositions, nextPosition)
                    })
                    resolve()
                })
        } else {
            console.log('done saving book to device ' + bookId)
            resolve()
        }
    })
}

function saveBookResourcesToDevice(bookId, section) {
    section.text().then(text => {
        var structure = JSON.parse(text)
        var node = convert(structure)
        var resources = node.getResources()
        console.log('resources:')
        console.log(resources)
        resources
            .filter(resource => resource.startsWith('bookResource'))
            .forEach(resource => saveResponseToDatabase('/' + resource, bookId))
    })
}

function saveComicPageToDevice(comicId, pages, page) {
    return new Promise((resolve, reject) => {
        if (page < pages) {
            var url = '/imageData?id=' + comicId + '&page=' + page
            console.log("save comic page to device " + url)
            fetchResponseFromDatabase(url)
                .then(response => {
                    if (response) saveComicPageToDevice(comicId, pages, page + 1)
                    else saveResponseToDatabase(url, comicId).then(() => saveComicPageToDevice(comicId, pages, page + 1))
                    resolve()
                })
        } else {
            console.log("done saving comic " + comicId)
            resolve()
        }
    })
}

function deleteFromDatabaseForIndex(table, indexName, indexValue) {
    return new Promise((resolve, reject) => {
        var transaction = db.transaction([table], "readwrite")
        var objectStore = transaction.objectStore(table)
        var index = objectStore.index(indexName)
        index.openCursor(IDBKeyRange.only(indexValue)).onsuccess = event => {
            var cursor = event.target.result
            if (cursor) {
                objectStore.delete(cursor.primaryKey)
                cursor.continue();
            } else {
                resolve()
            }
        }
    })
}