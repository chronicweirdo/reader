// setting object containing
/*
{
    "name": // how it's saved in local storage
    "default": // default value as string
    "parser": // function that transforms the string representation of the setting to its actual value
    "textValue": // function converting the setting to a string that can be shown in UI
    "textName": // the name of the setting as shown in UI
    "controller": // function that gives us the UI element for controlling this setting
    "listeners": // a list of objects to which we send the new value when the setting is updated
}

then, in the UI we grab the textValue, the textName and the controller, we register listeners
*/





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

function bookModeToString(mode) {
    if (mode == 0) {
        return "dark"
    } else if (mode == 2) {
        return "light"
    } else {
        return "auto"
    }
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
    /*controller.style.display = 'grid'
    controller.style.width = 'fit-content'
    controller.style.gridTemplateColumns = 'auto auto'*/
    /*input.style.justifySelf = 'right'*/
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

        // <output for="foo" onforminput="value = foo.valueAsNumber;"></output>
        let output = document.createElement('output')
        output.style.marginLeft = "10px"
        output.htmlFor = setting.name
        output.value = setting.getTextValue()
        setting.output = output

        input.addEventListener('input', function(event) {
            setting.put(event.target.value)
            setting.output.value = setting.getTextValue()
        }, false)

        let controller = document.createElement('div')
        controller.classList.add('setting')
        /*controller.style.display = 'grid'
        controller.style.width = 'fit-content'
        controller.style.gridTemplateColumns = 'auto auto'*/

        /*
        grid-column-start: 1;
        grid-column-end: 3;
        */
        input.style.gridColumnStart = '1'
        input.style.gridColumnEnd = '3'
        input.style.justifySelf = 'auto'
        /*output.style.justifySelf = 'right'*/
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
        setting.put(event.target.value)
    }
    setting.input = input

    let controller = document.createElement('div')
    controller.classList.add('setting')
    /*controller.style.display = 'grid'
    controller.style.width = 'fit-content'
    controller.style.gridTemplateColumns = 'auto auto'*/
    /*input.style.justifySelf = 'right'*/
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
    /*controller.style.display = 'grid'
    controller.style.width = 'fit-content'
    controller.style.gridTemplateColumns = 'auto auto'*/
    /*input.style.justifySelf = 'right'*/
    controller.appendChild(label)
    controller.appendChild(input)
    return controller
}

function Setting(name, textName, defaultValue, parser, textValueFunction, createControllerFunction) {
    this.name = name
    this.defaultValue = defaultValue
    this.parser = parser
    this.textValueFunction = textValueFunction
    this.textName = textName
    this.controller = createControllerFunction(this)
}

Setting.prototype.get = function() {
    let stringValue = window.localStorage.getItem(this.name)
    if (! stringValue) {
        stringValue = this.defaultValue
    }
    if (this.parser) {
        return this.parser(stringValue)
    } else {
        return stringValue
    }
}

Setting.prototype.getTextValue = function() {
    if (this.textValueFunction) return this.textValueFunction(this.get())
    else return this.get()
}

Setting.prototype.put = function(value) {
    if (this.encoder) {
        window.localStorage.setItem(this.name, this.encoder(value))
    } else {
        window.localStorage.setItem(this.name, value)
    }
    if (this.listeners) {
        this.listeners.forEach(listener => listener(value))
    }
}

Setting.prototype.addListener = function(listener) {
    if (! this.listeners) {
        this.listeners = []
    }
    this.listeners.push(listener)
}

/*Setting.prototype.createLabel = function() {
    let label = document.createElement('label')
    label.htmlFor = this.name
    label.innerHTML = this.textName
    return label
}*/

