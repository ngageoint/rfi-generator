// All map experience-related stuff...mostly global functions that are helpers around
// higher-level concepts like "open the RFI edit window", etc, etc

// Layer styles for openlayers, color/etc are NOT included in this and computed when RFIs/event are rendered
globals.layer_style = OpenLayers.Util.extend({ }, OpenLayers.Feature.Vector.style['default']);
globals.layer_style.cursor = "pointer";
globals.non_spatial_layer_style = OpenLayers.Util.extend({}, OpenLayers.Feature.Vector.style['default']);
globals.non_spatial_layer_style.fillOpacity = 0.0;
globals.non_spatial_layer_style.strokeOpacity = 0.0;

globals.DrawPolygon = OpenLayers.Class(OpenLayers.Control.DrawFeature, {
    initialize: function(layer, options) {
        OpenLayers.Control.DrawFeature.prototype.initialize.apply(
            this,
            [layer, OpenLayers.Handler.Polygon, options]
        );
    },
    activate: function(pt) {
        OpenLayers.Control.DrawFeature.prototype.activate.apply(this);
        this.startPoint = pt;
    }
});

globals.DrawPolyline = OpenLayers.Class(OpenLayers.Control.DrawFeature, {
    initialize: function(layer, options) {
        OpenLayers.Control.DrawFeature.prototype.initialize.apply(
            this,
            [layer, OpenLayers.Handler.Path, options]
        );
    },
    activate: function(pt) {
        OpenLayers.Control.DrawFeature.prototype.activate.apply(this);
        this.startPoint = pt;
    }
});

OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {                
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'pixelTolerance': 0,
        'stopSingle': false,
        'stopDouble': false
    },

    initialize: function(options) {
        this.handlerOptions = OpenLayers.Util.extend(
            {}, this.defaultHandlerOptions
        );
        OpenLayers.Control.prototype.initialize.apply(
            this, arguments
        ); 
        this.handler = new OpenLayers.Handler.Click(
            this, {
                'click': this.onClick,
                'dblclick': this.onDblclick 
            }, this.handlerOptions
        );
    }, 

    onClick: function(evt) {
        // Ugly
        if ( !$("a.exitRegionMode").hasClass("shown")) {
            globals.unselectAll();
            globals.closeClickPopup();
            var ll = globals.map.getLonLatFromPixel(evt.xy);
            // Ghetto
            var ll2 = globals.map.getLonLatFromPixel(new OpenLayers.Pixel(evt.xy.x + 6, evt.xy.y));
            var pt = new OpenLayers.Geometry.Point(ll.lon, ll.lat);
            var rfis = _.first(globals.map.getLayersByName("RFIs"));
            var hits = [];
            
            var clickPoly = OpenLayers.Geometry.Polygon.createRegularPolygon(pt, ll2.lon-ll.lon, 20, 0);

            _.each(rfis.features, function(f) {
                if ( f.geometry.intersects(clickPoly)) {
                    hits.push(f);
                }
            });
            
            globals.showClickPopup(ll, hits);
        }
    },
    
    onDblClick: function(evt) {
    }
});

globals.onRFISelected = function(evt, ll) {
    var f = evt.feature;
    globals.closeClickPopup();
    
    var curRFI = f.rfi;
    var t = _.template(templates.rfiPopup);
    var pt = f.geometry.getBounds().getCenterLonLat();
    
    if ( ll) {
        pt = ll;
    }
    
    var popup = new OpenLayers.Popup.FramedCloud("RFI",
        pt,
        new OpenLayers.Size(100,100),
        t(curRFI),
        null, true, function(e) {
            globals.unselectAll();
        });
    
    f.popup = popup;
    globals.map.addPopup(popup);
};
    
globals.onRFIUnselected = function(evt) {
    var f = evt.feature;
    if ( f.popup) {
        globals.map.removePopup(f.popup);
        f.popup.destroy();
        delete f.popup;
    }
};

