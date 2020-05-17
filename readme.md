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