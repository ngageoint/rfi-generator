// Used for binding stuff on the RFI List in the map experience

function bindGrid(parent, doFilterOnStart) {
    var parent = $(parent);
    var gridRoute = parent.data("grid-route");
    var gridParams = {};
    
    // http://stackoverflow.com/questions/1184624/convert-form-data-to-js-object-with-jquery
    $.fn.serializeObject = function()
    {
        var o = {};
        var a = this.serializeArray();
        $.each(a, function() {
            if (o[this.name] !== undefined) {
                if (!o[this.name].push) {
                    o[this.name] = [o[this.name]];
                }
                o[this.name].push(this.value || '');
            } else {
                o[this.name] = this.value || '';
            }
        });
        return o;
    };
    
    var pick = function(o, idx) {
        if ( _.isArray(o)) {
            return o[idx];
        }
        else {
            return o;
        }
    };
    
    var conditionalExtend = function(obj1, obj2) {
        var ret = {};
        for ( var key in obj1) {
            ret[key] = obj1[key];
        }
        for ( var key in obj2) {
            ret[key] = obj1[key] || obj2[key];
        }
        return ret;
    };
        
    (function () {
        var e,
            a = /\+/g,
            r = /([^&=]+)=?([^&]*)/g,
            d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
            q = parent.find("input[name='params']").val();

        while (e = r.exec(q))
           gridParams[d(e[1])] = d(e[2]).split(",");
    })();
    
    function flattenArrays(obj) {
        var ret = {};
        for(var p in obj) {
            if ( _.isArray(obj[p])) {
                ret[p] = obj[p].join(",");
            }
            else {
                ret[p] = obj[p];
            }
        }
        return ret;
    }
    
    function serialize(obj) {
        var str = [];
        for(var p in obj) {
            str.push(p + "=" + encodeURIComponent(obj[p]));
        }
        return str.join("&");
    }
    
    parent.find('th.sortable a').append(_.template(templates.sortTemplate, globals));
    
    if (parent.find(".no-items").length > 0 ){
        parent.find("a.export").attr("disabled", "disabled");
        parent.find("a.export").attr("href", "#");
    }
    else{
        var rqry = parent.find("select[name='filterRequestor']").val();
        var dqry = parent.find("select[name='filterEvent']").val();
        var aqry = parent.find("select[name='filterAssignee']").val();
        var sqry = parent.find("input[name='filterSearch']").val();
        
        gridParams.filterRequestor = [rqry];
        gridParams.filterEvent = [dqry];
        gridParams.filterAssignee = [aqry];
        gridParams.filterSearch = [sqry];
    
        parent.find("a.export").removeAttr("disabled");
        parent.find("a.export").attr("href",
            globals.route.RFIController.exportXLS + "?" + serialize(flattenArrays(conditionalExtend(gridParams, parent.find("form").serializeObject(), 0))));
    }
    
    if ( gridParams["sortDirection"]) {
        for ( var i in gridParams["sortDirection"]) {
            var cur = parent.find('table[data-idx='+i+'] th.sortable a[rel='+pick(gridParams["sort"], i)+']');
            cur.closest(".sortable").addClass("sorted");
            if ( gridParams["sortDirection"][i] == "asc") {
                cur.closest(".sortable").addClass("asc");
            }
            else {
                cur.closest(".sortable").addClass("desc");
            }
        }
    }
    
    parent.find(".rfiBottom td").not(":first-child").linkify({target:"_blank"});
    
    parent.find(".filters a.clear").off("click");
    parent.find(".filters a.clear").on("click", function() {
        var rqry = parent.find("select[name='filterRequestor']");
        var eqry = parent.find("select[name='filterEvent']");
        var aqry = parent.find("select[name='filterAssignee']");
        rqry.val("");
        eqry.val("");
        aqry.val("");
        var $f = parent.find(".filters");
        $f.off("change", "select", dofilter);
        rqry.trigger("change");
        eqry.trigger("change");
        aqry.trigger("change");
        dofilter();
    });
    
    parent.find("#filterForm a.clear").unbind();
    parent.find("#filterForm a.clear").click(function() {
        parent.find("#filterForm input[type='text'], #filterForm input[type='date'], #filterForm select").val("");
        parent.find("#filterForm select").trigger("change");
        dofilter();
        return false;
    });
    
    parent.find("form").unbind();
    parent.find("form").bind("submit", function() {
        dofilter();
        return false;
    });
    
    parent.find("a.advSearch").unbind();
    parent.find("a.advSearch").click(function() {
        parent.find("div.advSearch").toggle();
        parent.toggleClass("advOpen");
    });
    
    parent.find(".modal-body").unbind();
    parent.find(".modal-body").bind("scroll", function() {
        parent.find(".details").popover("hide");
    });
    
    parent.find(".details").popover({trigger: "manual", placement: "right"});
    parent.on("click", function() {
        parent.find(".details").popover("hide");
    });
    parent.find(".details").click(function(e) {
        parent.find(".details").not($(this)).popover("hide");
        $(this).popover("toggle");
        e.stopPropagation();
    });
    
    var dofilter = function(idx) {
        var rqry = parent.find("select[name='filterRequestor']").val();
        var dqry = parent.find("select[name='filterEvent']").val();
        var aqry = parent.find("select[name='filterAssignee']").val();
        var sqry = parent.find("input[name='filterSearch']").val();
        
        var rinp = parent.find("input[name='filterRequestor']");
        var ainp = parent.find("input[name='filterAssignee']");
        var oinp = parent.find("input[name='filterOrganization']");
        gridParams.filterRequestor = [rqry];
        gridParams.filterEvent = [dqry];
        gridParams.filterAssignee = [aqry];
        gridParams.filterSearch = [sqry];
        if ( rqry) {
            rinp.attr("disabled", "disabled");
        }
        else {
            rinp.removeAttr("disabled");
        }
        if ( aqry) {
            ainp.attr("disabled", "disabled");
        }
        else {
            ainp.removeAttr("disabled");
        }
        var items = parent.find("form").serializeObject();
        var data = conditionalExtend(flattenArrays(gridParams), items, idx);
        $.ajax({type: "GET", url: gridRoute, data: flattenArrays(data)})
        .done(function(res, status, xhr) {
            parent.find(".modal-body").empty();
            parent.find(".modal-body").append(res);
            bindGrid(parent);
        });
    };
    
    parent.find(".filters").off("change", "select");
    parent.find(".filters").on("change", "select", dofilter);
    parent.find(".fullSearch").unbind();
    parent.find(".listSearch").unbind();
    parent.find(".listSearch").bind("submit", function() {
        dofilter();
    });
    parent.find(".fullSearch").click(function() {
        parent.find(".listSearch").submit();
        return false;
    });
    
    parent.find('th.sortable a').click(function() {
        var ths = $(this);
        var destSort = "asc";
        var rel = ths.attr("rel");
        var idx = $(this).closest("table").data("idx");
        gridParams.sort[idx] = rel;
        
        if ( gridParams["sortDirection"][idx] == "asc") {
            gridParams.sortDirection[idx] = "desc";
        }        
        else {
            gridParams.sortDirection[idx] = "asc";
        }
        
        dofilter(idx);
        return false;
    });
    
    parent.find(".edit").click(function() {
        parent.modal("hide");
        globals.editRFI($(this).data("rfi"));
        return false;
    });
    
    parent.find(".comment").click(function() {
        parent.modal("hide");
        globals.commentRFI($(this).data("rfi"));
        return false;
    });
    
    parent.find(".delete").click(function() {
        if ( confirm("Are you sure you want to delete this RFI?")) {
            $.ajax({url: globals.route.RFIController.deleteRFI({id:$(this).attr("href")}), type:"DELETE"})
            .done(function(res,status,xhr) {
                $.ajax({url: gridRoute, type: "GET", data: gridParams})
                .done(function(res,status,xhr) {
                    parent.find(".modal-body").empty();
                    parent.find(".modal-body").append(res);
                    bindGrid(parent);
                    globals.refreshRFIs().done(function(res,status,xhr) {
                        globals.rfis = res;
                        globals._refreshRFIs();
                    });
                });
            });
        }
        return false;
    });
    
    parent.find(".verify").click(function() {
        $.ajax({url: $(this).attr("href"), type: "POST"})
        .done(function(res,statux,xhr) {
            $.ajax({url: gridRoute, type: "GET", data: gridParams})
            .done(function(res,status,xhr) {
                parent.find(".modal-body").empty();
                parent.find(".modal-body").append(res);
                bindGrid(parent);
                globals.refreshRFIs().done(function(res,status,xhr) {
                    globals.rfis = res;
                    globals._refreshRFIs();
                });
            });
        });
        return false;
    });
    
    parent.find(".panto").click(function() {
        var ths = $(this);
        var data = ths.closest("tr");
        
        if ( globals.session.event && globals.session.event != data.data("event")) {
            if (confirm("Ok to switch to a global event context?")) {
                var route = globals.route.Application.index({rfi: data.data("rfi"), event:''});
                
                window.location = route;
            }
        }
        else {
            if ( ths.data("point-lat")) {
                globals.moveTo({lat: ths.data("point-lat"), lng: ths.data("point-lon")});
                parent.modal("hide");
            }
            else if (ths.data("polyline")) {
                parent.modal("hide");
                globals.moveTo({polyline: ths.data("polyline")});
            }
            else if (ths.data("region")) {
                parent.modal("hide");
                globals.moveTo({region: ths.data("region")});
            }
        }
    });
    
    if ( doFilterOnStart) {
        dofilter();
    }
}
