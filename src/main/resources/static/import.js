function importSubmit(action) {
    console.log("setting action to " + action)
    document.getElementById("formAction").value = action
    var form = document.forms[0]
    form.submit()
}
window.onload = function() {
    checkAndUpdateTheme(true)
    window.addEventListener("focus", () => checkAndUpdateTheme(true), false)
}