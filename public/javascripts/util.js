// Global utils, also some suggestion parsing for search functions

var globals = globals || {};

if ( typeof(console) === "undefined") {
    console = {};
    console.log = function() {};
}

if (!String.prototype.trim) {
    String.prototype.trim = function() {
        return this.replace(/^\s+|\s+$/g, '');
    };
}

if (!String.prototype.trimTo) {
    String.prototype.trimTo = function(to) {
        if ( this.length > to) {
            return this.substring(0, to) + "...";
        }
        else {
            return this;
        }
    };
}

globals.getCenter = function(region) {
    var center = {lat: 0, lng: 0};
    for ( var i = 0; i < region.length; i++) {
        center.lat += region[i].lat;
        center.lng += region[i].lng;
    }
    center.lat /= region.length;
    center.lng /= region.length;
    return center;
}

globals.addToast = function(msg, type) {
    var t = _.template(templates.toast);
    var $toAdd = $(t({msg:msg, type:type}));
    $("#toast").append($toAdd);
    $toAdd.delay(5000).fadeOut("slow", function() { $(this).remove();});
}

globals._parseRFISuggestions = function(res) {
    var suggestions = [];
    $.each(res, function(i,val) {
        var coord = 0;
        if ( val.coordinates.lat) {
            coord = {
                lat: val.coordinates.lat,
                lng: val.coordinates.lon
            };
        }
        else if ( val.region) {
            coord = { region: JSON.parse(val.region)};
        }
        else if ( val.polyline) {
            coord = { region: JSON.parse(val.polyline)};
        }
        
        if ( coord || val.nonSpatial) {
            suggestions.push({
                label: val.title + " (RFI)",
                value: val.title,
                otherdata: {
                    id: val.id,
                    nonSpatial: val.nonSpatial,
                    pos: coord,
                    networkID: val.networkID
                }
            });
        }
    });
    return suggestions;
}

globals._parseGEONAMESSuggestions = function(res) {
    var suggestions = [];
    
    if ( res.totalResultsCount != 0) {
        $.each(res.places, function(i, val) {
            suggestions.push({
                label: val.name + " (" + val.country + ")",
                value: val.name,
                otherdata: {
                    pos: {
                        lat: val.point.split(" ")[0],
                        lng: val.point.split(" ")[1]
                    },
                    countryCode: val.country
                }
            });
        });
    }
    else {
        suggestions.push({label: "No GEONAMES results found", value: ""});
    }
    return suggestions;
}

globals.getGEONAMES = function(req,add) {
    var suggestions = [];

    $.ajax({url: globals.route.Services.geonames + "/" + escape(req.term),
        type: "GET",
        timeout: 10000
    })
    .done(function(res,status,xhr) {
        var toDump = res;
        add(globals._parseGEONAMESSuggestions(toDump));
    })
    .fail(function() {
        add([{label: "Error retrieving data from GEONAMES", value: ""}]);
    });
}

globals._geonames = function(req) {
    var ret = $.ajax({url: globals.route.Services.geonames + "/" + escape(req.term),
        type: "GET",
        timeout: 10000
    });
    
    function callback(req, def, suggestions) {
        if ( def.state() == "resolved") {
            var toDump = JSON.parse(def.responseText);
        
            $.each(globals._parseGEONAMESSuggestions(toDump), function(i,val) {
                suggestions.push(val);
            });
        }
        else {
            suggestions.push({label: "Error retrieving data from GEONAMES", value: ""});
        }
    }
    
    ret.deferredCallback = callback;
    return ret;
}

globals._rfis = function rfis(req) {
    var ret = $.ajax({url: globals.route.Services.rfisearch({searchKey: req.term, filter: req.filter || ""}),
        type: "GET",
        timeout: 10000
    });
    
    function callback(req, def, suggestions) {
        if ( def.state() == "resolved") {
            var res = JSON.parse(def.responseText);
            if ( res.length == 0) {
                suggestions.push({label:"No RFIs meet your search criteria", value: ""});
            }
            else {
                $.each(globals._parseRFISuggestions(res), function(i,val) {
                    suggestions.push(val);
                });
            }
        }
        else {
            suggestions.push({label: "Error retrieving RFI search data", value: ""});
        }
    }
    
    ret.deferredCallback = callback;
    return ret;
}
    