// function Setting(name, textName, defaultValue, parser, textValueFunction, createControllerFunction) {
var SETTING_DARK_MODE_BACKGROUND = new Setting("dark_mode_background", "dark mode background", "#000000", null, null, createColorController)
var SETTING_DARK_MODE_FOREGROUND = new Setting("dark_mode_foreground", "dark mode foreground", "#ffffff", null, null, createColorController)
var SETTING_COMIC_SCROLL_SPEED = new Setting("comic_scroll_speed", "comic scroll speed", "0.001", parseFloat, null, createNumberController(0.0005, 0.005, 0.0001))
var SETTING_LIGHT_MODE_BACKGROUND = new Setting("light_mode_background", "light mode background", "#ffffff", null, null, createColorController)
var SETTING_LIGHT_MODE_FOREGROUND = new Setting("light_mode_foreground", "light mode foreground", "#000000", null, null, createColorController)
var SETTING_BOOK_ZOOM = new Setting("book_zoom", "book zoom", "1.5", parseFloat, null, createNumberController(0.9, 2.1, 0.2))
var SETTING_COMIC_PAN_SPEED = new Setting("comic_pan_speed", "comic pan speed", "3", parseInt, null, createNumberController(1, 10, 1))
var SETTING_COMIC_INVERT_SCROLL = new Setting("comic_invert_scroll", "comic invert scroll", "false", parseBoolean, null, createBooleanController)
var SETTING_LATEST_READ_LIMIT = new Setting("latest_read_limit", "latest read limit", "6", parseInt, null, createNumberController(0, 12, 1))
var SETTING_COMIC_HORIZONTAL_JUMP = new Setting("comic_horizontal_jump", "horizontal jump", "0.9", parseFloat, percentageToString, createNumberController(0.1, 1, 0.1))
var SETTING_COMIC_VERTICAL_JUMP = new Setting("comic_vertical_jump", "vertical jump", "0.5", parseFloat, percentageToString, createNumberController(0.1, 1, 0.1))
var SETTING_COMIC_ROW_THRESHOLD = new Setting("comic_row_threshold", "comic row threshold", "0.02", parseFloat, percentageToString, createNumberController(0.01, 0.1, 0.01))
var SETTING_COMIC_COLUMN_THRESHOLD = new Setting("comic_column_threshold", "comic column threshold", "0.05", parseFloat, percentageToString, createNumberController(0.01, 0.1, 0.01))
var SETTING_LIBRARY_DISPLAY_TITLE = new Setting("library_display_title", "library display title", "false", parseBoolean, null, createBooleanController)
var SETTING_SWIPE_PAGE = new Setting("swipe_page", "swipe page", "true", parseBoolean, null, createBooleanController)
var SETTING_SWIPE_VERTICAL_THRESHOLD = new Setting("swipe_vertical_threshold", "swipe vertical threshold", "0.11", parseFloat, percentageToString, createNumberController(0.01, 0.41, 0.1)) // screen percentage for vertical finger move before swipe becomes invalid
var SETTING_SWIPE_LENGTH = new Setting("swipe_length", "swipe length", "0.06", parseFloat, percentageToString, createNumberController(0.01, 0.31, 0.05)) // screen percentage for horizontal finger move for swipe action to register
var SETTING_ACCENT_COLOR = new Setting("accent_color", "accent color", "#FFD700", null, null, createColorController)
var SETTING_FOREGROUND_COLOR = new Setting("foreground_color", "foreground color", "#000000", null, null, createColorController)
var SETTING_BACKGROUND_COLOR = new Setting("background_color", "background color", "#FFFFFF", null, null, createColorController)
var SETTING_DESIRED_STATUS_BAR_LUMINANCE = new Setting("desired_status_bar_luminance", "desired status bar luminance", "180", parseInt, null, createNumberController(150, 255, 5))
var SETTING_DAY_START = new Setting("day_start", "day start", "07:00", parseTime, null, createTimeController)
var SETTING_DAY_END = new Setting("day_end", "day end", "22:00", parseTime, null, createTimeController)
var SETTING_BOOK_MODE = new Setting("book_mode", "book mode", "1", parseInt, bookModeToString, createNumberController(0, 2, 1))
var SETTING_BOOK_EDGE_HORIZONTAL = new Setting("book_edge_horizontal", "book edge horizontal", "0.1", parseFloat, percentageToString, createNumberController(0.05, 0.2, 0.05))
var SETTING_BOOK_EDGE_VERTICAL = new Setting("book_edge_vertical", "book edge vertical", "0.05", parseFloat, percentageToString, createNumberController(0.03, 0.11, 0.02))
var SETTING_BOOK_TOOLS_HEIGHT = new Setting("book_tools_height", "book tools height", "0.1", parseFloat, percentageToString, createNumberController(0.05, 0.3, 0.05))
var SETTING_OVERLAY_TRANSPARENCY = new Setting("overlay_transparency", "overlay transparency", "0.8", parseFloat, percentageToString, createNumberController(0.5, 0.9, 0.1))
