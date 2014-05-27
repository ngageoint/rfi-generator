function bindEventGrid(parent) {
    // Binds all of the actions on the event grid (in the map experience)
    var parent = $(parent);
    var gridParams = {};
    // "gridParams" pulls sorting info from hidden field into a dictionary (querystring-encoded)
        
    (function () {
        var e,
            a = /\+/g,
            r = /([^&=]+)=?([^&]*)/g,
            d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
            q = parent.find("input[name='params']").val();

        while (e = r.exec(q))
           gridParams[d(e[1])] = d(e[2]);
    })();
    
    function serialize(obj) {
        var str = [];
        for(var p in obj)
         str.push(p + "=" + encodeURIComponent(obj[p]));
        return str.join("&");
    }
    
    parent.find('th.sortable a').append(_.template(templates.sortTemplate, globals));
    parent.find("td").not(":first-child").linkify({target:"_blank"});
    
    if ( gridParams["sortDirection"]) {
        var cur = $('th.sortable a[rel='+gridParams["sort"]+']');
        cur.closest(".sortable").addClass("sorted");
        if ( gridParams["sortDirection"] == "asc") {
            cur.closest(".sortable").addClass("asc");
        }
        else {
            cur.closest(".sortable").addClass("desc");
        }
    }
    
    parent.find('th.sortable a').click(function() {
        var ths = $(this);
        var destSort = "asc";
        var rel = ths.attr("rel");
        gridParams.sort = rel;
        
        if ( gridParams["sort"] == rel) {
            if ( gridParams["sortDirection"] == "asc") {
                gridParams.sortDirection = "desc";
            }        
            else {
                gridParams.sortDirection = "asc";
            }        
        }
        
        $.ajax({url: globals.route.EventController.grid, type: "GET", data: gridParams})
        .done(function(res,status,xhr) {
            parent.find(".modal-body").empty();
            parent.find(".modal-body").append(res);
            bindEventGrid(parent);
        });
        return false;
    });
    
    parent.find(".sendEmail").click(function() {
        parent.modal("hide");
        $("#send_event_email").data("event", $(this).data("event"));
        $("#send_event_email").modal("show");
    });
    
    parent.find(".edit").click(function() {
        parent.modal("hide");
        globals.editEvent($(this).attr("href"));
        return false;
    });
    
    parent.find(".editRegion").click(function() {
        parent.modal("hide");
        globals.startRegionEditing($(this).data("event"));
        return false;
    });
    
    parent.find(".panto").click(function() {
        window.location = globals.route.Application.index({event: $(this).attr("rel"), rfi:''});
    });
}
