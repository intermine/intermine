<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="mm"%>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- subListGBrowseTrackDisplayer.jsp -->

<script type="text/javascript" charset="utf-8">
<!--//<![CDATA[

    jQuery(document).ready(function(){

        if ("${tracks}" == "{}")
        {
          jQuery("#GBTitle").after("<div><i>no tracks</i></div>");
          return;
        }

        allChecked();
        // Get full url for fly and worm tracks
        jQuery("#D\\.\\ melanogaster_a").attr("href", getFullTrackURL("D. melanogaster"));
        jQuery("#C\\.\\ elegans_a").attr("href", getFullTrackURL("C. elegans"));

        //Hide (Collapse) the toggle containers on load
        jQuery(".toggle_container").show();

        //Switch the "Open" and "Close" state per click then slide up/down (depending on open/close state)
        jQuery("div.trigger").click(function(){
            jQuery(this).toggleClass("active").next().slideToggle("slow");
    });

    });

   // (un)Check all featureType checkboxes
   function checkAll(id)
   {
     if (id == "D. melanogaster_all")
     {
       jQuery("input[name$='D. melanogaster_each']").attr('checked', jQuery('#D\\.\\ melanogaster_all').is(':checked'));
       jQuery('#D\\.\\ melanogaster_all').css("opacity", 1);
     }
     if (id == "C. elegans_all")
     {
       jQuery("input[name$='C. elegans_each']").attr('checked', jQuery('#C\\.\\ elegans_all').is(':checked'));
       jQuery('#C\\.\\ elegans_all').css("opacity", 1);
     }
   }

   function allChecked()
   {
     jQuery('#D\\.\\ melanogaster_all').attr('checked', true);
     jQuery("input[name$='D. melanogaster_each']").attr('checked', true);
     jQuery('#C\\.\\ elegans_all').attr('checked', true);
     jQuery("input[name$='C. elegans_each']").attr('checked', true);
   }

   function updateURL(id)
   {
     GBROWSE_BASE_URL = "${GBROWSE_BASE_URL}";
     if (id == "D. melanogaster_a")
     {
       var GBROWSE_URL = GBROWSE_BASE_URL + "fly/?label=";
       // go through fly checkboxes and get submission titles
       var selectedCb = new Array();
       jQuery("input[name$='D. melanogaster_each']:checked").each(function(){
         selectedCb.push(jQuery(this).val());
       });

       if (selectedCb.length == 0)
       {
         alert("Please select some submissions...");
         return false;
       }
       else
       {
         GBROWSE_URL = GBROWSE_URL + getTrackURL(selectedCb, "D. melanogaster");
         jQuery("#D\\.\\ melanogaster_a").attr("href", GBROWSE_URL);
         return true;
       }

     }

     if (id == "C. elegans_a")
     {
       var GBROWSE_URL = GBROWSE_BASE_URL + "worm/?label=";
       // go through worm checkboxes and get submission titles
       var selectedCb = new Array();
       jQuery("input[name$='C. elegans_each']:checked").each(function(){
         selectedCb.push(jQuery(this).val());
       });

       if (selectedCb.length == 0)
       {
         alert("Please select some submissions...");
         return false;
       }
       else
       {
         GBROWSE_URL = GBROWSE_URL + getTrackURL(selectedCb, "C. elegans");
         jQuery("#C\\.\\ elegans_a").attr("href", GBROWSE_URL);
         return true;
       }
     }
   }

   function getTrackURL(selectedCb, organism)
   {
      var GBROWSE_TAIL = "";
      var idx;
      // TODO -
      for (idx in selectedCb) {
        <c:forEach var="tracks" items="${tracks}" varStatus="track_status">
          var tracksKey = "${tracks.key}";
          if (tracksKey == organism){
              <c:forEach var="trackDetails" items="${tracks.value}" varStatus="trackDetails_status">
                if (selectedCb[idx] == "${trackDetails.key}")
                {
                  <c:forEach var="gbTrack" items="${trackDetails.value}" varStatus="gbTrack_status">
                    GBROWSE_TAIL = GBROWSE_TAIL + "${gbTrack.track}/${gbTrack.subTrack}-";
                  </c:forEach>
                }
              </c:forEach>
          }
        </c:forEach>
      }

      return GBROWSE_TAIL.substr(0, GBROWSE_TAIL.length-1);
   }

   function getFullTrackURL(organism)
   {
     GBROWSE_URL = "${GBROWSE_BASE_URL}";
     if (organism == "D. melanogaster")
     {
       GBROWSE_URL = GBROWSE_URL + "fly/?label=";
     }
     if (organism == "C. elegans")
     {
       GBROWSE_URL = GBROWSE_URL + "worm/?label=";
     }

     <c:forEach var="tracks" items="${tracks}" varStatus="track_status">
          var tracksKey = "${tracks.key}";
          if (tracksKey == organism){
              <c:forEach var="trackDetails" items="${tracks.value}" varStatus="trackDetails_status">
                  <c:forEach var="gbTrack" items="${trackDetails.value}" varStatus="gbTrack_status">
                    GBROWSE_URL = GBROWSE_URL + "${gbTrack.track}/${gbTrack.subTrack}-";
                  </c:forEach>
              </c:forEach>
          }
      </c:forEach>

      return GBROWSE_URL.substr(0, GBROWSE_URL.length-1);
   }

   function updateStatus(status, name)
   {
     // 1. update check status; 2. update GBrowse url
     // TODO - generic
     var statTag;
     if (name == "D. melanogaster_each")
     {
         if (!status) { //unchecked
           jQuery("input[name$='D. melanogaster_each']").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#D\\.\\ melanogaster_all").attr('checked', true);
            jQuery("#D\\.\\ melanogaster_all").css("opacity", 0.5); }
           else {
            jQuery("#D\\.\\ melanogaster_all").removeAttr('checked');
            jQuery("#D\\.\\ melanogaster_all").css("opacity", 1);}
         }
         else { //checked
           jQuery("input[name$='D. melanogaster_each']").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#D\\.\\ melanogaster_all").attr('checked', true);
           jQuery("#D\\.\\ melanogaster_all").css("opacity", 0.5); }
         else {
           jQuery("#D\\.\\ melanogaster_all").attr('checked', true);
           jQuery("#D\\.\\ melanogaster_all").css("opacity", 1);}
         }

         // update url
           var GBROWSE_URL = "${GBROWSE_BASE_URL}" + "fly/?label=";

           var selectedCb = new Array();
           jQuery("input[name$='D. melanogaster_each']:checked").each(function(){
             selectedCb.push(jQuery(this).val());
           });

           if (selectedCb.length == 0)
           {
             jQuery("#D\\.\\ melanogaster_a").attr("href", GBROWSE_URL);
           }
           else
           {
             GBROWSE_URL = GBROWSE_URL + getTrackURL(selectedCb, "D. melanogaster");
             jQuery("#D\\.\\ melanogaster_a").attr("href", GBROWSE_URL);
           }
     }
     if (name == "C. elegans_each")
     {
          if (!status) { //unchecked
           jQuery("input[name$='C. elegans_each']").each(function() {
             if (this.checked) {statTag=true;}
           });

           if (statTag) {
            jQuery("#C\\.\\ elegans_all").attr('checked', true);
            jQuery("#C\\.\\ elegans_all").css("opacity", 0.5); }
           else {
            jQuery("#C\\.\\ elegans_all").removeAttr('checked');
            jQuery("#C\\.\\ elegans_all").css("opacity", 1);}
         }
         else { //checked
           jQuery("input[name$='C. elegans_each']").each(function() {
             if (!this.checked) {statTag=true;}
         });

         if (statTag) {
           jQuery("#C\\.\\ elegans_all").attr('checked', true);
           jQuery("#C\\.\\ elegans_all").css("opacity", 0.5); }
         else {
           jQuery("#C\\.\\ elegans_all").attr('checked', true);
           jQuery("#C\\.\\ elegans_all").css("opacity", 1);}
         }

         // update url
           var GBROWSE_URL = "${GBROWSE_BASE_URL}" + "worm/?label=";

           var selectedCb = new Array();
           jQuery("input[name$='C. elegans_each']:checked").each(function(){
             selectedCb.push(jQuery(this).val());
           });

           if (selectedCb.length == 0)
           {
             jQuery("#C\\.\\ elegans_a").attr("href", GBROWSE_URL);
           }
           else
           {
             GBROWSE_URL = GBROWSE_URL + getTrackURL(selectedCb, "C. elegans");
             jQuery("#C\\.\\ elegans_a").attr("href", GBROWSE_URL);
           }
     }
   }

