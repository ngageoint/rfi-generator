// 99% (maybe even 100%) isn't currently used - all geocoding related/HTML5 geolocation API stuff

function revGeoCode(latlng){
    geocoder = new google.maps.Geocoder();
    geocoder.geocode( {'latLng': latlng}, function(results, status) {
        if (status == google.maps.GeocoderStatus.OK) {
            var arrAddress = results[0].address_components;
            var addr1 = " ";
            $.each(arrAddress, function (i, address_component) {
            switch(address_component.types[0]){
                case "street_number":
                    addr1 = address_component.long_name + addr1;
                    break;
                case "route":
                    addr1 = addr1 + address_component.long_name;
                    break;
                case "neighborhood":
                    $("input[name='r.address2']").val(address_component.long_name)
                case "locality":
                    $("input[name='r.cityName']").val(address_component.long_name)
                    break;
                case "postal_code":
                    $("input[name='r.zipcode']").val(address_component.long_name)
                    break;                    
                case "administrative_area_level_1":
                    $("select[name='r.state']").val(address_component.short_name);
                    break;
                case "country":
                    $("select[name='r.country']").val(address_component.short_name.toUpperCase());
                    break;                           
                }
            });
            $("input[name='r.address1']").val(addr1);
        } else {
            alert("Address lookup was not successful for the following reason: " + status);
        }
    });
};

function geoCode(searchstr)
{
    searchstr = searchstr.replace(/undefined/gi, " ");
    geocoder = new google.maps.Geocoder();

        geocoder.geocode( {'address': searchstr}, function(results, status) {
            if (status == google.maps.GeocoderStatus.OK) {
                populateCoords(results[0].geometry.location);
            } else {
                var err_msg = "";
                if(status == "ZERO_RESULTS"){
                    err_msg = "Geodoing was unsuccessful because there was no matching address.  ";
                    if( $("select[name='r.state']").val() == "")
                        err_msg = err_msg + "\n Try populating state ";
                    if( typeof $("select[name='r.zipcode']").val() === "undefined")
                        err_msg = err_msg + "\n Try populating zipcode ";
                }else {
                    err_msg = "Geocode was unsuccessful for the following reason: " + status;
                }

                alert (err_msg);
            }
    });
};

function getCurrentLocation(successCallback, errorCallback) {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position) {
            successCallback(position);
        }, function(error) {
            errorCallback(error);
        });
    }
}

function geoCodeCurrentLocation(){
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position) {
            latLng = new google.maps.LatLng(position.coords.latitude, position.coords.longitude);
            populateCoords (latLng);
            revGeoCode(latLng);
            
        }, function(error) {
            alert("Error occurred. Error code: " + error.code);
        });
    }
};
function populateCoords(latLng)
{
    $("input[name='r.coordinates.lat']").val(latLng.lat());
    $("input[name='r.coordinates.lon']").val(latLng.lng());          
}