globals.selectRFI = function(rfi_id, lat, lng) {
    var l = _.first(globals.map.getLayersByName("RFIs"));
    var f = _.first(_.where(l.features, {rfi_id: rfi_id}));
    var r = _.first(_.where(globals.rfis, { id: rfi_id}));
    // Redo this w/ stylemaps
    if ( !r.nonSpatial) {
        f.style.fillOpacity = 1.0;
        f.style.strokeOpacity = 1.0;
    }
    globals.onRFISelected({feature: f}, new OpenLayers.LonLat(lng, lat));
    l.redraw();
};

globals.unselectAll = function() {
    // Fudge
    var l = _.first(globals.map.getLayersByName("RFIs"));
    _.each(l.features, function(f) {
        if ( f.rfi.nonSpatial) {
            f.style.fillOpacity = globals.non_spatial_layer_style.fillOpacity;
            f.style.strokeOpacity = globals.non_spatial_layer_style.strokeOpacity;
        }
        else {
            f.style.fillOpacity = globals.layer_style.fillOpacity;
            f.style.strokeOpacity = globals.layer_style.strokeOpacity;
        }
        globals.onRFIUnselected({feature:f});
    });
    l.redraw();
};

globals.showClickPopup = function(ll, hits) {
    var t = _.template(templates.mapClick);
    globals.map.clickPopup = new OpenLayers.Popup.FramedCloud("add",
        ll,
        new OpenLayers.Size(100,100),
        t({lat: ll.lat, lng: ll.lon, hits: hits, ll:ll}),
        null, true, function() {
            globals.map.removePopup(globals.map.clickPopup);
            globals.map.clickPopup.destroy();
            delete globals.map.clickPopup;
        });
    globals.map.addPopup(globals.map.clickPopup);
};

globals.closeClickPopup = function() {
    var p = globals.map.clickPopup;
    if ( p) {
        globals.map.removePopup(p);
        p.destroy();
        delete globals.map.clickPopup;
    }
};

globals._sanitizeModal = function($modal) {
    $modal.removeData("point");
    $modal.removeData("region");
    $modal.removeData("polyline");
    $modal.removeData("nonspatial");
}

globals._updateModalContent = function($modal, c) {
    var cur = $modal.find(".modal-body");
    cur.empty();
    cur.append(c);
    cur.scrollTop(0);
}

globals._polylineStroke = function(zoom) {
    return Math.ceil((Math.sqrt(zoom)-3)*10);
}

globals.showNewModal = function(lat, lng) {
    var cur = $("#new_rfi_modal");
    globals._sanitizeModal(cur);
    cur.data("point", JSON.stringify({lat: lat, lng: lng}));
    cur.modal("show");
}

globals.getZoom = function(accuracy) {
    return Math.round(1/Math.pow(accuracy,0.1)*15+6);
}

globals.moveToRFI = function(rfi) {
    if ( rfi.region && rfi.region.toString() != "") {
        globals.moveTo({region: rfi.region});
    }
    else if ( rfi.polyline) {
        globals.moveTo({polyline: rfi.polyline});
    }
    else if (rfi.coordinates.lat || rfi.coordinates.lon) {
        globals.moveTo({lat: rfi.coordinates.lat, lng: rfi.coordinates.lon});
    }
};

globals.moveTo = function(regionOrPoint) {
    var r = regionOrPoint;
    if (r.region && r.region.toString() != "") {
        var region = r.region;
        if ( typeof(region) == "string") {
            region = JSON.parse(region);
        }
        
        var geom = globals.jsonPolyToGeometry(region);
        globals.map.zoomToExtent(geom.getBounds());
    }
    else if (r.polyline) {
        var polyline = r.polyline;
        if ( typeof(polyline) == "string") {
            polyline = JSON.parse(polyline);
        }
        
        var geom = globals.jsonPolylineToGeometry(polyline);
        globals.map.zoomToExtent(geom.getBounds());
    }
    else if (r.lat || r.lng) {
        var ct = new OpenLayers.LonLat(r.lng, r.lat).transform(globals.map.displayProjection, globals.map.projection);
        globals.map.setCenter(ct, 11);
    }
}

function newRFIFromRegion(f) {
    var $modal = $("#new_rfi_modal");
    globals._sanitizeModal($modal);
    var arr = [];
    _.each(f.geometry.getVertices(), function(p) {
        arr.push({lat: p.y, lng: p.x});
    });
    $modal.data("region", JSON.stringify(arr));
    $modal.modal("show");
    globals.exitRegionMode();
}

