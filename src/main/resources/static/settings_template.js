const SETTING_THEME_DEFAULT = "1"
const SETTING_DARK_BACKGROUND_COLOR_DEFAULT = "#000000"
const SETTING_DARK_TEXT_COLOR_DEFAULT = "#ffffff"
const SETTING_DARK_ACCENT_COLOR_DEFAULT = "#FFD700"
const SETTING_DARK_ACCENT_TEXT_COLOR_DEFAULT = "#000000"
const SETTING_LIGHT_BACKGROUND_COLOR_DEFAULT = "#ffffff"
const SETTING_LIGHT_TEXT_COLOR_DEFAULT = "#000000"
const SETTING_LIGHT_ACCENT_COLOR_DEFAULT = "#FFD700"
const SETTING_LIGHT_ACCENT_TEXT_COLOR_DEFAULT = "#000000"
const SETTING_LATEST_READ_LIMIT_DEFAULT = "6"
const SETTING_LATEST_ADDED_LIMIT_DEFAULT = "6"
const SETTING_DAY_START_DEFAULT = "07:00"
const SETTING_DAY_END_DEFAULT = "22:00"

function alignSettingWidths() {
    let settingsInPage = Array.from(document.getElementsByClassName('setting'))
    let maxWidth = Math.max(...settingsInPage.map(s => s.offsetWidth))
    settingsInPage.forEach(s => s.style.width = maxWidth + "px")
}

function parseBoolean(value) {
    return value == 'true'
}

function timeStringToDate(value) {
    return new Date((new Date()).toDateString() + " " + value)
}

function parseTime(value) {
    let date = timeStringToDate(value)
    return convertDateToTimeString(date)
}

function convertDateToTimeString(date) {
    let hour = date.getHours()
    let minute = date.getMinutes()
    let hourString = hour < 10 ? "0" + hour : hour
    let minuteString = minute < 10 ? "0" + minute : minute
    return hourString + ":" + minuteString
}

var THEME_DARK = 0
var THEME_OS = 1
var THEME_TIME = 2
var THEME_LIGHT = 3

function themeToString(theme) {
    if (theme == THEME_DARK) {
        return "dark"
    } else if (theme == THEME_OS) {
        return "OS theme"
    } else if (theme == THEME_TIME) {
        return "time based"
    } else {
        return "light"
    }
}

function degreeToString(degree) {
    return degree + "°"
}

function percentageToString(percentage) {
    return Math.floor(percentage * 100) + "%"
}

function createColorController(setting) {
    let label = document.createElement('label')
    label.htmlFor = setting.name
    label.innerHTML = setting.textName
    setting.label = label

    let input = document.createElement('input')
    input.type = 'color'
    input.name = setting.name
    let value = setting.get()
    input.value = value

    input.onchange = function(event) {
        setting.put(event.target.value)
    }
    setting.input = input

    let controller = document.createElement('div')
    controller.classList.add('setting')
    controller.appendChild(label)
    controller.appendChild(input)
    return controller
}

function createNumberController(min, max, step) {

    return function(setting) {
        let label = document.createElement('label')
        label.htmlFor = setting.name
        label.innerHTML = setting.textName
        setting.label = label

        let input = document.createElement('input')
        input.type = 'range'
        input.name = setting.name
        input.min = min
        input.max = max
        input.step = step
        input.value = setting.get()
        setting.input = input

        let output = document.createElement('output')
        if (output.style) output.style.marginLeft = "10px"
        output.htmlFor = setting.name
        output.value = setting.getTextValue()
        setting.output = output

        input.addEventListener('input', function(event) {
            setting.put(event.target.value)
            setting.output.value = setting.getTextValue()
        }, false)

        let controller = document.createElement('div')
        controller.classList.add('setting')
        if (input.style) {
            input.style.gridColumnStart = '1'
            input.style.gridColumnEnd = '3'
            input.style.justifySelf = 'auto'
        }
        controller.appendChild(label)
        controller.appendChild(output)
        controller.appendChild(input)
        return controller
    }
}

function createBooleanController(setting) {
    let label = document.createElement('label')
    label.htmlFor = setting.name
    label.innerHTML = setting.textName
    setting.label = label

    let input = document.createElement('input')
    input.type = 'checkbox'
    input.name = setting.name
    input.checked = setting.get()
    input.onchange = function(event) {
        setting.put(event.target.checked)
    }
    setting.input = input

    let controller = document.createElement('div')
    controller.classList.add('setting')
    controller.appendChild(label)
    controller.appendChild(input)
    return controller
}

