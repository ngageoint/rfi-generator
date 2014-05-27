// Used on the map experience for the user grid's events, etc

function bindUserGrid(parent) {
    var parent = $(parent);
    var gridParams = {};
        
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
        
        $.ajax({url: globals.route.UserManagement.grid, type: "GET", data: gridParams})
        .done(function(res,status,xhr) {
            parent.find(".modal-body").empty();
            parent.find(".modal-body").append(res);
            bindUserGrid(parent);
        });
        return false;
    });
    
    parent.find(".resetPassword").click(function() {
        var id= $(this).data("user");
        $.ajax({url: globals.route.UserManagement.resetPassword({id:id}), type: "POST"})
        .done(function(res,status,xhr) {
            if ( xhr.getResponseHeader("FormErrors")) {
                alert("Error encountered resetting user's password");
            }
            else {
                alert("Password reset email in route to " + res);
            }
        })
    });
    
    parent.find(".edit").click(function() {
        parent.modal("hide");
        globals.editUser($(this).attr("href"));
        return false;
    });
}
