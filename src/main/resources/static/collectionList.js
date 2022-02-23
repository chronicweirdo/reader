var EXPAND_CLASS = "expand"
var EXPAND_HTML = "+"
var COLLAPSE_HTML = "-"
var EXPAND_TITLE = "expand"
var COLLAPSE_TITLE = "collapse"

function toggle(el) {
    var current = el
    while (current && current.tagName != "LI") current = current.parentElement
    var target = current.getElementsByTagName("UL")[0]
    if (target) {
        if (target.style.display != "none") {
            target.style.display = "none"
            let sign = target.previousElementSibling
            if (sign && sign.classList.contains(EXPAND_CLASS)) {
                sign.innerHTML = EXPAND_HTML
                sign.title = EXPAND_TITLE
            }
        } else {
            target.style.display = "block"
            let sign = target.previousElementSibling
            if (sign && sign.classList.contains(EXPAND_CLASS)) {
                sign.innerHTML = COLLAPSE_HTML
                sign.title = COLLAPSE_TITLE
            }
        }
    }
}

function addExpandSymbol(item) {
    var link = item.getElementsByTagName("A")[0]
    var expand = document.createElement("span")
    expand.classList.add(EXPAND_CLASS)
    expand.innerHTML = EXPAND_HTML
    expand.title = EXPAND_TITLE
    expand.addEventListener("click", (event) => {
        event.stopPropagation()
        toggle(event.target)
    })
    link.after(expand)
}

function getLiWithChildren() {
    var items = document.getElementsByTagName("LI")
    var result = []
    for (var i = 0; i < items.length; i++) {
        if (items[i].getElementsByTagName("UL").length > 0) {
            result.push(items[i])
        }
    }
    return result
}

window.onload = function() {
    checkAndUpdateTheme(true)
    var expandable = getLiWithChildren()
    for (var i = 0; i < expandable.length; i++) {
        addExpandSymbol(expandable[i])
        toggle(expandable[i])
    }
    window.addEventListener("focus", () => checkAndUpdateTheme(true), false)
}