globals._geocode = function(req) {
    var dfd = new $.Deferred();
    
    var geocoder = new google.maps.Geocoder();
    
    var ret = {};
    
    geocoder.geocode( {'address': req.term}, function(results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            ret.geocoding = { ok: true, results: results};
            dfd.resolve(ret);
        } else {
            ret.geocoding = { ok: false, status: status};
            dfd.reject();
        }
    });
    
    function callback(req, def, suggestions) {
        if ( def.state() == "resolved") {
            var ret = def.geocoding;
            if ( ret.ok) {
                $.each(ret.results, function(i,val) {
                    var state = "";
                    var country = "";
                    var city = "";
                    var zip = "";
                    var street = "";
                    $.each(val.address_components, function(i,val) {
                        if ( $.inArray("administrative_area_level_1", val.types) >= 0) {
                            state = val.short_name;
                        }
                        if ( $.inArray("country", val.types) >= 0) {
                            country = val.short_name;
                        }
                        if ( $.inArray("locality", val.types) >= 0) {
                            city = val.short_name;
                        }
                        if ( $.inArray("postal_code", val.types) >= 0) {
                            zip = val.short_name;
                        }
                        if ( $.inArray("street_number", val.types) >= 0) {
                            street += val.short_name + " ";
                        }
                        if ( $.inArray("route", val.types) >= 0) {
                            street += val.short_name;
                        }
                    });
                
                    suggestions.push({
                        label: val.formatted_address,
                        value: req.term,
                        otherdata: {
                            pos: {
                                lat: val.geometry.location.lat(),
                                lng: val.geometry.location.lng()
                            },
                            countryCode: country,
                            stateCode: state,
                            city: city,
                            zip: zip,
                            street: street
                        }
                    });
                });
            }
        }
    }
    
    var r = dfd.promise(ret);
    ret.deferredCallback = callback;
    
    return r;
}

globals.search = function(req, add) {
    var suggestions = [];
    var deferreds = [];
    var sources = this.options && this.options.customSources;
    req.filter = this.options && this.options.filter;
    
    if ( sources) {
        $.each(sources, function(i,val) {
            deferreds.push(val(req));
        });
    }
    else {
        add([{label: "No sources defined", value:""}]);
        return;
    }
    
    $.when.apply(null, deferreds)
    .done(function() { })
    .always(
        function() {
            $.each(deferreds, function(idx, val) {
                if ( val.state() == "resolved") {
                    val.deferredCallback(req, val, suggestions);
                }
            });
            
            add(suggestions);
        }
    );
}

globals.shimInputs = function($parent) {
    var elem = document.createElement('input');
    elem.setAttribute('type', 'date');

    if ( elem.type === 'text' ) { 
        $parent.find("input[type=date]").datepicker({dateFormat: 'yy-mm-dd'}); 
    }

    $parent.find("input[placeholder], textarea[placeholder]").each(function() {
        $(this).watermark($(this).attr("placeholder"), { useNative: false});
    });
    
    $parent.find("select[data-select2enabled]").select2();
};

