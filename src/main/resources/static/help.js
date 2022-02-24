window.onload = function() {
    checkAndUpdateTheme(true)
    window.addEventListener("focus", () => checkAndUpdateTheme(true), false)
    addLibraryLinks()
}

function addLibraryLinks() {

    let getLibraryLink = function() {
        //<a href="/">library</a>
        let link = document.createElement("a")
        link.href = "/"
        link.innerHTML = "library"
        link.classList.add("libraryLink")
        return link
    }

    var links = document.getElementsByTagName("a")
    for (let i = 0; i < links.length; i++) {
        if (links[i].innerHTML == "top") {
            links[i].append(getLibraryLink())
        }
    }
}