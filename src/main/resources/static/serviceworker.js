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
    var objectStore = db.createObjectStore('requests', { keyPath: 'url' })
    objectStore.createIndex('bookId', 'bookId', { unique: false })
    objectStore.transaction.oncomplete = function(event) {
        console.log('created object store')
        /*var customerObjectStore = db.transaction("customers", "readwrite").objectStore("customers");
        customerData.forEach(function(customer) {
          customerObjectStore.add(customer);
        });*/
    };
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
    } else if (url.pathname === '/latestRead') {
        e.respondWith(fetch(e.request)
            .then(response => {
                handleLatestRead(response.clone())
                return response
            })
            .catch(() => fetchFromCache(request, url))
        )
    } else if (url.pathname === '/imageData') {
        e.respondWith(fetchFromDatabase(url).catch(() => {
            console.log("failed to find in database")
            return fetch(e.request)
        }))
    } else {
        e.respondWith(fetch(e.request).catch(() => fetchFromCache(e.request, url)))
    }
})

function fetchFromDatabase(url) {
    return new Promise((resolve, reject) => {
        var key = url.pathname + url.search
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
            if (event.target.result) {
                console.log(event.target)
                var body = JSON.stringify(event.target.result.response)
                var rObject = new Response(body)
                console.log(rObject)
                resolve(rObject)
            } else {
                reject("object not found")
            }
        };
    })
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
    console.log("storing progress to db for later: " + request.url)
    var id = url.searchParams.get("id")
    var position = url.searchParams.get("id")
    return new Response()
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
            /*console.log("books on device from method")
            console.log(ids)
            return ids*/
        })
    })
}

/*function deleteComicFromDevice(comicId) {
    caches.open(cacheName).then(cache => {
        cache.keys().then(keys => {
            keys.forEach(function (request, index, array) {
                var url = new URL(request.url)
                if (url.pathname === '/imageData' && url.searchParams.get("id") == comicId) {
                    cache.delete(request)
                } else if (url.pathname === '/comic' && url.searchParams.get("id") == comicId) {
                    cache.delete(request)
                }
            })
        })
    })
}*/

function saveComicToDevice(comicId, pages) {
    console.log("downloading comic to cache")
    caches.open(cacheName).then(cache => {
        cache.add('/comic?id=' + comicId)
        /*for (var i = 0; i < pages; i++) {
            cache.add('/imageData?id=' + comicId + '&page=' + i)
        }*/
        addComicPageToCache(cache, comicId, pages, 0)
    })
    console.log("done saving comic")
}

function addComicPageToCache(cache, comicId, pages, page) {
    if (page < pages) {
        var url = '/imageData?id=' + comicId + '&page=' + page
        fetchFromDatabase(url)
            .then(() => addComicPageToCache(cache, comicId, pages, page + 1))
            .catch(() => {
                fetch(url).then(response => {
                    response.json().then(responseJson => {
                        var transaction = db.transaction(["requests"], "readwrite")
                        transaction.oncomplete = function(event) {
                            console.log("transaction complete")
                            addComicPageToCache(cache, comicId, pages, page + 1)
                        }
                        transaction.onerror = function(event) {
                            console.log("transaction error")
                        }
                        var requestResponse = {
                            url: url,
                            response: responseJson,
                            bookId: comicId
                        }
                        var requestsStore = transaction.objectStore("requests");
                        var addRequest = requestsStore.add(requestResponse)
                        addRequest.onsuccess = function(event) {
                            console.log('add request successful')
                        }
                    })
                })
            })



        /*caches.match(url).then(data => {
            if (data) {
                console.log("data already in cache for comic " + comicId + " page " + page)
                addComicPageToCache(cache, comicId, pages, page + 1)
            } else {
                cache
                    .add(url)
                    .then(() => addComicPageToCache(cache, comicId, pages, page + 1))
            }
        })*/
        //cache
        //    .add(url)
        //    .then(() => addComicPageToCache(cache, comicId, pages, page + 1))
    } else {
        console.log("done saving comic " + comicId)
    }
}