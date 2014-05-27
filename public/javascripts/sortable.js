$(function() {
    // A more generic sortable helper, used to support the sorting on the admin experience's lists
    var parent = $("table.sortable");
    var cnt = parent.length;
    window.grid = parent;
    var route = parent.attr("rel");
    parent.find('th.sortable a').append(_.template(templates.sortTemplate, globals));
    
    var gridParams = {};
    window.gridP = gridParams;
        
    (function () {
        var e,
            a = /\+/g,
            r = /([^&=]+)=?([^&]*)/g,
            d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
            q = window.location.search.substring(1);

        while (e = r.exec(q))
            gridParams[d(e[1])] = d(e[2]).split(",");
        
        if ( gridParams["sort"])
            gridParams["sort"].length = cnt;
        if ( gridParams["sortDirection"])
            gridParams["sortDirection"].length = cnt;
        if ( gridParams["scroll"]) 
            $(window).scrollTop(gridParams["scroll"])
    })();
    
    function serialize(obj) {
        var str = [];
        for(var p in obj) {
            str.push(p + "=" + encodeURIComponent(obj[p]));
        }
        return str.join("&");
    }
    
    if ( gridParams["sortDirection"] && gridParams["sort"]) {
        parent.each(function(idx,el) {
            var cur = $(el).find('th.sortable a[rel='+gridParams["sort"][idx]+']');
            cur.closest(".sortable").addClass("sorted");
            if ( gridParams["sortDirection"][idx] == "asc") {
                cur.closest(".sortable").addClass("asc");
            }
            else {
                cur.closest(".sortable").addClass("desc");
            }
        });
    }
    
    parent.each(function(idx,el) {
        $(el).find('th.sortable a').click(function() {
            var ths = $(this);
            gridParams["scroll"] = $(window).scrollTop();
            var curDir = ths.closest(".sortable").hasClass("desc") ? "desc": "asc";
            if ( !gridParams["sort"]) {
                gridParams["sort"] = [];
                gridParams["sort"].length = cnt;
            }
            if ( !gridParams["sortDirection"]) {
                gridParams["sortDirection"] = [];
                gridParams["sortDirection"].length = cnt;
            }
            
            parent.find("th.sortable.sorted a").each(function(i,el) {
                var $cur = $(this);
                var rel = $cur.attr("rel");
                gridParams.sort[idx] = rel;
                gridParams.sortDirection[idx] = $cur.closest(".sortable").hasClass("desc") ? "desc": "asc";
            });
            
            if ( curDir == "desc") {
                gridParams.sortDirection[idx] = "asc";
            }
            else {
                gridParams.sortDirection[idx] = "desc";
            }
            gridParams.sort[idx] = ths.attr("rel");
            
            ths.attr("href", route + "?" + serialize(gridParams));
        });
    });
    
    $("select[name='pageSize']").change(function() {
        gridParams.pageSize = $(this).val();
        gridParams.page = 0;
        window.location.href = window.location.pathname + "?" + serialize(gridParams);
    });
});
