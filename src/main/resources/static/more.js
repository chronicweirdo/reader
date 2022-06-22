var ONLINE_ONLY_CLASS = "online-only"

function resetSettings(element) {
    for (let i = 0; i < SETTINGS.length; i++) {
        SETTINGS[i].reset()
    }
    element.innerHTML = element.innerHTML + " - done at " + Date()
    loadSettings()
    configureThemeForMorePage()
}

function clearBookPageCache(element) {
    let all_keys = Object.keys(window.localStorage).filter(k => k.startsWith("bookPages_"))
    for (let k in all_keys) {
        window.localStorage.removeItem(all_keys[k])
    }
    element.innerHTML = element.innerHTML + " - done at " + Date()
}

function resetApplication(element) {
    if('serviceWorker' in navigator) {
        if (navigator.serviceWorker.controller) {
            navigator.serviceWorker.controller.postMessage({type: 'reset'})
        }
    }
    element.innerHTML = element.innerHTML + " - done at " + Date()
}

//function clearStorage(element) {
    /*clearLocalStorage()
    element.innerHTML = element.innerHTML + " - done!"
    loadSettings()
    configureThemeForMorePage()*/
//}

function updateStatusBarForMorePage() {
    setStatusBarColor(SETTING_ACCENT_COLOR.get())
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

function checkOnline() {
    var xhttp = new XMLHttpRequest()
    xhttp.onreadystatechange = function() {
        if (this.readyState == 4) {
            if (this.status == 200) {
                var response = JSON.parse(this.responseText)
                if (response.offline == undefined) {
                    setToOnlineMode()
                }
            }
        }
    }
    xhttp.open("GET", "search?term=&page=0")
    xhttp.send()
}

function setToOnlineMode() {
    let onlineOnlyElements = Array.from(document.getElementsByClassName(ONLINE_ONLY_CLASS))
    for (let i = 0; i < onlineOnlyElements.length; i++) {
        //onlineOnlyElements[i].style.display = "none"
        onlineOnlyElements[i].classList.remove(ONLINE_ONLY_CLASS)
    }
}

function loadSettings() {
    let settingsWrapper = document.getElementById('settings')
    settingsWrapper.innerHTML = ""

    // library settings
    settingsWrapper.appendChild(getSettingCategory("Library Settings"))
    settingsWrapper.appendChild(SETTING_LATEST_READ_LIMIT.getController())
    settingsWrapper.appendChild(SETTING_LATEST_ADDED_LIMIT.getController())
    settingsWrapper.appendChild(SETTING_COLLECTIONS_IN_BOOK_TITLES.getController())
    settingsWrapper.appendChild(SETTING_LIBRARY_SCROLL_TOOLS.getController())

    // theme settings
    settingsWrapper.appendChild(getSettingCategory("Theme Settings"))
    settingsWrapper.appendChild(SETTING_THEME.getController())
    SETTING_THEME.addListener(function() {
        configureThemeForMorePage()
        setDaySettingsStatus()
    })

    settingsWrapper.appendChild(SETTING_DARK_BACKGROUND_COLOR.getController())
    SETTING_DARK_BACKGROUND_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_DARK_TEXT_COLOR.getController())
    SETTING_DARK_TEXT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_DARK_ACCENT_COLOR.getController())
    SETTING_DARK_ACCENT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_DARK_ACCENT_TEXT_COLOR.getController())
    SETTING_DARK_ACCENT_TEXT_COLOR.addListener(configureThemeForMorePage)

    settingsWrapper.appendChild(SETTING_LIGHT_BACKGROUND_COLOR.getController())
    SETTING_LIGHT_BACKGROUND_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_LIGHT_TEXT_COLOR.getController())
    SETTING_LIGHT_TEXT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_LIGHT_ACCENT_COLOR.getController())
    SETTING_LIGHT_ACCENT_COLOR.addListener(configureThemeForMorePage)
    settingsWrapper.appendChild(SETTING_LIGHT_ACCENT_TEXT_COLOR.getController())
    SETTING_LIGHT_ACCENT_TEXT_COLOR.addListener(configureThemeForMorePage)

    settingsWrapper.appendChild(SETTING_DAY_START.getController())
    settingsWrapper.appendChild(SETTING_DAY_END.getController())

    settingsWrapper.appendChild(SETTING_OVERLAY_TRANSPARENCY.getController())
    setDaySettingsStatus()

    // gesture settings
    settingsWrapper.appendChild(getSettingCategory("Controls Settings"))
    settingsWrapper.appendChild(SETTING_SWIPE_PAGE.getController())
    settingsWrapper.appendChild(SETTING_SWIPE_LENGTH.getController())
    settingsWrapper.appendChild(SETTING_SWIPE_ANGLE_THRESHOLD.getController())
    settingsWrapper.appendChild(SETTING_COMIC_INVERT_SCROLL.getController())
    settingsWrapper.appendChild(SETTING_COMIC_SCROLL_SPEED.getController())
    settingsWrapper.appendChild(SETTING_COMIC_PAN_SPEED.getController())
    // controls settings
    settingsWrapper.appendChild(SETTING_BOOK_EDGE_HORIZONTAL.getController()) // also affects comic
    settingsWrapper.appendChild(SETTING_BOOK_TOOLS_HEIGHT.getController()) // also affects comic

    // book settings
    settingsWrapper.appendChild(getSettingCategory("Book Settings"))
    settingsWrapper.appendChild(SETTING_BOOK_ZOOM.getController())
    settingsWrapper.appendChild(SETTING_BOOK_EDGE_VERTICAL.getController())

    // comic settings
    settingsWrapper.appendChild(getSettingCategory("Comic Settings"))
    settingsWrapper.appendChild(SETTING_COMIC_HORIZONTAL_JUMP.getController())
    settingsWrapper.appendChild(SETTING_COMIC_VERTICAL_JUMP.getController())
    settingsWrapper.appendChild(SETTING_COMIC_ROW_THRESHOLD.getController())
    settingsWrapper.appendChild(SETTING_COMIC_COLUMN_THRESHOLD.getController())
}

window.onload = function() {
    loadSettings()
    configureThemeForMorePage()
    window.addEventListener("focus", () => checkAndUpdateTheme(true), false)
    checkOnline()
}