//]]>-->
</script>

<style type="text/css">

table.stats
{
    text-align: center;
    font-family: Verdana, Geneva, Arial, Helvetica, sans-serif ;
    font-weight: normal;
    font-size: 11px;
    color: #fff;
    width: 100%;
    background-color: #666;
    border: 0px;
    border-collapse: collapse;
    border-spacing: 0px;
}

table.stats td
{
    background-color: #CCC;
    color: #000;
    padding: 4px;
    text-align: left;
    border: 1px #fff solid;
}

table.stats td.head
{
    background-color: #ffc;
    color: #000;
    padding: 4px;
    border-bottom: 2px #fff solid;
    font-size: 12px;
    font-weight: bold;
    text-align: left;
}

table.stats td.subnamecol
{
    white-space: nowrap;
}

table.stats td.trackcol
{
    background-color: #666;
}

div.trigger {
    padding: 0 0 0 40px;
    margin: 0 0 5px 0;
    background: url(model/images/toggle_minus_small.png) no-repeat;
    background-position: 0% 50%;
    height: 50px;
    line-height: 49px;
    width: 600px;
    font-family: Verdana;
    font-size: 1.8em;
    font-weight: normal;
    float: left;
    cursor: pointer;
}

div.active {
    background: url(model/images/toggle_plus_small.png) no-repeat;
    background-position: 0% 50%;
}

