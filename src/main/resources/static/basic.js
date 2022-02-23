window.onload = function() {
    checkAndUpdateTheme(true)
    window.addEventListener("focus", () => checkAndUpdateTheme(true), false)
}