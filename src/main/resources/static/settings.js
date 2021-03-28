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

/*function createColorController(wrapperType, settingName, label) {
    let wrapperType = document.createElement(wrapper)
    let label = document.createElement('label')

}*/

var settingControllers = {}
settingControllers[SETTING_DARK_MODE_BACKGROUND] =
    '<label for="' + SETTING_DARK_MODE_BACKGROUND + '">dark mode background color</label>'
    + '<input type="color" name="' + SETTING_DARK_MODE_BACKGROUND + '"><span></span>'
settingControllers[SETTING_DARK_MODE_FOREGROUND] =
    '<label for="' + SETTING_DARK_MODE_FOREGROUND + '">dark mode text color</label>'
    + '<input type="color" name="' + SETTING_DARK_MODE_FOREGROUND + '"><span></span>'
settingControllers[SETTING_LIGHT_MODE_BACKGROUND] =
    '<label for="' + SETTING_LIGHT_MODE_BACKGROUND + '">light mode background color</label>'
    + '<input type="color" name="' + SETTING_LIGHT_MODE_BACKGROUND + '"><span></span>'
settingControllers[SETTING_LIGHT_MODE_FOREGROUND] =
    '<label for="' + SETTING_LIGHT_MODE_FOREGROUND + '">light mode text color</label>'
    + '<input type="color" name="' + SETTING_LIGHT_MODE_FOREGROUND + '"><span></span>'
settingControllers[SETTING_COMIC_SCROLL_SPEED] =
    '<label for="' + SETTING_COMIC_SCROLL_SPEED + '">comic scroll speed:</label>'
    + '<input type="range" name="' + SETTING_COMIC_SCROLL_SPEED + '" min="0.0001" max="0.05" step="0.001">'
    + '<span></span>'
settingControllers[SETTING_DARK_MODE] =
    '<label for"' + SETTING_DARK_MODE + '">dark mode:</label>'
    + '<input type="checkbox" name="' + SETTING_DARK_MODE + '"><span></span>'
settingControllers[SETTING_BOOK_ZOOM] =
    '<label for="' + SETTING_BOOK_ZOOM + '">book zoom:</label>'
    + '<input type="range" name="' + SETTING_BOOK_ZOOM + '" min="0.5" max="2.5" step="0.1">'
    + '<span></span>'

function updateSetting(element) {
    let valueField = event.target.nextElementSibling
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
    let value = getSetting(element.name)
    if (value) {
        element.value = value
        let valueField = element.nextElementSibling
        if (valueField) {
            valueField.innerHTML = value
        }
    }
    element.onchange = function(event) {
        updateSetting(event.target)
    }
}

function getSettingController(name) {
    let settingControllerHtml = settingControllers[name]
    if (settingControllerHtml) {
        let wrapper = document.createElement('p')
        wrapper.innerHTML = settingControllerHtml
        let input = wrapper.getElementsByTagName('input')[0]
        initElement(input)
        return wrapper
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
