var SETTING_DARK_MODE_BACKGROUND = "dark_mode_background"
var SETTING_DARK_MODE_FOREGROUND = "dark_mode_foreground"
var SETTING_COMIC_SCROLL_SPEED = "comic_scroll_speed"
var SETTING_DARK_MODE = "dark_mode"
var SETTING_LIGHT_MODE_BACKGROUND = "light_mode_background"
var SETTING_LIGHT_MODE_FOREGROUND = "light_mode_foreground"
var SETTING_BOOK_ZOOM = "book_zoom"
var SETTING_COMIC_PAN_SPEED = "comic_pan_speed"
var SETTING_COMIC_INVERT_SCROLL = "comic_invert_scroll"
var SETTING_LATEST_READ_LIMIT = "latest_read_limit"
var SETTING_COMIC_HORIZONTAL_JUMP = "comic_horizontal_jump"
var SETTING_COMIC_VERTICAL_JUMP = "comic_vertical_jump"
var SETTING_COMIC_ROW_THRESHOLD = "comic_row_threshold"
var SETTING_COMIC_COLUMN_THRESHOLD = "comic_column_threshold"
var SETTING_LIBRARY_DISPLAY_TITLE = "library_display_title"
var SETTING_SWIPE_PAGE = "swipe_page"
var SETTING_SWIPE_VERTICAL_THRESHOLD = "swipe_vertical_threshold" // screen percentage for vertical finger move before swipe becomes invalid
var SETTING_SWIPE_LENGTH = "swipe_length" // screen percentage for horizontal finger move for swipe action to register
var SETTING_ACCENT_COLOR = "accent_color"
var SETTING_FOREGROUND_COLOR = "foreground_color"
var SETTING_BACKGROUND_COLOR = "background_color"

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
settingDefaults[SETTING_LATEST_READ_LIMIT] = "6"
settingDefaults[SETTING_COMIC_HORIZONTAL_JUMP] = "0.9"
settingDefaults[SETTING_COMIC_VERTICAL_JUMP] = "0.5"
settingDefaults[SETTING_COMIC_ROW_THRESHOLD] = "0.02"
settingDefaults[SETTING_COMIC_COLUMN_THRESHOLD] = "0.05"
settingDefaults[SETTING_LIBRARY_DISPLAY_TITLE] = "false"
settingDefaults[SETTING_SWIPE_PAGE] = "true"
settingDefaults[SETTING_SWIPE_VERTICAL_THRESHOLD] = "0.11"
settingDefaults[SETTING_SWIPE_LENGTH] = "0.06"
settingDefaults[SETTING_ACCENT_COLOR] = "#FFD700"
settingDefaults[SETTING_FOREGROUND_COLOR] = "#000000"
settingDefaults[SETTING_BACKGROUND_COLOR] = "#FFFFFF"

function parseBoolean(value) {
    return value == 'true'
}

var settingParsers = {}
settingParsers[SETTING_COMIC_SCROLL_SPEED] = parseFloat
settingParsers[SETTING_BOOK_ZOOM] = parseFloat
settingParsers[SETTING_COMIC_PAN_SPEED] = parseInt
settingParsers[SETTING_DARK_MODE] = parseBoolean
settingParsers[SETTING_COMIC_INVERT_SCROLL] = parseBoolean
settingParsers[SETTING_LATEST_READ_LIMIT] = parseInt
settingParsers[SETTING_COMIC_HORIZONTAL_JUMP] = parseFloat
settingParsers[SETTING_COMIC_VERTICAL_JUMP] = parseFloat
settingParsers[SETTING_COMIC_ROW_THRESHOLD] = parseFloat
settingParsers[SETTING_COMIC_COLUMN_THRESHOLD] = parseFloat
settingParsers[SETTING_LIBRARY_DISPLAY_TITLE] = parseBoolean
settingParsers[SETTING_SWIPE_PAGE] = parseBoolean
settingParsers[SETTING_SWIPE_VERTICAL_THRESHOLD] = parseFloat
settingParsers[SETTING_SWIPE_LENGTH] = parseFloat

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
    let textValue = document.createElement('span')
    textValue.innerHTML = value
    input.onchange = function(event) {
        updateSetting(event.target)
        textValue.innerHTML = getSetting(settingName)
    }
    return [label, input, textValue]
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
    let textValue = document.createElement('span')
    textValue.innerHTML = value
    input.addEventListener('input', function(event) {
        updateSetting(event.target)
        textValue.innerHTML = getSetting(settingName)
    }, false)
    return [label, input, textValue]
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
    let textValue = document.createElement('span')
    textValue.innerHTML = value
    input.onchange = function(event) {
        updateSetting(event.target)
        textValue.innerHTML = getSetting(settingName)
    }
    return [label, input, textValue]
}