function newEventFromRegion(f) {
    var $modal = $("#new_event_modal");
    globals._sanitizeModal($modal);
    window.f = f;
    var arr = [];
    _.each(f.geometry.getVertices(), function(p) {
        arr.push({lat: p.y, lng: p.x});
    });
    $modal.data("region", JSON.stringify(arr));
    $modal.modal("show");
    globals.exitRegionMode();
}

function newRFIFromPolyline(f) {
    var $modal = $("#new_rfi_modal");
    globals._sanitizeModal($modal);
    var arr = [];
    _.each(f.geometry.getVertices(), function(p) {
        arr.push({lat: p.y, lng: p.x});
    });
    $modal.data("polyline", JSON.stringify(arr));
    $modal.modal("show");
    globals.exitRegionMode();
}

function newNonSpatial($modal) {
    globals._sanitizeModal($modal);
    $modal.data("nonspatial", 1);
    $modal.modal("show");
}

globals.startPointCreation = function() {
    $('a.exitRegionMode').addClass("shown");
    $("a.drawPoint").addClass("active");
    globals.closeClickPopup();
    globals.drawPoint.activate();
    return false;
}

globals.startPolylineDrawing = function(lat,lng) {
    $('a.exitRegionMode').addClass("shown");
    $("a.drawPolyline").addClass("active");
    globals.closeClickPopup();
    globals.drawPolyline.activate(lat && lng ? {lat:lat, lng:lng}:null);
    return false;
}

function updateRegion(id) {
    if ( confirm("Update this event's polygon?")) {
        var region = [];
        globals.path.forEach(function(e,idx) {
            region.push({lat: e.lat(), lng: e.lng()});
        });
        
        $.ajax({url: globals.route.EventController.updateRegion({id: id, region: JSON.stringify(region)}), type: "POST"})
        .done(function(res,status,xhr) {
            globals.refreshEvents();
            globals.exitRegionMode();
            globals.addToast("Event's polygon has been updated", "success");
        });
    }
}

globals.startRegionEditing = function(id) {
    var itm = _.find(globals.events, function(e) {
        return e.id == id;
    });
    
    if ( !itm) {
        if ( confirm("Cannot edit a polygon for an event that isn't in the current view context...switch to a global context (you will need to edit the polygon after the page refreshes)?")) {
            window.location = globals.route.root + "?noRedirect=true";
        }
        else {
            return;
        }
    }
    
    globals.moveTo({region: itm.region});
    
    var l = globals.map.getLayersByName("Events")[0];
    var sketch = globals.map.getLayersByName("sketch")[0];
    var g = _.findWhere(l.features, {event_id: id}).geometry.clone();
    
    // Create feature in sketch
    sketch.addFeatures([new OpenLayers.Feature.Vector(g)]);
    globals.mapClick.deactivate();
    globals.modify.activate();
}

globals.startRFIPolygonDrawing = function(lat, lng) {
    $('a.exitRegionMode').addClass("shown");
    $("a.drawPoly").addClass("active");
    globals.closeClickPopup();
    globals.drawPolygon.activate(lat && lng ? {lat:lat, lng:lng}:null);
    return false;
}

globals.startEventPolygonDrawing = function(lat, lng) {
    $('a.exitRegionMode').addClass("shown");
    globals.closeClickPopup();
    globals.drawEventPolygon.activate(lat && lng ? {lat:lat, lng:lng}:null);
    return false;
}

globals.deleteRFI = function(id, admin) {
    if ( confirm("Are you sure you want to delete this RFI?")) {
        $.ajax({type:"DELETE", url: globals.route.RFIController.deleteRFI({id:id})})
        .done(function(res,status,xhr) {
            if ( !admin) {
                globals.closeClickPopup();
                globals.closeRFIPopups();
                console.log("RFI deleted");
                globals.refreshRFIs().done(function(res, status, xhr) {
                    globals.rfis = res;
                    globals._refreshRFIs();
                });
                globals.addToast("RFI deleted", "error");
            }
            else {
                window.location = globals.route.Admin.index();
            }
        });
    }
}

