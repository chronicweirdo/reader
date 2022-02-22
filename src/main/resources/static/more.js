function clearStorage(element) {
    window.localStorage.clear()
    if('serviceWorker' in navigator) {
        navigator.serviceWorker.controller.postMessage({type: 'reset'})
    }
    element.innerHTML = element.innerHTML + " - done!"
    loadSettings()
    updateAccentColor()
    updateBackgroundColor()
    updateForegroundColor()
}

function updateAccentColor() {
    document.documentElement.style.setProperty('--accent-color', SETTING_ACCENT_COLOR.get())
    if (updateStatusBarForMorePage) updateStatusBarForMorePage()
}

function updateStatusBarForMorePage() {
    setStatusBarColor(SETTING_ACCENT_COLOR.get())
}

function updateForegroundColor() {
    document.documentElement.style.setProperty('--foreground-color', SETTING_FOREGROUND_COLOR.get())
}

function updateBackgroundColor() {
    document.documentElement.style.setProperty('--background-color', SETTING_BACKGROUND_COLOR.get())
    if (updateStatusBarForMorePage) updateStatusBarForMorePage()
}

function setDaySettingsStatus() {
    let theme = SETTING_THEME.get()
    if (theme == THEME_TIME) {
        document.getElementsByName(SETTING_DAY_START.name)[0].disabled = false
        document.getElementsByName(SETTING_DAY_END.name)[0].disabled = false
    } else {
        document.getElementsByName(SETTING_DAY_START.name)[0].disabled = true
        document.getElementsByName(SETTING_DAY_END.name)[0].disabled = true
    }
}
function configureThemeForMorePage() {
    configureTheme(true)
}

function getSettingCategory(title) {
    let cat = document.createElement("h3")
    cat.innerHTML = title
    return cat
}

function loadSettings() {
    let settingsWrapper = document.getElementById('settings')
    settingsWrapper.innerHTML = ""

    // library settings
    settingsWrapper.appendChild(getSettingCategory("Library Settings"))
    settingsWrapper.appendChild(SETTING_LATEST_READ_LIMIT.controller)
    settingsWrapper.appendChild(SETTING_LATEST_ADDED_LIMIT.controller)
    settingsWrapper.appendChild(SETTING_COLLECTIONS_IN_BOOK_TITLES.controller)

    // theme settings
    settingsWrapper.appendChild(getSettingCategory("Theme Settings"))
    settingsWrapper.appendChild(SETTING_THEME.controller)
    SETTING_THEME.addListener(function() {
        configureThemeForMorePage()
        setDaySettingsStatus()
    })

    settingsWrapper.appendChild(SETTING_DARK_BACKGROUND_COLOR.controller)
    SETTING_DARK_BACKGROUND_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_DARK_TEXT_COLOR.controller)
    SETTING_DARK_TEXT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_DARK_ACCENT_COLOR.controller)
    SETTING_DARK_ACCENT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_DARK_ACCENT_TEXT_COLOR.controller)
    SETTING_DARK_ACCENT_TEXT_COLOR.addListener(configureThemeForMorePage)

    settingsWrapper.appendChild(SETTING_LIGHT_BACKGROUND_COLOR.controller)
    SETTING_LIGHT_BACKGROUND_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_LIGHT_TEXT_COLOR.controller)
    SETTING_LIGHT_TEXT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_LIGHT_ACCENT_COLOR.controller)
    SETTING_LIGHT_ACCENT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_LIGHT_ACCENT_TEXT_COLOR.controller)
    SETTING_LIGHT_ACCENT_TEXT_COLOR.addListener(configureThemeForMorePage)

    settingsWrapper.appendChild(SETTING_DAY_START.controller)
    settingsWrapper.appendChild(SETTING_DAY_END.controller)

    settingsWrapper.appendChild(SETTING_OVERLAY_TRANSPARENCY.controller)
    setDaySettingsStatus()

    // gesture settings
    settingsWrapper.appendChild(getSettingCategory("Controls Settings"))
    settingsWrapper.appendChild(SETTING_SWIPE_PAGE.controller)
    settingsWrapper.appendChild(SETTING_SWIPE_LENGTH.controller)
    settingsWrapper.appendChild(SETTING_SWIPE_ANGLE_THRESHOLD.controller)
    settingsWrapper.appendChild(SETTING_COMIC_INVERT_SCROLL.controller)
    settingsWrapper.appendChild(SETTING_COMIC_SCROLL_SPEED.controller)
    settingsWrapper.appendChild(SETTING_COMIC_PAN_SPEED.controller)
    // controls settings
    settingsWrapper.appendChild(SETTING_BOOK_EDGE_HORIZONTAL.controller) // also affects comic
    settingsWrapper.appendChild(SETTING_BOOK_TOOLS_HEIGHT.controller) // also affects comic

    // book settings
    settingsWrapper.appendChild(getSettingCategory("Book Settings"))
    settingsWrapper.appendChild(SETTING_BOOK_ZOOM.controller)
    settingsWrapper.appendChild(SETTING_BOOK_EDGE_VERTICAL.controller)

    // comic settings
    settingsWrapper.appendChild(getSettingCategory("Comic Settings"))
    settingsWrapper.appendChild(SETTING_COMIC_HORIZONTAL_JUMP.controller)
    settingsWrapper.appendChild(SETTING_COMIC_VERTICAL_JUMP.controller)
    settingsWrapper.appendChild(SETTING_COMIC_ROW_THRESHOLD.controller)
    settingsWrapper.appendChild(SETTING_COMIC_COLUMN_THRESHOLD.controller)
}

window.onload = function() {
    loadSettings()
    configureThemeForMorePage()
}