globals.rfiFormSetup = function($parent) {
    globals.shimInputs($parent);
    
    $parent.find(".selectMove").click(function() {
        var $t = $(this);
        var $src = $(_.template(".<%= src %> option:selected", {src: $t.data("src")}));
        var $dst = $(_.template(".<%= dest %>", { dest: $t.data("dest")}));
        $src.remove();
        $src.each(function() {
            var $me = $(this);
            var pos = $me.data("sort");
            var $higherItems = $dst.children().filter(function() {
                return $(this).data("sort") > pos;
            });
            if ( $higherItems.length == 0) {
                $dst.prepend($me);
            }
            else {
                $me.insertAfter($higherItems.last());
            }
        });
        return false;
    });
    
    var loadSMTSKeywords = function() {
        var $s = $parent.find(".availableSMTSKeywords");
        var $d = $parent.find(".destinationSMTSKeywords");
        var category = $parent.find("select[name='r.event.id'] option:selected").data("smts");
        var $status = $parent.find(".smtsStatus");
        var curKeywords = $parent.find("input[name='smtsKeywords']").val().split(", ");
        var $inp = $parent.find(".inp");
        
        $status.empty();
        $s.empty();
        $d.empty();
        $inp.hide();
        $status.show();
        
        if ( category) {
            var url = globals.SMTS_URL.replace("{EVENT}", category);
            var keywords = [];
            $status.append("Loading SMTS keywords for this event...");
            $.ajax({url:url, dataType:"jsonp", crossDomain: true})
            .done(function(res, status, xhr) {
                var features = _.where(res["#children"], {"#name": "featureMember"});
                keywords = _.map(features, function(f) {
                    return {text: f["#children"][0]["#children"][0]["#text"], hits: f["#children"][0]["#children"][2]["#text"]};
                });
                
                $status.empty();
                if ( _.isEmpty(keywords)) {
                    $status.append(_.template("No keywords for this event in SMTS (SMTS category = <%= category %>)", { category: category}));
                }
                else {
                    $status.hide();
                    $inp.show();
                    var $sout = $("<div/>");
                    var $dout = $("<div/>");
                    var t = "<option data-sort='<%= uses %>' value='<%= k %>'><%= k %> (<%= uses %> uses)</option>";
                    _.each(curKeywords, function(k) {
                        var mat = _.first(_.where(keywords, {text: k}));
                        if ( mat) {
                            $dout.append(_.template(t, { k: mat.text, uses: mat.hits}));
                        }
                    });
                    _.each(keywords, function(k) {
                        if ( !_.contains(curKeywords, k.text)) {
                            $sout.append(_.template(t, { k: k.text, uses: k.hits}));
                        }
                    });
                    $d.append($dout.find("option"));
                    $s.append($sout.find("option"));
                }
            })
            .fail(function() {
                $status.append("Unable to connect to SMTS server");
            });
        }
        else {
            $status.append("Event does not have an associated SMTS category: no keywords available for this event");
        }
    };
    
    var loadGeoQProject = function() {
        var project = $parent.find("select[name='r.event.id'] option:selected").data("geoq-project");
        if ( !project) {
            $parent.find(".createGeoQ").hide();
            $parent.find(".geoqJobId input").hide();
            $parent.find(".geoqJobId p").show();
        }
        else {
            $parent.find(".createGeoQ").show();
            $parent.find(".geoqJobId input").show();
            $parent.find(".geoqJobId p").hide();
        }
    };
    
    var loadEventActivities = function() {
        var evtId = $parent.find("select[name='r.event.id'] option:selected").val();
        var cur = $parent.find("input[name='eventActivity']").val();
        $.ajax({url:globals.route.EventController.get({id:evtId}), type:"GET"})
        .done(function(res,status,xhr) {
            var $event = $(".control-group.event");
            var $e = $event.find("select[name='r.eventActivity.id']");
            $e.empty();
            if ( !_.isEmpty(res.eventActivities)) {
                $event.find(".controls .activity").removeClass("hide");
                // Note: IE doesn't like prepending...
                $e.append(_.template("<option value=''<% if (selected) print(' selected'); %>>Select activity...</option>", { selected: !cur}));
                _.each(res.eventActivities, function(e) {
                    $e.append(_.template("<option value='<%= id %>'<% if (selected) print(' selected'); %>><%= name %></option>", {id:e.id, name:e.name, selected:e.id == cur}));
                });
            }
            else {
                $event.find(".controls .activity").addClass("hide");
                $e.append("<option selected value=''>No activites for this event...</option>");
            }
            $e.trigger("change");
        });
    };
    
    $parent.find(".selectMove").click(function() {
        var $t = $(this);
        var $src = $(_.template(".<%= src %> option:selected", {src: $t.data("src")}));
        var $dst = $(_.template(".<%= dest %>", { dest: $t.data("dest")}));
        $src.remove();
        $src.each(function() {
            var $me = $(this);
            var pos = $me.data("sort");
            var $higherItems = $dst.children().filter(function() {
                return $(this).data("sort") > pos;
            });
            if ( $higherItems.length == 0) {
                $dst.prepend($me);
            }
            else {
                $me.insertAfter($higherItems.last());
            }
        });
        return false;
    });
    
    var loadSMTSKeywords = function() {
        var $s = $parent.find(".availableSMTSKeywords");
        var $d = $parent.find(".destinationSMTSKeywords");
        var category = $parent.find("select[name='r.event.id'] option:selected").data("smts");
        var $status = $parent.find(".smtsStatus");
        var curKeywords = $parent.find("input[name='smtsKeywords']").val().split(", ");
        var $inp = $parent.find(".inp");
        
        $status.empty();
        $s.empty();
        $d.empty();
        $inp.hide();
        $status.show();
        
        if ( category) {
            var url = globals.SMTS_URL.replace("{EVENT}", category);
            var keywords = [];
            $status.append("Loading SMTS keywords for this event...");
            $.ajax({url:url, dataType:"jsonp", crossDomain: true})
            .done(function(res, status, xhr) {
                var features = _.where(res["#children"], {"#name": "featureMember"});
                keywords = _.map(features, function(f) {
                    return {text: f["#children"][0]["#children"][0]["#text"], hits: f["#children"][0]["#children"][2]["#text"]};
                });
                
                $status.empty();
                if ( _.isEmpty(keywords)) {
                    $status.append(_.template("No keywords for this event in SMTS (SMTS category = <%= category %>)", { category: category}));
                }
                else {
                    $status.hide();
                    $inp.show();
                    var $sout = $("<div/>");
                    var $dout = $("<div/>");
                    var t = "<option data-sort='<%= uses %>' value='<%= k %>'><%= k %> (<%= uses %> uses)</option>";
                    _.each(curKeywords, function(k) {
                        var mat = _.first(_.where(keywords, {text: k}));
                        if ( mat) {
                            $dout.append(_.template(t, { k: mat.text, uses: mat.hits}));
                        }
                    });
                    _.each(keywords, function(k) {
                        if ( !_.contains(curKeywords, k.text)) {
                            $sout.append(_.template(t, { k: k.text, uses: k.hits}));
                        }
                    });
                    $d.append($dout.find("option"));
                    $s.append($sout.find("option"));
                }
            })
            .fail(function() {
                $status.append("Unable to connect to SMTS server");
            });
        }
        else {
            $status.append("Event does not have an associated SMTS category: no keywords available for this event");
        }
    };
    
    var loadGeoQProject = function() {
        var project = $parent.find("select[name='r.event.id'] option:selected").data("geoq-project");
        if ( !project) {
            $parent.find(".createGeoQ").hide();
            $parent.find(".geoqJobId input").hide();
            $parent.find(".geoqJobId p").show();
        }
        else {
            $parent.find(".createGeoQ").show();
            $parent.find(".geoqJobId input").show();
            $parent.find(".geoqJobId p").hide();
        }
    };
    
    $parent.find("select[name='r.event.id']").unbind("change");
    $parent.find("select[name='r.event.id']").bind("change", function() {
        loadSMTSKeywords();
        loadGeoQProject();
        loadEventActivities();
    });
    $parent.find("select[name='r.event.id']").trigger("change");
    
    $parent.find(".productURLs > a, .webLinks > a").unbind("click");
    $parent.find(".productURLs > a, .webLinks > a").bind("click", function() {
        var $ths = $(this);
        var inp = $ths.parent().find("input").val();
        var rfiID = $parent.find("input[name='r.id']").val();
        
        $.ajax({type:"POST", url: $ths.attr("rel"), data: {rel: { text: inp, rfi: {id: rfiID}}}})
        .done(function(res,status,xhr) {
            var warning = xhr.getResponseHeader("FormErrors");
            if ( warning) {
                alert("Error adding your product/weblink (Did you put some text in the associated input field?)");
            }
            else {
                $ths.parent().find("input").val("");
                $ths.parent().find(".alert").prepend(_.template(templates.relatedItemTemplate, { rel: res}));
                $ths.parent().find(".alert").removeClass("hide");
            }
        });
        return false;
    });
};
