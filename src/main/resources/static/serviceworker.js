var cacheName = 'my-cache'
var filesToCache = [
  //'/',
  '/form.css',
  '/favicon.ico',

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
    )
})

self.addEventListener('activate', e => {
    console.log("service worker activating")
    self.clients.claim()
})

self.addEventListener('fetch', e => {
    console.log("am online: " + navigator.onLine)
    console.log("service worker fetching")

    e.respondWith(fetch(e.request).catch(() => sendOfflineOperationResponse(e.request)))

    /*e.respondWith(
        caches.match(e.request).then(response => {
            console.log("for request: " + e.request.url)
            if (response) {
                console.log("response in cache")
                return response
            } else {
                console.log("need to get response from server")
                return fetch(e.request).catch(() => sendOfflineOperationResponse(e.request))
            }
        })
    )*/
})

function sendOfflineOperationResponse(request) {
    var url = new URL(request.url)
    if (url.pathname === '/markProgress') {
        return storeToProgressDatabase(request, url)
    } else {
        return fetchFromCache(request, url)
    }
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

function saveComicToDevice(comicId, pages) {
    console.log("downloading comic to cache")
    caches.open(cacheName).then(cache => {
        cache.add('/openBook?id=' + comicId)
        cache.add('/comic?id=' + comicId)
        for (var i = 0; i < pages; i++) {
            cache.add('/imageData?id=' + comicId + '&page=' + i)
        }
    })
    console.log("done saving comic")
}