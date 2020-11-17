var data = "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam bibendum dapibus tempus. Duis fringilla nibh sed turpis malesuada, quis vestibulum dui tincidunt. Nam in facilisis risus, a vehicula libero. Fusce at vestibulum tellus. Quisque et posuere nunc. Sed est mi, scelerisque non massa in, dictum gravida augue. Praesent vel aliquam velit, euismod varius ipsum. Nulla pretium odio mauris, quis tempus magna rhoncus nec. In tincidunt velit non quam ultrices faucibus. Phasellus sit amet dictum magna. Etiam tincidunt purus est, at tincidunt orci mollis congue. Ut interdum accumsan ligula non molestie. Ut feugiat quam suscipit porttitor viverra. Praesent a massa ipsum.\nNam in tempus elit. Ut iaculis lorem id quam rutrum interdum. Morbi est enim, aliquet vitae rhoncus ac, cursus quis risus. Nam non mi sit amet nulla consequat dictum. Praesent vitae laoreet quam, a gravida ligula. Duis volutpat mi risus, vel blandit orci lacinia tincidunt. Sed congue est id quam dignissim molestie. Duis dui orci, fermentum ac dolor a, hendrerit facilisis leo. Aenean ultrices faucibus augue vel convallis. Nullam nec laoreet nunc.</p><p>Duis elementum sapien ut est feugiat imperdiet. Nullam mi ex, vehicula at ultrices ac, tincidunt id odio. Ut dolor dui, semper eget purus sed, fermentum ornare felis. Vivamus sit amet nisi non dui euismod cursus. Maecenas cursus leo nisi, quis rutrum purus mollis eget. Mauris orci augue, rhoncus vel luctus a, accumsan vel ligula. In eget tellus vestibulum, molestie dolor at, maximus nisl. Sed rutrum mi eu massa varius, non mollis lacus congue. Vivamus id nisl neque.</p><p>Aliquam a arcu ullamcorper, pretium lacus vitae, tristique nunc. Nullam pellentesque blandit commodo. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nulla placerat finibus vestibulum. Phasellus finibus tristique enim a feugiat. Nam cursus scelerisque ante, in congue nibh tempus quis. Donec dictum, lectus a lobortis porttitor, nisi felis gravida augue, a ullamcorper turpis lorem sed massa.</p><p>Cras turpis mi, placerat dictum risus quis, aliquam dictum risus. Sed at metus sed urna facilisis dictum sit amet et mauris. Fusce massa justo, congue quis porta laoreet, sagittis consequat enim. Donec lobortis, ante a iaculis dignissim, eros purus ultricies lacus, a ultricies ex libero at eros. Vestibulum sit amet pulvinar ante. Proin molestie iaculis dolor, interdum sodales libero congue consectetur. Integer pulvinar sodales dolor, faucibus posuere augue vehicula laoreet. Duis commodo placerat egestas. Morbi pharetra, magna vitae fringilla bibendum, massa nunc sodales est, ut scelerisque arcu est non tortor. Pellentesque vel semper augue, ut maximus augue. Quisque ut lectus luctus elit vehicula faucibus ac eu metus. Sed et consectetur metus, at malesuada erat. Suspendisse aliquet ullamcorper gravida. Proin iaculis dictum eleifend. Vestibulum dapibus diam at lectus gravida egestas vitae nec arcu.</p>"

function scrollNecessary(el) {
    return el.scrollHeight > el.offsetHeight || el.scrollWidth > el.offsetWidth
}

function getPageFor(position, callback) {
    // get the page
    var page = data
    // invoke the callback
    callback(page)
}

function displayPageFor(position) {
    console.log("displaying page for " + position)
    // show spinner
    getPageFor(position, function(page) {
        var content = document.getElementById("content")
        console.log("content scroll necessary (before): " + scrollNecessary(content))
        content.innerHTML = page
        console.log("content scroll necessary: " + scrollNecessary(content))
        var shadowContent = document.getElementById("shadowContent")
        console.log("shadow content scroll necessary (before): " + scrollNecessary(shadowContent))
        shadowContent.innerHTML = page
        console.log("shadow content scroll necessary: " + scrollNecessary(shadowContent))
        // hide spinner
    })
}

window.onload = function() {
    var startPosition = num(getMeta("startPosition"))
    console.log("start position: " + startPosition)

    displayPageFor(startPosition)
}