function createTimeController(setting) {
    let label = document.createElement('label')
    label.htmlFor = setting.name
    label.innerHTML = setting.textName
    setting.label = label

    let input = document.createElement('input')
    input.type = 'time'
    input.name = setting.name
    input.value = setting.get()

    input.onchange = function(event) {
        setting.put(event.target.value)
    }
    setting.input = input

    let controller = document.createElement('div')
    controller.classList.add('setting')
    controller.appendChild(label)
    controller.appendChild(input)
    return controller
}

class Setting {
    constructor(name, textName, defaultValue, parser, textValueFunction, createControllerFunction, bookSpecific = false) {
        this.name = name
        this.defaultValue = defaultValue
        this.parser = parser
        this.textValueFunction = textValueFunction
        this.textName = textName
        this.bookSpecific = bookSpecific
        if (createControllerFunction) this.createControllerFunction = createControllerFunction
    }
    getController() {
        return this.createControllerFunction(this)
    }
    #getBookId() {
        return getMeta("bookId")
    }
    #getSettingName() {
        if (this.bookSpecific) {
            return this.name + "_" + this.#getBookId()
        } else {
            return this.name
        }
    }
    clearUnused(ids) {
        if (this.bookSpecific) {
            let all_keys = Object.keys(window.localStorage).filter(k => k.startsWith(this.name))
            for (let k in all_keys) {
                let id = all_keys[k].substring(all_keys[k].lastIndexOf("_") + 1)
                if (! ids.includes(id)) {
                    window.localStorage.removeItem(all_keys[k])
                }
            }
        }
    }
    get() {
        let stringValue = window.localStorage.getItem(this.#getSettingName())
        if (!stringValue) {
            stringValue = this.defaultValue
        }
        if (this.parser) {
            return this.parser(stringValue)
        } else {
            return stringValue
        }
    }
    getTextValue() {
        if (this.textValueFunction)
            return this.textValueFunction(this.get())
        else
            return this.get()
    }
    put(value) {
        if (this.encoder) {
            window.localStorage.setItem(this.#getSettingName(), this.encoder(value))
        } else {
            window.localStorage.setItem(this.#getSettingName(), value)
        }
        if (this.listeners) {
            this.listeners.forEach(listener => listener(value))
        }
    }
    addListener(listener) {
        if (!this.listeners) {
            this.listeners = []
        }
        this.listeners.push(listener)
    }
}



var SETTING_DARK_BACKGROUND_COLOR = new Setting("dark_background", "dark theme background color", SETTING_DARK_BACKGROUND_COLOR_DEFAULT, null, null, createColorController)
var SETTING_DARK_TEXT_COLOR = new Setting("dark_text", "dark theme text color", SETTING_DARK_TEXT_COLOR_DEFAULT, null, null, createColorController)
var SETTING_DARK_ACCENT_COLOR = new Setting("dark_accent", "dark theme accent background color", SETTING_DARK_ACCENT_COLOR_DEFAULT, null, null, createColorController)
var SETTING_DARK_ACCENT_TEXT_COLOR = new Setting("dark_accent_text", "dark theme accent text color", SETTING_DARK_ACCENT_TEXT_COLOR_DEFAULT, null, null, createColorController)

var SETTING_LIGHT_BACKGROUND_COLOR = new Setting("light_background", "light theme background color", SETTING_LIGHT_BACKGROUND_COLOR_DEFAULT, null, null, createColorController)
var SETTING_LIGHT_TEXT_COLOR = new Setting("light_text", "light theme text color", SETTING_LIGHT_TEXT_COLOR_DEFAULT, null, null, createColorController)
var SETTING_LIGHT_ACCENT_COLOR = new Setting("light_accent", "light theme accent background color", SETTING_LIGHT_ACCENT_COLOR_DEFAULT, null, null, createColorController)
var SETTING_LIGHT_ACCENT_TEXT_COLOR = new Setting("light_accent_text", "light theme accent text color", SETTING_LIGHT_ACCENT_TEXT_COLOR_DEFAULT, null, null, createColorController)

var SETTING_THEME = new Setting("theme", "theme", SETTING_THEME_DEFAULT, parseInt, themeToString, createNumberController(0, 3, 1))

var SETTING_COMIC_SCROLL_SPEED = new Setting("comic_scroll_speed", "comic scroll speed", "0.001", parseFloat, null, createNumberController(0.0005, 0.005, 0.0001))
var SETTING_BOOK_ZOOM = new Setting("book_zoom", "text size", "1.5", parseFloat, null, createNumberController(0.9, 2.1, 0.2))
var SETTING_COMIC_PAN_SPEED = new Setting("comic_pan_speed", "comic pan speed", "3", parseInt, null, createNumberController(1, 10, 1))
var SETTING_COMIC_INVERT_SCROLL = new Setting("comic_invert_scroll", "scroll down to zoom in", "false", parseBoolean, null, createBooleanController)
var SETTING_LATEST_READ_LIMIT = new Setting("latest_read_limit", "latest read books to load", SETTING_LATEST_READ_LIMIT_DEFAULT, parseInt, null, createNumberController(0, 12, 1))
var SETTING_COMIC_HORIZONTAL_JUMP = new Setting("comic_horizontal_jump (of screen width)", "horizontal jump", "0.9", parseFloat, percentageToString, createNumberController(0.1, 1, 0.1))
var SETTING_COMIC_VERTICAL_JUMP = new Setting("comic_vertical_jump", "vertical jump (of screen height)", "0.5", parseFloat, percentageToString, createNumberController(0.1, 1, 0.1))
var SETTING_COMIC_ROW_THRESHOLD = new Setting("comic_row_threshold", "comic row threshold (of comic page width)", "0.02", parseFloat, percentageToString, createNumberController(0.01, 0.1, 0.01))
var SETTING_COMIC_COLUMN_THRESHOLD = new Setting("comic_column_threshold", "comic column threshold (of comic page height)", "0.05", parseFloat, percentageToString, createNumberController(0.01, 0.1, 0.01))
var SETTING_LIBRARY_DISPLAY_TITLE = new Setting("library_display_title", "display title in library", "false", parseBoolean, null, createBooleanController)
var SETTING_SWIPE_PAGE = new Setting("swipe_page", "swipe to turn page", "true", parseBoolean, null, createBooleanController)
var SETTING_SWIPE_LENGTH = new Setting("swipe_length", "minimum swipe length (of screen width)", "0.06", parseFloat, percentageToString, createNumberController(0.01, 0.31, 0.05)) // screen percentage for horizontal finger move for swipe action to register

var SETTING_DAY_START = new Setting("day_start", "switch to light mode at", SETTING_DAY_START_DEFAULT, parseTime, null, createTimeController)
var SETTING_DAY_END = new Setting("day_end", "switch to dark mode at", SETTING_DAY_END_DEFAULT, parseTime, null, createTimeController)

var SETTING_BOOK_EDGE_HORIZONTAL = new Setting("book_edge_horizontal", "left/right book edge (of screen width)", "0.1", parseFloat, percentageToString, createNumberController(0.05, 0.2, 0.05))
var SETTING_BOOK_EDGE_VERTICAL = new Setting("book_edge_vertical", "top/bottom book edge (of screen height)", "0.05", parseFloat, percentageToString, createNumberController(0.03, 0.11, 0.02))
var SETTING_BOOK_TOOLS_HEIGHT = new Setting("book_tools_height", "tools button height (of screen height)", "0.1", parseFloat, percentageToString, createNumberController(0.05, 0.3, 0.05))
var SETTING_OVERLAY_TRANSPARENCY = new Setting("overlay_transparency", "tools panel transparency", "0.8", parseFloat, percentageToString, createNumberController(0.5, 0.9, 0.1))
var SETTING_LATEST_ADDED_LIMIT = new Setting("latest_added_limit", "latest added books to load", SETTING_LATEST_ADDED_LIMIT_DEFAULT, parseInt, null, createNumberController(0, 48, 6))
var SETTING_SWIPE_ANGLE_THRESHOLD = new Setting("swipe_angle_threshold", "maximum swipe angle", "30", parseInt, degreeToString, createNumberController(10, 60, 10))
var SETTING_ZOOM_JUMP = new Setting("zoom_jump", "zoom jump", "1.0", parseFloat, null, null, true)
var SETTING_COLLECTIONS_IN_BOOK_TITLES = new Setting("collections_book_titles", "show collections in latest read and added", "true", parseBoolean, null, createBooleanController)
var SETTING_FIT_COMIC_TO_SCREEN = new Setting("fit_comic_to_screen", "fit comic page to screen", "true", parseBoolean, null, null)