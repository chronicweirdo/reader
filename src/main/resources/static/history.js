var COLLECTION_CLASS = "history-collection"
var TITLE_CLASS = "history-title"
var BOOK_ID_CLASS = "history-book-id"
var PROGRESS_CLASS = "history-progress"
var DATE_CLASS = "history-date"
var ACTIONS_CLASS = "history-actions"
var BOOK_ID_ATTRIBUTE = "bookId"
var ORPHANED_CLASS = "history-orphaned"
var ORPHANED_CONTENT = "orphaned"

function deleteElementsForBookId(bookId) {
    let sel = Array.from(document.getElementById("ch_read_history").childNodes).filter(e => e.getAttribute).filter(e => e.getAttribute("bookId") == bookId)
    sel.forEach(e => e.remove())
}

function deleteProgress(id, element) {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                deleteElementsForBookId(id)
            }
        }
    }
    xhttp.open("DELETE", "removeProgress?id=" + id)
    xhttp.send()
}
function showSpinner() {
    var spinner = document.getElementById("spinner")
    spinner.style.display = "block"
}
function hideSpinner() {
    var spinner = document.getElementById("spinner")
    spinner.style.display = "none"
}
function getTableRow(book) {
    let elements = []

    var collection = document.createElement("span")
    collection.classList.add(COLLECTION_CLASS)
    collection.setAttribute(BOOK_ID_ATTRIBUTE, book.id)
    addCollectionLinkTokens(collection, book.collection, '/', searchLinkBuildHrefFunction)
    elements.push(collection)

    var title = document.createElement("span")
    title.classList.add(TITLE_CLASS)
    title.setAttribute(BOOK_ID_ATTRIBUTE, book.id)
    var titleLink = document.createElement("a")
    titleLink.href = book.type + "?id=" + book.id
    titleLink.innerHTML = book.title
    title.appendChild(titleLink)
    elements.push(title)

    var bookId = document.createElement("span")
    bookId.classList.add(BOOK_ID_CLASS)
    bookId.setAttribute(BOOK_ID_ATTRIBUTE, book.id)
    bookId.innerHTML = book.id
    elements.push(bookId)

    var progress = document.createElement("span")
    progress.classList.add(PROGRESS_CLASS)
    progress.setAttribute(BOOK_ID_ATTRIBUTE, book.id)
    var percent = ((book.progress + 1) / parseFloat(book.pages)) * 100
    progress.innerHTML = Math.floor(percent) + "%"
    elements.push(progress)

    var date = document.createElement("span")
    date.classList.add(DATE_CLASS)
    date.setAttribute(BOOK_ID_ATTRIBUTE, book.id)
    var lastUpdate = new Date(book.lastUpdate)
    date.innerHTML = lastUpdate.getFullYear() + "-" + (lastUpdate.getMonth() + 1) + "-" + lastUpdate.getDate() + " " + lastUpdate.getHours() + ":" + lastUpdate.getMinutes()
    elements.push(date)

    var orphaned = document.createElement("span")
    orphaned.classList.add(ORPHANED_CLASS)
    orphaned.setAttribute(BOOK_ID_ATTRIBUTE, book.id)
    if (book.orphaned) orphaned.innerHTML = ORPHANED_CONTENT
    elements.push(orphaned)

    var actions = document.createElement("span")
    actions.classList.add(ACTIONS_CLASS)
    actions.setAttribute(BOOK_ID_ATTRIBUTE, book.id)
    var removeProgressButton = document.createElement("a")
    removeProgressButton.onclick = function(event) {
        deleteProgress(book.id, this)
    }
    removeProgressButton.innerHTML = "delete"
    actions.appendChild(removeProgressButton)
    elements.push(actions)

    return elements
}
function loadHistory() {
    var xhttp = new XMLHttpRequest();
    showSpinner()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                var books = JSON.parse(this.responseText)
                if (books.length > 0) {
                    let bookIds = []
                    for (var i = 0; i < books.length; i++) {
                        var book = books[i]
                        var history = document.getElementById("ch_read_history")
                        if (history != null) {
                            let row = getTableRow(book)
                            for (let i = 0; i < row.length; i++) {
                                history.appendChild(row[i])
                            }
                        }
                    }

                }
            }
            hideSpinner()
        }
    }
    xhttp.open("GET", "historyData")
    xhttp.send()
}
window.onload = function() {
    configureTheme(true)
    loadHistory()
    window.addEventListener("focus", () => checkAndUpdateTheme(true), false)
}