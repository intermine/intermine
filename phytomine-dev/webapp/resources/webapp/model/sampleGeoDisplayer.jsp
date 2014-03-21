<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<style>
  #sample-geo-map { height: 160px; }
</style>

<!-- sampleGeoDisplayer.jsp -->
<div id="sample-geo-display" class="collection-table" >

<h3> Sample Collection Location </h3>

<script type="text/javascript">
   function ShowMap() {
   google.load("maps", "3", {other_params:"sensor=false", "callback":loadGoogleMap});
   function loadGoogleMap() {
    var mapOptions = {
      center: new google.maps.LatLng('${latitude}','${longitude}'),
      zoom: 11,
      mapTypeId: google.maps.MapTypeId.TERRAIN
    };
    var map = new google.maps.Map(document.getElementById("sample-geo-map"),mapOptions);
    var mapMarker = new google.maps.Marker( {
      position: map.getCenter(),
      map: map,
      title: '${name}' });
    }
    }

    ShowMap();
</script>

<div id="sample-geo-map" class="map">
</div>
</div>


<!-- /sampleGeoDisplayer.jsp -->
