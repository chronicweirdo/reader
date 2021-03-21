var cacheName = 'chronic-reader-cache'
var dbName = 'chronic-reader-db'
var dbVersion = 1
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
    var requestsStore = db.createObjectStore('requests', {keyPath: 'url'})
    requestsStore.createIndex('bookId', 'bookId', { unique: false })
    requestsStore.transaction.oncomplete = function(event) {
        console.log('created requests store')
        /*var customerObjectStore = db.transaction("customers", "readwrite").objectStore("customers");
        customerData.forEach(function(customer) {
          customerObjectStore.add(customer);
        });*/
    };
    var progressStore = db.createObjectStore('progress', {keyPath: 'id'})
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
    '/comic.js'
]

self.addEventListener('install', e => {
    console.log("service worker processing install")
    e.waitUntil(
        caches.open(cacheName).then(cache => cache.addAll(filesToCache))
        //self.skipWaiting() // TODO: does this activate the service worker immediately
    )
})

self.addEventListener('activate', e => {
    console.log("service worker activating")
    self.clients.claim()
})

self.addEventListener('fetch', e => {
    var url = new URL(e.request.url)

    if (url.pathname === '/markProgress') {
        e.respondWith(fetch(e.request).catch(() => storeToProgressDatabase(e.request, url)))
    } else if (url.pathname === '/loadProgress') {
        e.respondWith(fetch(e.request).catch(() => getProgressFromDatabase(e.request, url)))
    } else if (url.pathname === '/latestRead') {
        e.respondWith(fetch(e.request)
            .then(response => {
                handleLatestRead(response.clone())
                return response
            })
            .catch(() => fetchFromCache(request, url))
        )
    } else if (url.pathname === '/imageData' || url.pathname === '/comic') {
        var key = url.pathname + url.search
        e.respondWith(fetchResponseFromDatabase(key).then(response => {
            if (response) {
                return response
            } else {
                console.log("failed to find in database")
                return fetch(e.request)
            }
        }))
    } else {
        e.respondWith(fetch(e.request).catch(() => fetchFromCache(e.request, url)))
    }
})

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

function fetchResponseFromDatabase(key) {
    return new Promise((resolve, reject) => {
        loadFromDatabase('requests', key)
            .then(result => {
                if (result) {
                    resolve(new Response(result.response, {headers: new Headers(result.headers)}))
                } else {
                    resolve(undefined)
                }
            })
    })
    /*return new Promise((resolve, reject) => {
        var transaction = db.transaction(["requests"])
        var objectStore = transaction.objectStore("requests");
        var dbRequest = objectStore.get(key);
        dbRequest.onerror = function(event) {
            console.log("failed to load from database")
            reject()
        };
        dbRequest.onsuccess = function(event) {
            console.log("success loading from database")

            //rObject.status = 200
            console.log(event.target.result)
            if (event.target.result) {
                //var body = JSON.stringify(event.target.result.response)
                resolve(new Response(event.target.result.response, {headers: new Headers(event.target.result.headers)}))
            } else {
                //reject("object not found")
                resolve(undefined)
            }
        };
    })*/
}

/*function sendOfflineOperationResponse(request, url) {

        return storeToProgressDatabase(request, url)
    } else if (url.pathname == '/latestRead') {
        return handleLatestRead(request, url)
    } else {
        return fetchFromCache(request, url)
    }
}*/

function handleLatestRead(response) {
    console.log("handling latest read")
    console.log(response)
    response.json().then(json => {
        console.log(json)
        var booksToKeep = new Set()
        for (var i = 0; i < json.length; i++) {
            var book = json[i]
            console.log(book)
            booksToKeep.add(book.id)
            if (book.type === 'comic') {
                //saveComicToDevice(book.id, book.pages)
            }
        }
        clearFromCache(booksToKeep)
    })
    /*console.log("books on device:")
    console.log(getBooksOnDevice())*/
    //return response
}

function storeToProgressDatabase(request, url) {
    return new Promise((resolve, reject) => {
        console.log("storing progress to db for later: " + request.url)
        var id = url.searchParams.get("id")
        var position = url.searchParams.get("position")
        saveToDatabase('progress', {id: id, position: position})
            .then(() => resolve(new Response()))
            .catch(() => reject())
    })
}

function getProgressFromDatabase(request, url) {
    return new Promise((resolve, reject) => {
        var id = url.searchParams.get("id")
        loadFromDatabase('progress', id)
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
    if (event.data.type === 'storeBook') {
        console.log('storing book')
        console.log(event)
    } else if (event.data.type === 'storeComic') {
        console.log("received store comic message")
        console.log(event)
        var comicId = event.data.bookId
        var comicPages = event.data.pages
        saveComicToDevice(comicId, comicPages)
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

function saveComicToDevice(comicId, pages) {
    var url = '/comic?id=' + comicId
    console.log("downloading comic to cache " + url)
    fetchResponseFromDatabase(url)
        .then(response => {
            console.log(response)
            if (response) saveComicPageToDevice(comicId, pages, 0)
            else saveResponseToDatabase(url, comicId).then(() => saveComicPageToDevice(comicId, pages, 0))
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

function saveResponseToDatabase(url, bookId) {
    return new Promise((resolve, reject) => {
        fetch(url).then(response => response.text().then(responseText => {
            console.log(response.headers)
            var entry = {
                url: url,
                response: responseText,
                headers: Object.fromEntries(response.headers.entries()),
                bookId: bookId
            }
            saveToDatabase("requests", entry).then(() => resolve())
                .catch(() => reject())
        }))
    })
}

function saveComicPageToDevice(comicId, pages, page) {
    if (page < pages) {
        var url = '/imageData?id=' + comicId + '&page=' + page
        console.log("save comic page to device " + url)
        fetchResponseFromDatabase(url)
            .then(response => {
                if (response) saveComicPageToDevice(comicId, pages, page + 1)
                else saveResponseToDatabase(url, comicId).then(() => saveComicPageToDevice(comicId, pages, page + 1))
            })
    } else {
        console.log("done saving comic " + comicId)
    }
}