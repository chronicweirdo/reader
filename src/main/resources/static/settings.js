var SETTING_DARK_MODE_BACKGROUND = "dark_mode_background"
var SETTING_DARK_MODE_FOREGROUND = "dark_mode_foreground"
var SETTING_COMIC_SCROLL_SPEED = "comic_scroll_speed"
var SETTING_DARK_MODE = "dark_mode"
var SETTING_LIGHT_MODE_BACKGROUND = "light_mode_background"
var SETTING_LIGHT_MODE_FOREGROUND = "light_mode_foreground"
var SETTING_BOOK_ZOOM = "book_zoom"
var SETTING_COMIC_PAN_SPEED = "comic_pan_speed"
var SETTING_COMIC_INVERT_SCROLL = "comic_invert_scroll"

var settingDefaults = {}
settingDefaults[SETTING_COMIC_SCROLL_SPEED] = "0.001"
settingDefaults[SETTING_COMIC_PAN_SPEED] = "3"
settingDefaults[SETTING_DARK_MODE_BACKGROUND] = "#000000"
settingDefaults[SETTING_DARK_MODE_FOREGROUND] = "#ffffff"
settingDefaults[SETTING_DARK_MODE] = "false"
settingDefaults[SETTING_LIGHT_MODE_BACKGROUND] = "#ffffff"
settingDefaults[SETTING_LIGHT_MODE_FOREGROUND] = "#000000"
settingDefaults[SETTING_BOOK_ZOOM] = "1.5"
settingDefaults[SETTING_COMIC_INVERT_SCROLL] = "false"

function parseBoolean(value) {
    return value == 'true'
}

var settingParsers = {}
settingParsers[SETTING_COMIC_SCROLL_SPEED] = parseFloat
settingParsers[SETTING_BOOK_ZOOM] = parseFloat
settingParsers[SETTING_COMIC_PAN_SPEED] = parseInt
settingParsers[SETTING_DARK_MODE] = parseBoolean
settingParsers[SETTING_COMIC_INVERT_SCROLL] = parseBoolean

var settingEncoders = {}

var settingListeners = {}

function createColorController(settingName, text) {
    let label = document.createElement('label')
    label.htmlFor = settingName
    label.innerHTML = text
    let input = document.createElement('input')
    input.type = 'color'
    input.name = settingName
    let value = getSetting(settingName)
    input.value = value
    input.onchange = function(event) {
        updateSetting(event.target)
    }
    return [label, input]
}

function createNumberController(settingName, text, min, max, step) {
    let label = document.createElement('label')
    label.htmlFor = settingName
    label.innerHTML = text
    let input = document.createElement('input')
    input.type = 'range'
    input.name = settingName
    input.min = min
    input.max = max
    input.step = step
    let value = getSetting(settingName)
    input.value = value
    input.onchange = function(event) {
        updateSetting(event.target)
    }
    return [label, input]
}

function createBooleanController(settingName, text) {
    let label = document.createElement('label')
    label.htmlFor = settingName
    label.innerHTML = text
    let input = document.createElement('input')
    input.type = 'checkbox'
    input.name = settingName
    let value = getSetting(settingName)
    input.checked = value
    input.onchange = function(event) {
        updateSetting(event.target)
    }
    return [label, input]
}

var settingControllers = {}
settingControllers[SETTING_DARK_MODE_BACKGROUND] = () => createColorController(SETTING_DARK_MODE_BACKGROUND, "dark background")
settingControllers[SETTING_DARK_MODE_FOREGROUND] = () => createColorController(SETTING_DARK_MODE_FOREGROUND, "dark text")
settingControllers[SETTING_LIGHT_MODE_BACKGROUND] = () => createColorController(SETTING_LIGHT_MODE_BACKGROUND, "light background")
settingControllers[SETTING_LIGHT_MODE_FOREGROUND] = () => createColorController(SETTING_LIGHT_MODE_FOREGROUND, "light text",)
settingControllers[SETTING_BOOK_ZOOM] = () => createNumberController(SETTING_BOOK_ZOOM, "book zoom", 0.5, 2.5, 0.1)
settingControllers[SETTING_COMIC_SCROLL_SPEED] = () => createNumberController(SETTING_COMIC_SCROLL_SPEED, "scroll speed", 0.0005, 0.005, 0.0001)
settingControllers[SETTING_COMIC_PAN_SPEED] = () => createNumberController(SETTING_COMIC_PAN_SPEED, "pan speed", 1, 10, 1)
settingControllers[SETTING_DARK_MODE] = () => createBooleanController(SETTING_DARK_MODE, "dark mode")
settingControllers[SETTING_COMIC_INVERT_SCROLL] = () => createBooleanController(SETTING_COMIC_INVERT_SCROLL, "invert scroll")

function updateSetting(element) {
    let value
    if (element.type == 'checkbox') {
        value = element.checked
    } else {
        value = element.value
    }
    putSetting(element.name, value)
}

function getSettingController(name) {
    let settingControllerConstructor = settingControllers[name]
    if (settingControllerConstructor) {
        return settingControllerConstructor()
    }
    return undefined
}

function getSetting(name) {
    let stringValue = window.localStorage.getItem(name)
    if (! stringValue) {
        stringValue = settingDefaults[name]
    }
    if (settingParsers[name]) {
        return settingParsers[name](stringValue)
    } else {
        return stringValue
    }
}

function putSetting(name, value) {
    if (settingEncoders[name]) {
        window.localStorage.setItem(name, settingEncoders[name](value))
    } else {
        window.localStorage.setItem(name, value)
    }
    if (settingListeners[name]) {
        settingListeners[name].forEach(listener => listener(value))
    }
}

function addSettingListener(name, listener) {
    if (! settingListeners[name]) {
        settingListeners[name] = []
    }
    settingListeners[name].push(listener)
}
