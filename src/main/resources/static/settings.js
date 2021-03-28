var SETTING_DARK_MODE_BACKGROUND = "dark_mode_background"
var SETTING_DARK_MODE_FOREGROUND = "dark_mode_foreground"
var SETTING_COMIC_SCROLL_SPEED = "comic_scroll_speed"
var SETTING_DARK_MODE = "dark_mode"
var SETTING_LIGHT_MODE_BACKGROUND = "light_mode_background"
var SETTING_LIGHT_MODE_FOREGROUND = "light_mode_foreground"
var SETTING_BOOK_ZOOM = "book_zoom"

var settingDefaults = {}
settingDefaults[SETTING_COMIC_SCROLL_SPEED] = "0.001"
settingDefaults[SETTING_DARK_MODE_BACKGROUND] = "#000000"
settingDefaults[SETTING_DARK_MODE_FOREGROUND] = "#ffffff"
settingDefaults[SETTING_DARK_MODE] = "false"
settingDefaults[SETTING_LIGHT_MODE_BACKGROUND] = "#ffffff"
settingDefaults[SETTING_LIGHT_MODE_FOREGROUND] = "#000000"
settingDefaults[SETTING_BOOK_ZOOM] = "1.5"


function parseBoolean(value) {
    return value == 'true'
}

var settingParsers = {}
settingParsers[SETTING_COMIC_SCROLL_SPEED] = parseFloat
settingParsers[SETTING_BOOK_ZOOM] = parseFloat
settingParsers[SETTING_DARK_MODE] = parseBoolean

var settingEncoders = {}

var settingListeners = {}

function createColorController(settingName, text, displayValue) {
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
    if (displayValue) {
        let span = document.createElement('span')
        span.innerHTML = value
        return [label, input, span]
    } else {
        return [label, input]
    }
}

function createNumberController(settingName, text, min, max, step, displayValue) {
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
    if (displayValue) {
        let span = document.createElement('span')
        span.innerHTML = value
        return [label, input, span]
    } else {
        return [label, input]
    }
}

function createBooleanController(settingName, text, displayValue) {
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
    if (displayValue) {
        let span = document.createElement('span')
        span.innerHTML = value
        return [label, input, span]
    } else {
        return [label, input]
    }
}

var settingControllers = {}
settingControllers[SETTING_DARK_MODE_BACKGROUND] = (displayValue) => createColorController(SETTING_DARK_MODE_BACKGROUND, "dark mode background color", displayValue)
settingControllers[SETTING_DARK_MODE_FOREGROUND] = (displayValue) => createColorController(SETTING_DARK_MODE_FOREGROUND, "dark mode text color", displayValue)
settingControllers[SETTING_LIGHT_MODE_BACKGROUND] = (displayValue) => createColorController(SETTING_LIGHT_MODE_BACKGROUND, "light mode background color", displayValue)
settingControllers[SETTING_LIGHT_MODE_FOREGROUND] = (displayValue) => createColorController(SETTING_LIGHT_MODE_FOREGROUND, "light mode text color", displayValue)
settingControllers[SETTING_BOOK_ZOOM] = (displayValue) => createNumberController(SETTING_BOOK_ZOOM, "book zoom", 0.5, 2.5, 0.1, displayValue)
settingControllers[SETTING_DARK_MODE] = (displayValue) => createBooleanController(SETTING_DARK_MODE, "dark mode", displayValue)

function updateSetting(element) {
    let valueField = event.target.nextElementSibling
    if (valueField && valueField.nodeType != 'SPAN') {
        valueField = undefined
    }
    let value
    if (element.type == 'checkbox') {
        value = element.checked
    } else {
        value = element.value
    }
    if (valueField) {
        valueField.innerHTML = value
    }
    putSetting(element.name, value)
}

function initElement(element) {
    console.log("initializing element for " + element.name)
    let value = getSetting(element.name)
    console.log(value)
    if (value != undefined) {
        if (element.type == 'checkbox') {
            element.checked = value
        } else {
            element.value = value
        }
        let valueField = element.nextElementSibling
        console.log(valueField)
        if (valueField) {
            valueField.innerHTML = value
        }
    }
    element.onchange = function(event) {
        updateSetting(event.target)
    }
}

function getSettingController(name, displayValue = false) {
    let settingControllerCreator = settingControllers[name]
    if (settingControllerCreator) {
        return settingControllerCreator(displayValue)
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