.toggle_container {
    clear: both;
    position:relative;
    left:5px;
}

</style>

<h3 id="GBTitle">GBrowse Tracks</h3>

<div id="details" style="display: block" class="collection-table column-border">

<c:forEach var="tracks" items="${tracks}" varStatus="track_status">
  <div class="trigger">
    <i style="color:black;">${tracks.key}</i>
  </div>
  <div class="toggle_container">

  <table id="${tracks.key}_table" >
<thead>
  <tr>
      <td class="head" colspan="3">
        <input type="checkbox" id="${tracks.key}_all" value="${tracks.key}" onclick="checkAll(this.id)"/>
        <a id="${tracks.key}_a" title="View selected tracks for ${tracks.key} in GBrowse" target="_blank" onclick="if(!updateURL(this.id)){return false;}" >View Selected Tracks in GBrowse</a>
      </td>
    </tr>
</thead>
<tbody>
    <c:forEach var="trackDetails" items="${tracks.value}" varStatus="trackDetails_status">
      <tr>
        <td valign="middle" class="subnamecol">
          <input type="checkbox" name="${tracks.key}_each" value="${trackDetails.key}" onclick="updateStatus(this.checked, this.name)" />
          <c:set var="maxLength" value="60"/>
          <im:abbreviate value="${trackDetails.key}" length="${maxLength}"/>
        </td>
        <td valign="middle">
          <c:forEach var="gbTrack" items="${trackDetails.value}" varStatus="gbTrack_status">
            <mm:singleTrack track="${gbTrack}"/>
            <br>
            <c:set var="DCCid" scope="request" value="${gbTrack.DCCid}"/>
          </c:forEach>


          </td>
         <td valign="middle">
           <mm:allTracks tracks="${trackDetails.value}" dccId="${DCCid}"/>
         </td>


         </tr>
    </c:forEach>
</tbody>
    </table>
  </div>
  </c:forEach>
</div>

<!-- /subListGBrowseTrackDisplayer.jsp -->