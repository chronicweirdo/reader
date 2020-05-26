# Reader

A very simple web-reader app. Should support the following functionality:

- be able to show contents of CBR and EPUB files in a web page
- store a library of those files
- be able to sync position in those files across devices
- allow, where possible, the storage of notes on those files
- if all this works well, extend to other file types (CBZ, PDF)

## Preliminary Test

- preliminary test in reading CBR file was successful
- preliminary test in reading EPUB file was successful
- for CBR files it can be easier to build a reading UI
    - I need a JS library to allow zoom and pan on an image
    - for each CBR file, the reading position consists of the page and the UI settings (zoom and pan)
- for EPUB files, making a UI will be more complex
    - first, when presenting the file for consumption, the whole archive needs to be treated as a whole web page (in case there are images or other media)
    - then we need functions to compute:
        - what a "position" in the book is
        - how much we can display in the page
        - what the next positions is, based on UI settings (font size)
    - we may use some html library to read the file and compute the position of each character or word, by looking just at the text content that gets displayed
    - then, we need a way to extract part of the html file, from position X to position Y, but keep the html structure for those positions (?)

- whatever approach I take, I need a clear API to represent a book (epub or cbr or otherwise)
    - when loading a book, the api called by the web UI should be the same, preferably
    - should know how much content the book has
    - should know how to give content at position X

## Epub pagination

- the simplest approach
    - load epub html, show it in a frame with `overflow: hidden;`
    - when clicking for next page, get the viewport height (`Math.max(document.documentElement.clientHeight, window.innerHeight || 0);`, then scroll between pages with `window.scrollTo(0, 2*1508)` (1508 is the page size).
    - the position in the book is the current html file + scroll value (+ view settings)
    - the problem here is that part of the lines on adjacent pages can still be visible

- another approach: https://stackoverflow.com/questions/8518257/render-the-epub-book-in-android

- a library that seems to do what I need: https://github.com/mertakdut/EpubParser/blob/8fdc48879e596ea33d4b908f8a5cea2973de6d88/src/main/java/com/github/mertakdut/Reader.java#L493

- I've looked at the source code of that library, what it does:
    - it computes the contents for each page successively
    - if you want to get page 10, it will compute pages 1 through 9 first (if they were not computed before)
    - the size of a page is determined by the number of characters shown in that page

- I would approach this a different way
    - the size of the page can still be determined by the number of characters in that page
    - the ideea would be to insert page breaks inside a html file
    - load the file in memory, go over actual characters in the file and approximately every X characters insert a page break <div>
    - if that page break is in the middle of a paragraph/complex tag structure, insert closing tags and reopen tags appropriately around the inserted page break

## Rudimentary comic book reading app

- already showing a list of all comics in the collection with their cover image and the name in the "title"
- already can click through comic
- todo:
    - better comic model (with total number of pages)
    - integrate a database that contains comic information (and will eventually contain users and progress information)
    - make app scan for changes between comic library and database on start (also have an option in the ui to trigger rescan, maybe do it periodically)
    - integrate a html templating engine (not ideea what, what's hip now with the kids?)
    - integrate a js library that gives image "controls": pinch or mouse zoom and pan on the image
    - integrate a js library that detects clicks in the edges of the screen (finger or mouse) to flip between pages (maybe even have a page flip if finger flips the screen, if the image is not zoomed)
    - also make a swipe from top (or mouse hover near top) open the ui menu that can take you back to the comics library
    - make the library page organize comics in folders (and subfolders?)
    - introduce a section with "recently" at the top of the library page
    - add option to download comic file
    - add option to upload comic file
    - (a lot of ui work)
    
## Epub Reading App idea

- to correctly identify the number of words to display on each page, use a html/css in-memory renderer
    - set the page size to what we have in the UI
    - set the page css (margins) to what we have in the UI
    - add words in the page until we detect an overflow (the appearance of scroll bars)
    - remove last word after overflow happened
    - this is the page size
    - we can attempt to do this for the whole book when loading it from disk, and every time the UI settings are changed (check performance)
    
- we also need to find a way to treat
    - images
    - tables
    
## Spring Data with Scala

https://spring.io/guides/gs/accessing-data-jpa/
https://stackoverflow.com/questions/27865403/spring-data-with-scala

## Building the UI

Spring template engines: https://www.baeldung.com/spring-template-engines

Js gestures library:
    - https://hammerjs.github.io/
    - http://gojquery.com/best-javascript-touch-gesture-libraries/
    
- the operations on a comic book page:
    - pan: left, right, up, down; triggered by mouse click and drag, or finger drag (touchscreen)
    - zoom: in, out; triggered by mouse scroll or pinch gesture
    - jump: previous, next page; triggered by click on ui sections for it, or by finger-click
    - display menu: lowers down the top menu; triggerred by mouse hover in top area, or by finger drag from top (if possible, on touch screen)
    
- also, the jump operations should be implemented using Ajax, so that on mobile, the back operation will take used back to the collections page

- CONCLUSIONS
    - kind of difficult to make the UI with javascript behave in a mobile/touch enabled browser
    - the big problem was preventing the pinch-zoom behavior of the browser app when I tried to implement my own behavior
    - the saner solution seems to be to keep the functionality at a minimum:
        - have a special page able to display a simple image
        - minimal css, mainly to switch between pages
        - the change should be made through Ajax, that way the back action will take us back to the comics list

- UPDATED CONCLUSIONS:
    - I made it work, with the generous help of the internet
    - the important part was finding the right dom element to remove events from and to bind the hammer library to
    - the correct element was html
    - i then changed the ui to be completely split into areas of influence, overlapping divs, with one large div in the middle of the page to which the gestures are bound
    
## A new model for the comic page UI

- we have the settings of the page held in memory:
    - original width
    - original height
    - left
    - top
    - zoom
- the image display is udpated based on those values
- operations (pan, zoom) change some of those values, within the acceptable limits, and then the image is redisplayed
- some of the limits can be precomputed and stored with the image settings:
    - minimum and maximum zoom, for example (minimum zoom is when the whole page fits on screen)
    
## Next phase of features

- [] When last page is reached, if next page is called, mark the comic as read
- [] Search functionality on home page
- [] Users for access (cookie based)
- [] Store comic progress on user (page, zoom level, pan settings? Would require sending pan info to server on every change; better only 2 seconds after pan/zoom operation is over)
- [] Show read/finished comics on home page, maybe even read progress bar? On bottom or top of page. Or read/total numbers?
- [] Functionality to mark comics as unread
- [] Functionality to show the latest read and unfinished comics at top of home page
- [] A go back button on the comic page?
- [] Functionality to do periodic rescan of the comics folder
- [] Functionality to download/upload comic?
- [] Response compression https://howtodoinjava.com/spring-boot2/rest/response-gzip-compression/
- [] don't load all comics on the home page from beginning (phase out loading, load on scroll...); but make sure search will look for comics on the server