globals.editRFI = function(id) {
    $.ajax({type:"GET", url: globals.route.RFIController.edit({id:id})})
    .done(function(res, status, xhr) {
        var cur = $("#edit_rfi_modal");
        globals._updateModalContent(cur,res);
        cur.modal("show");
    });
}

globals.adminEditRFI = function(id) {
    window.location = globals.route.Admin.editrfi({id:id});
}

globals.editEvent = function(URL) {
    $.ajax({type:"GET", url: URL})
    .done(function(res, status, xhr) {
        var cur = $("#edit_event_modal");
        globals._updateModalContent(cur,res);
        cur.modal("show");
    });
}

globals.editUser = function(URL) {
    $.ajax({type:"GET", url:URL})
    .done(function(res,status,xhr) {
        var cur = $("#edit_user_modal");
        globals._updateModalContent(cur,res);
        cur.modal("show");
    });
}

globals.commentRFI = function(id, callback) {
    $.ajax({type:"GET", url: globals.route.RFIController.comment({id:id})})
    .done(function(res, status, xhr) {
        var cur = $("#comment_rfi_modal");
        globals._updateModalContent(cur,res);
        cur.modal("show");
        cur.on("hidden", function() {
            
        });
        if ( callback) {
            cur.on("hidden", callback);
        }
    });
}

globals.exitRegionMode = function() {
    $(".exitRegionMode").removeClass("shown");
    $(".createRegionRFI").removeClass("shown");
    $(".createPolylineRFI").removeClass("shown");
    $(".createRegionEvent").removeClass("shown");
    $("a.drawPoly").removeClass("active");
    $("a.drawPolyline").removeClass("active");
    $("a.drawPoint").removeClass("active");
    globals.closeClickPopup();
    globals.unselectAll();
    globals.drawPolygon.deactivate();
    globals.drawEventPolygon.deactivate();
    globals.drawPolyline.deactivate();
    globals.drawPoint.deactivate();
    globals.map.getLayersByName("sketch")[0].removeAllFeatures();
}

globals.jsonPolyToGeometry = function(pts) {
    var pointList = [];
    _.each(pts, function(pt) {
        pointList.push(new OpenLayers.Geometry.Point(pt.lng, pt.lat));
    });
    var lr = new OpenLayers.Geometry.LinearRing(pointList);
    pointList.push(pointList[0]);
    return lr;
}

globals.jsonPolylineToGeometry = function(pts) {
    var pointList = [];
    _.each(pts, function(pt) {
        pointList.push(new OpenLayers.Geometry.Point(pt.lng, pt.lat));
    });
    var ls = new OpenLayers.Geometry.LineString(pointList);
    return ls;
}

globals.polyToFeature = function(pts) {
    var lr = globals.jsonPolyToGeometry(pts);
    
    var f = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Polygon([lr]), {}, OpenLayers.Util.extend({}, globals.layer_style));
    // You wouldn't think you'd need to do a manual projection...
    f.geometry.transform(globals.map.displayProjection, globals.map.projection);
    return f;
}

globals.polylineToFeature = function(pts) {
    var ls = globals.jsonPolylineToGeometry(pts);
    var f = new OpenLayers.Feature.Vector(ls, {}, OpenLayers.Util.extend({}, globals.layer_style));
    f.geometry.transform(globals.map.displayProjection, globals.map.projection);
    return f;
}

globals.closeRFIPopups = function() {
    //globals.unselectAll();
};

