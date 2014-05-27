$(function() {

// Included on all /admin pages

// qs will include all querystring params from the request URL as a dictionary
var qs = {};
var _public = { qs: qs, serialize: serialize};

(function () {
    var e,
        a = /\+/g,
        r = /([^&=]+)=?([^&]*)/g,
        d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        q = window.location.search.substring(1);

    while (e = r.exec(q))
        qs[d(e[1])] = d(e[2]).split(",");
})();

function serialize(obj) {
    var str = [];
    for(var p in obj) {
        str.push(p + "=" + encodeURIComponent(obj[p]));
    }
    return str.join("&");
}

window.qs = _public;

// Wire up popovers
$(document).on("click", "a[rel='popover']", function() {
    var $me = $(this);
    $("a[rel='popover']").not($me).popover("hide");
    $me.popover("show");
    if ( $me.data("adv-position")) {
        $("body").addClass($me.data("adv-position"));
    }
    $(document).on("click", "*", function() {
        if ( $me.data("adv-position")) {
            $("body").removeClass($me.data("adv-position"));
        }
        $me.popover("hide");
        $(document).off("click", "*");
    });
    return false;
});

});
