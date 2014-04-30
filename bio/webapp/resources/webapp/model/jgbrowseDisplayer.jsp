<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<!-- jBrowseDisplayer.jsp -->
<script type="text/javascript">



function mobileTest(){

    /// care of http://detectmobilebrowsers.com/
    var a = navigator.userAgent||navigator.vendor||window.opera;
	if(/(android|bb\d+|meego|android|ipad|playbook|silk).+mobile|avantgo|bada\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\.(browser|link)|vodafone|wap|windows (ce|phone)|xda|xiino/i.test(a)
	||
	/1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\-(n|u)|c55\/|capi|ccwa|cdm\-|cell|chtm|cldc|cmd\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\-s|devi|dica|dmob|do(c|p)o|ds(12|\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\-|_)|g1 u|g560|gene|gf\-5|g\-mo|go(\.w|od)|gr(ad|un)|haie|hcit|hd\-(m|p|t)|hei\-|hi(pt|ta)|hp( i|ip)|hs\-c|ht(c(\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\-(20|go|ma)|i230|iac( |\-|\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\/)|klon|kpt |kwc\-|kyo(c|k)|le(no|xi)|lg( g|\/(k|l|u)|50|54|\-[a-w])|libw|lynx|m1\-w|m3ga|m50\/|ma(te|ui|xo)|mc(01|21|ca)|m\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\-2|po(ck|rt|se)|prox|psio|pt\-g|qa\-a|qc(07|12|21|32|60|\-[2-7]|i\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\-|oo|p\-)|sdk\/|se(c(\-|0|1)|47|mc|nd|ri)|sgh\-|shar|sie(\-|m)|sk\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\-|v\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\-|tdg\-|tel(i|m)|tim\-|t\-mo|to(pl|sh)|ts(70|m\-|m3|m5)|tx\-9|up(\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\-|your|zeto|zte\-/i.test(a.substr(0,4)))
	{
	return true;
	}else{
	return false;
	}
	
	};
	
if(mobileTest()){
 document.getElementById("gBrowseWrapper").style.display="inline";
}else{
 document.getElementById("jBrowse").style.display="inline";
}


function toggleBrowser(){
if(document.getElementById("gBrowseWrapper").style.display == "inline"){
 document.getElementById("gBrowseWrapper").style.display = "none";
 document.getElementById("jBrowse").style.display="inline";
 document.getElementById("jBrowseFrame").src = document.getElementById("jBrowseFrame").src;
}else{
 document.getElementById("jBrowse").style.display="none";
 document.getElementById("gBrowseWrapper").style.display = "inline";
}
}



var expand = true;
function expandCollapse(){
if(expand){
  jQuery('iframe').css({height: '600px'});
}else{
  jQuery('iframe').css({height: '75px'});
}
expand = !expand;
}

</script>

<c:if test="${((!empty reportObject.object.chromosomeLocation && !empty reportObject.object.chromosome)
                || cld.unqualifiedName == 'Chromosome') && cld.unqualifiedName != 'ChromosomeBand'}">
<div id="jBrowse" style="display:none">
  <div class="geneInformation">

    <h3 class="overlapping">Genome Browser</h3>
    

    <c:set var="baseUrl" value="${baseURL}"/>
    <c:set var="chr" value="${reportObject.object.chromosomeLocation.locatedOn.primaryIdentifier}"/>
    <c:set var="padding" value="${10}"/>
    <c:set var="offset" value="${fn:substringBefore((reportObject.object.length * 0.1), '.')}"/>

    <c:set var="start" value="${reportObject.object.chromosomeLocation.start - offset}"/>
    <c:set var="end" value="${reportObject.object.chromosomeLocation.end + offset}"/>
    <c:set var="genus" value="${reportObject.object.organism.genus}"/>
    <c:set var="species" value="${reportObject.object.organism.species}"/>
    <c:if test="${species == 'sapiens'}">
       <c:set var="tracks" value="NCBI"/>
    </c:if> 
    <c:set var="jbLink" value="${baseUrl}&loc=${chr}:${start}..${end}&tracks=${tracks}"/>

    <p>Click and drag the browser to move the view.  Drag and drop tracks from left menu into the main
	   panel to see the data. Clicking on individual features to open a report page for that feature.
	    <br/>
	    <strong>*</strong> denotes SNPs that are mapped to multiple genome position.
    <a href="${jbLink}" target="jbrowse">Center on ${reportObject.object.symbol}</a></p>
	<iframe id="jBrowseFrame" name="jbrowse" height="300px" width="98%" style="border: 1px solid #dfdfdf; padding: 1%" src="${jbLink}"></iframe>
    <p><a href="javascript:;" onclick="javascript:expandCollapse();">Expand/Collapse viewer</a>&nbsp;(more about <a href="http://jbrowse.org">JBrowse</a>) &nbsp;
    <a href="${jbLink}" target="_blank">Open in new tab</a></p>
    <p><a href="javascript:;" onclick="javascript:toggleBrowser();">Not working? Try GBrowse</a></p>
</div>
</div>
<!-- END GBrowse section -->

<!-- gbrowseDisplayer.jsp -->

<c:set var="object" value="${reportObject.object}"/>

<c:choose>
<c:when test="${!empty object.chromosomeLocation && !empty object.chromosome && object.organism.taxonId==10090}">
<div id="gBrowseWrapper" style="display:none">
<div id="gBrowse" class="feature basic-table">
  <h3><fmt:message key="sequenceFeature.GBrowse.message"/></h3>

  <c:set var="loc" value="${object.chromosomeLocation}" />
  <c:set var="chrom" value="${loc.locatedOn.primaryIdentifier}" />
  <c:if test="${fn:startsWith(chrom, '0')}">
<c:set var="chrom" value="${fn:substring(chrom, 1, 2)}" />
  </c:if>
  <c:set var="name" value="${chrom}:${loc.start}..${loc.end}" />

  <c:choose>
  <c:when test="${WEB_PROPERTIES['gbrowse.database.source'] != null}">
    <div class="loading">
      <html:link  styleId="imageLink" href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?start=${loc.start};stop=${loc.end};ref=${chrom};label=${WEB_PROPERTIES['gbrowse.tracks']}"></html:link>
    </div>
    
<p><a href='javascript:;' onclick='javascript:toggleBrowser();'>Not working? Try JBrowse</a></p>
</div>
<script type="text/javascript">
jQuery(document).ready(function() {
var img = new Image();
// wrap our new image in jQuery
jQuery(img)
// once the image has loaded, execute this code
.load(function() {
// 'remove' loading
jQuery("#gBrowse div").removeClass('loading');
// attach image
jQuery('#imageLink').html(this);
})
.error(function() {
// 'remove' loading
jQuery("#gBrowse div").removeClass('loading');
// notify the user that the image could not be loaded
jQuery('#gBrowse').addClass('warning').append(jQuery('</p>', { 'text': 'There was a problem rendering the displayer, image could not be fetched.' }));
})
// set the attributes of the image
.attr('src', "${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.database.source']}?t=${WEB_PROPERTIES['gbrowse_image.tracks']};name=${name};width=600")
.attr('style', 'border:1px solid #000;')
.attr('title', 'GBrowse');
});
</script>
  </c:when>
  <c:otherwise>
    <p>There was a problem rendering the displayer, check: <code>WEB_PROPERTIES['gbrowse.database.source']</code>.</p>
<script type="text/javascript">
jQuery('#gBrowse').addClass('warning');
</script>
  </c:otherwise>
  </c:choose>
</div>
</c:when>
</c:choose>
<!-- /gbrowseDisplayer.jsp -->




<!--

<script type="text/javascript">
/* <![CDATA[ */
var bookmarkCallback = function(brwsr) {
    return window.location.protocol
       + "//" + window.location.host
       + window.location.pathname
       + "?loc=" + brwsr.visibleRegion()
       + "&tracks=" + brwsr.visibleTracks();
    }
var dataDir = window.location.protocol
       + "//" + window.location.host
	   + "/jbrowse/data";
var b = new Browser({
    containerID: "GenomeBrowser",
    refSeqs: refSeqs,
    trackData: trackInfo,
    defaultTracks: "DNA,gene,mRNA,noncodingRNA",
    location: "chr${chr}:${start}..${end}",
    tracks: "${tracks}",
    bookmark: bookmarkCallback,
    dataRoot: dataDir
});

/* ]]> */
</script>

-->

</c:if>
<!-- /jBrowseDisplayer.jsp -->