globals._refreshEvents = function(pan) {
    var events = _.first(globals.map.getLayersByName("Events"));
    var id = globals.session.event;
    _.each(globals.events, function(e) {
        if ( e.region) {
            var pts = JSON.parse(e.region);
            if ( pts.length > 0) {
                var f = globals.polyToFeature(pts);
                f.event_id = e.id;
                events.addFeatures([f]);
                f.style.fillColor = "#ff0000";
                f.style.fillOpacity = 0.1;
                f.style.strokeDashstyle = "dash";
                f.style.strokeWidth = 1;
                f.style.cursor = "default";
                if ( id && pan) {
                    var pts = [];
                    pts.push(JSON.parse(e.region));
                    _.each(globals.rfis, function(e) {
                        if ( e.event.id == id) {
                            hasRFI = true;
                            if ( e.region) {
                                var r = JSON.parse(e.region);
                                _.each(r, function(e) {
                                    pts.push({lat: e.lat, lng: e.lng});
                                });
                            }
                            else if ( e.polyline) {
                                var r = JSON.parse(e.polyline);
                                _.each(r, function(e) {
                                    pts.push({lat: e.lat, lng: e.lng});
                                });
                            }
                            else if ( e.nonSpatial) {
                                // Do nothing
                            }
                            else {
                                pts.push({lat: e.coordinates.lat, lng: e.coordinates.lon});
                            }
                        }
                    });
                    globals.moveTo({region: _.flatten(pts)});
                }
            }
            else if ( id && pan) {
                globals.map.setCenter(new OpenLayers.LonLat(-91,38), 4);
            }
        }
    });
    events.redraw();
};

globals.refreshEvents = function() {
    var route = globals.route.EventController.getAll;
    var id = globals.session.event;
    if ( id) {
        route += "?event=" + id;
    }
    else {
        route += "?active=true";
    }
    var events = _.first(globals.map.getLayersByName("Events"));
    events.removeAllFeatures();
    return $.ajax({url: route, type: "GET"});
};

globals._refreshRFIs = function() {
    var rfis = _.first(globals.map.getLayersByName("RFIs"));
    var nonSpatialDrawn = false;
    _.each(globals.rfis, function(r) {
        if ( r.region) {
            var pts = JSON.parse(r.region);
            var f = globals.polyToFeature(pts);
            f.rfi = r;
            f.rfi_id = r.id;
            rfis.addFeatures([f]);
            f.style.fillColor = globals.status_colors(r.status);
            f.style.strokeColor = "#000000";
            f.style.strokeWidth = 1;
            f.style.strokeOpacity = 0.7;
        }
        else if ( r.polyline) {
            var pts = JSON.parse(r.polyline);
            var f = globals.polylineToFeature(pts);
            f.rfi = r;
            f.rfi_id = r.id;
            rfis.addFeatures([f]);
            f.style.strokeColor = globals.status_colors(r.status);
            f.style.strokeWidth = 2;
            f.style.strokeOpacity = 0.7;
        }
        else if ( r.coordinates && (r.coordinates.lat != 0.0 || r.coordinates.lon != 0.0)) {
            var pt = new OpenLayers.Geometry.Point(r.coordinates.lon, r.coordinates.lat);
            var projPt = new OpenLayers.LonLat(pt.x, pt.y).transform(globals.map.displayProjection, globals.map.projection);
            var style = OpenLayers.Util.extend({}, globals.layer_style);
            style.fillColor = globals.status_colors(r.status);
            style.strokeColor = "#666666";
            style.strokeOpacity = 0.7;
            var toAdd = new OpenLayers.Feature.Vector(new OpenLayers.Geometry.Point(projPt.lon, projPt.lat), {}, style);
            toAdd.rfi = r;
            toAdd.rfi_id = r.id;
            rfis.addFeatures([toAdd]);
        }
        else if ( r.nonSpatial && r.event.region != "[]") {
            var events = _.first(globals.map.getLayersByName("Events"));
            console.log(events);
            var f = _.first(_.where(events.features, {event_id: r.event.id}));
            var style = OpenLayers.Util.extend({}, globals.non_spatial_layer_style);
            var toAdd = new OpenLayers.Feature.Vector(f.geometry.clone(), {}, style);
            toAdd.rfi = r;
            toAdd.rfi_id = r.id;
            rfis.addFeatures([toAdd]);
        }
    });
    
    rfis.redraw();
};

globals.refreshRFIs = function(callback) {
    var layers = globals.map.getLayersByName("RFIs");
    globals.unselectAll();
    _.each(layers, function(l) {
        l.removeAllFeatures();
    });
    
    var route = globals.route.RFIController.getAll;
    if ( globals.session.event != "") {
        route += "?event=" + globals.session.event + "&hideArchived=1";
    }
    else {
        route += "?hideArchived=1";
    }
    return $.ajax({url: route, type: "GET"});
};
