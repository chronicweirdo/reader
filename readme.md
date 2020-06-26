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

- [x] Users for access (cookie based)
- [x] make ui larger on phone (https://gist.github.com/marcedwards/3446599)
- [x] Store comic progress on user (page, zoom level, pan settings? Would require sending pan info to server on every change; better only 2 seconds after pan/zoom operation is over)
    - current implementation does not include zoom level or pan settings
    - if I add those, how will those values translate when switching devices?
    - also, there would probably be too much "chatter" between the UI and the backend
    - keeping it as is for now
- [x] Show read/finished comics on home page, maybe even read progress bar? On bottom or top of page. Or read/total numbers?
    - added the read progress bar, on hover (on pc) the number of pages out of total pages is displayed
    - only comics that were opened will show the progress bar
- [x] Functionality to mark comics as unread
- [x] Functionality to show the latest read and unfinished comics at top of home page

- [x] When last page is reached, if next page is called, mark the comic as read
- [x] Search functionality on home page
- [x] A go back button on the comic page?
    - implemented in the tools page
- [x] Functionality to do periodic rescan of the comics folder
- [] Functionality to download/upload comic?
- [x] Response compression https://howtodoinjava.com/spring-boot2/rest/response-gzip-compression/
    - 6 pages, before compression: 6857 kb
    - same 6 pages, after compression: 5071 kb
    - helps a bit, good for now
- [x] don't load all comics on the home page from beginning (phase out loading, load on scroll...); but make sure search will look for comics on the server
- [~] remember me functionality, and increase the life of the login session (to days! weeks!)
    - does not work between server restarts

- [x] refactor collection page (js especially); find icons for selection tools
    - some refactoring was done, gestures were redesigned to only need one icon
- [x] improve performance for progress load (only load progress for comics in page)
- [x] improved latest read comics load (db query) - set latest list to only 6 comics

- [-] try and see if it makes sense to jump to collection page when reaching the end or beginning and pressing next/before
    - this would be too confusing
- [x] add logs to file, keep only latest
    - actually, rolling file appender to keep for 5 days
- [x] jump to specific page when double clicking on sides in comic screen
    - this functionality has been implemented in a different manner: right click / hold on comic page to display a tools box, which shows the comic title, the number of pages and the current page, and the current page is an editable input field
- [x] add spinner when loading data through ajax
- [x] remove autocomplete from inputs
- [x] add clear field button for search

- jump to page functionality
- bookmark page (zoom, pan? viewport size?) functionality
- export bookmarks as images?

## Install Jar as Windows Service

Use [nssm](http://nssm.cc/). Run `nssm install chronicreader`. Used the following parameters:

- Path: `C:\Program Files\Java\jdk1.8.0_201\bin\java.exe`
- Startup directory: `C:\chronicreader`
- Arguments: `-jar reader-1.0.0.jar`
- Startup type: `Automatic`

New release can be done by stopping the service, copying the new jar, and starting the service back up. As long as the jar has the same name, it will be picked up.

## Further version one functionality, after using the app

- [x] show page numbers in status bar
- [x] toggle full screen on zoom on mobile
    - kind of distracting
- [-] toggle full screen on triple click?
    - not workable, we need to go through the second click to get to the third, kind of complicated to set up all the timing and everything
- [x] full screen toggle switch in tools, no more auto full screen
- [] reload home page when clearing search?
- [x] Adjust tablet sizing of home page to account for the scroll bar size (make comic pages a tini bit smaller)
- [x] set zindex of spinner to high on home page
- [x] Make drag work on all areas of the screen (drag can also start on edges)
- [x] Make drag faster
- [x] make double click work the same everywhere on the page (including edges)
- [] Use comic checksum to id comics (to be used in case the comic name and path changes)
- [] add preferences for users
    - preferences could be added for pan speed (currently 3), jump zoom factor (currently 2.5), how many recent comics to display (currently 6)
    - maybe a preference to turn on or off experimental features, if they exist (jump between panels or not, if it is implemented)
- [] use image processing to identify the comic panels, and jump from panel to panel (adjusting pan and zoom so the full panel is in view);

## A better scanner

https://docs.oracle.com/javase/tutorial/essential/io/notification.html

- since switching to using file checksum for comic id, scanning the disk will take several minutes
- can't do this every hour, maybe should not do this evey 3 hours either (it takes over )
- I need a new scanner service that deals with this
- a watcher on every folder, for changes
- if a folder is deleted, remove its watcher
- if a folder is added, also add a watcher
- if a file in a folder is added, full scan and save to database
- if a file in a folder is changed, again full scan and save to database
- if a file in a folde

- a separate branch was made for this, but the idea is for the moment abandoned; navigating the various file change events is too complicated and the benefits are minimal
- a different approach would be to detect changes on the library folder and subfolder, and after a time threshold (3 minutes, for example), to trigger a full rescan

## Panel Detection

Edge detection https://en.wikipedia.org/wiki/Edge_detection

http://www.pages.drexel.edu/~nk752/cannyTut2.html

https://docs.opencv.org/2.4/doc/tutorials/introduction/desktop_java/java_dev_intro.html

https://github.com/rstreet85/JCanny

The best Idea I have now is to:

- detect straight edges in the image (horizontal, vertical, maybe various diagonals)
- intersect edges them and then find all the poligons thus formed
- order the poligons left->right top->bottom
- send the poligons top-left and bottom-right corners in order to the UI 

This, as a feature, is pretty complex, the advantage of this is not obvious. The user would not have enough control, if the algorithm can't detect the

## The EPUB branch of the project

- epub files are archives with html contents
- there are a couple of ways to define the order of contents, tedious work to understand those
- the complicated problem here is how to split an html into pages
- I am tempted to now do this in browser:
    - load the whole html contents file
    - based on page size (a number, not the actual pixels size of the font) decide the number of words that make it into a page
        - I will then need to find the correlation between the page size and the font sizes to use, and test this across multiple devices (if I find the right math formula, even better)
    - split the contents into pages - by wrapping each page into a div (i did a similar thing for "powerpoint" like browser functionality in the past, but at that time I did not have to decide what fit into a page)
    - some complications and difficulties will arise around non-text elements:
        - images, maybe we can dedicate a full page to those, maybe even transition some of the comic side features like zooming and panning
        - tables, these will most likely have to be split somehow
        
- right not, I have a workable algorithm that:
    - reads the html document
    - puts it into a hidden storage
    - precomputes the pages using the browser rendering
    - displays the pages, moves between pages
- the algorithm computing how much constitutes a page does so by:
    - starting with 100 characters
    - keeps adding 10 characters to the page and checks if scroll is now necessary of not
    - the moment scroll becomes necessary we know the page has overflow, so we use the previous page size
- problems:
    - adding 10 characters may mean we grab the next paragrapf, triggering overflow, but maybe we still have enough space for the last word in the paragraph; I can consistently observe situations where only the last word of a paragraph is moved to the next page
- solutions:
    - need a new approach to page computing: maybe grow a page word by word, rather than an arbitrary number of characters
    
- problem: while iterating word by word might work, right now I have the issue that my betterTextContent and the actual positions in the content hierarchy do not match
- solution: found, a different approach to finding positions in text (buld an index of positions to text nodes), and the pages are built word by word.
        
## Bypass CORS when debugging:

Firefox, at `about:config` change the `security.fileuri.strict_origin_policy` to `false`

## Ebook Reader next steps

- learn about the ebook format, what is the expected structure of the archive, where to get the expected structure of the book
- implement loading of ebook from disk
- make links work - retrieve the file or image from the ebook archive (this would probably mean modifying and replacing all links inside the ebook as it gets loaded)
- have a different handling of page breaks? (may be difficult and not worth the effort)
- book navigation (with table of contents)
- bookmarks and other navigation features

Epub spec: http://idpf.org/epub/dir/