var settingControllers = {}
settingControllers[SETTING_DARK_MODE_BACKGROUND] = () => createColorController(SETTING_DARK_MODE_BACKGROUND, "dark background")
settingControllers[SETTING_DARK_MODE_FOREGROUND] = () => createColorController(SETTING_DARK_MODE_FOREGROUND, "dark text")
settingControllers[SETTING_LIGHT_MODE_BACKGROUND] = () => createColorController(SETTING_LIGHT_MODE_BACKGROUND, "light background")
settingControllers[SETTING_LIGHT_MODE_FOREGROUND] = () => createColorController(SETTING_LIGHT_MODE_FOREGROUND, "light text",)
settingControllers[SETTING_BOOK_ZOOM] = () => createNumberController(SETTING_BOOK_ZOOM, "book zoom", 0.9, 2.1, 0.2)
settingControllers[SETTING_COMIC_SCROLL_SPEED] = () => createNumberController(SETTING_COMIC_SCROLL_SPEED, "scroll speed", 0.0005, 0.005, 0.0001)
settingControllers[SETTING_COMIC_PAN_SPEED] = () => createNumberController(SETTING_COMIC_PAN_SPEED, "pan speed", 1, 10, 1)
settingControllers[SETTING_DARK_MODE] = () => createBooleanController(SETTING_DARK_MODE, "dark mode")
settingControllers[SETTING_COMIC_INVERT_SCROLL] = () => createBooleanController(SETTING_COMIC_INVERT_SCROLL, "invert scroll")
settingControllers[SETTING_LATEST_READ_LIMIT] = () => createNumberController(SETTING_LATEST_READ_LIMIT, "latest read to load", 0, 12, 1)

settingControllers[SETTING_BOOK_ZOOM] = () => createNumberController(SETTING_BOOK_ZOOM, "book zoom", 0.9, 2.1, 0.2)
settingControllers[SETTING_COMIC_HORIZONTAL_JUMP] = () => createNumberController(SETTING_COMIC_HORIZONTAL_JUMP, "horizontal jump", 0.1, 1, 0.1)
settingControllers[SETTING_COMIC_VERTICAL_JUMP] = () => createNumberController(SETTING_COMIC_VERTICAL_JUMP, "vertical jump", 0.1, 1, 0.1)
settingControllers[SETTING_COMIC_ROW_THRESHOLD] = () => createNumberController(SETTING_COMIC_ROW_THRESHOLD, "row threshold", 0.01, 0.1, 0.01)
settingControllers[SETTING_COMIC_COLUMN_THRESHOLD] = () => createNumberController(SETTING_COMIC_COLUMN_THRESHOLD, "column threshold", 0.01, 0.1, 0.01)

settingControllers[SETTING_LIBRARY_DISPLAY_TITLE] = () => createBooleanController(SETTING_LIBRARY_DISPLAY_TITLE, "display titles")
settingControllers[SETTING_SWIPE_PAGE] = () => createBooleanController(SETTING_SWIPE_PAGE, "swipe pages")
settingControllers[SETTING_SWIPE_VERTICAL_THRESHOLD] = () => createNumberController(SETTING_SWIPE_VERTICAL_THRESHOLD, "swipe y threshold", 0.01, 0.41, 0.1)
settingControllers[SETTING_SWIPE_LENGTH] = () => createNumberController(SETTING_SWIPE_LENGTH, "swipe length", 0.01, 0.31, 0.05)

settingControllers[SETTING_ACCENT_COLOR] = () => createColorController(SETTING_ACCENT_COLOR, "accent color")
settingControllers[SETTING_FOREGROUND_COLOR] = () => createColorController(SETTING_FOREGROUND_COLOR, "foreground color")
settingControllers[SETTING_BACKGROUND_COLOR] = () => createColorController(SETTING_BACKGROUND_COLOR, "background color")

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
