<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- modmineGBrowse.jsp -->

<%--TODO check all this cases, with list of possible types from toronto --%>
<%--
<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome')
                && cld.unqualifiedName != 'Exon'
                && cld.unqualifiedName != 'CDS'}">
--%>

<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome')}">


  <c:choose>
    <c:when test="${object.organism.taxonId == 6239}">
      <c:set var="gbrowseSource" value="worm"/>
    </c:when>
    <c:otherwise>
      <c:set var="gbrowseSource" value="fly"/>
    </c:otherwise>
  </c:choose>


<c:set var="start" value="${object.chromosomeLocation.start}"/>
<c:set var="end" value="${object.chromosomeLocation.end}"/>
<c:set var="ref" value="${object.chromosome.primaryIdentifier}"/>

<c:set var="offset" value="${(end-start)/10}"/>
<c:set var="istart" value="${start-offset}"/>
<c:set var="iend" value="${end+offset}"/>

<c:set var="label" value=""/>

<%-- in case there are >1 submissions associated TODO: check if the + is ok (or - like in labels?)--%>
<c:forEach items="${object.submissions}" var="sub" varStatus="status">

   <c:forEach items="${subTracks}" var="st" varStatus="st_status">
      <c:if test="${st.key == sub.dCCid}">
        <c:forEach items="${st.value}" var="track" varStatus="track_status">

            <c:choose>
                <c:when test="${track_status.first}">
                <%-- this should in theory link to the right subtrack, but doesn't work
                     so at the moment displaying all subtracks
                     <c:set var="label" value=";label=${track.track}/${track.subTrack}" />
                --%>
                     <c:set var="label" value=";label=${track.track}" />
                </c:when>
                <c:otherwise>
                     <c:set var="label" value="${label};label=${track.track}" />
                </c:otherwise>
            </c:choose>

        </c:forEach>
      </c:if>

   </c:forEach>

</c:forEach>


<%-- display starts  --%>

<div id="gBrowse">
  <h3>modENCODE Genome Browser</h3>

  <c:set var="link" value="?start=${start};end=${end};ref=${ref};label=Genes${label}"></c:set>
  <c:choose>
  <c:when test="${WEB_PROPERTIES['gbrowse.prefix'] != null}">
    <div class="loading" align="center">
      <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${gbrowseSource}/${link};width=750"></html:link>
    </div>
  </c:when>
  <c:otherwise>
    <p class="gbrowse-not-configured"><i>GBrowse is not configured in modmine.properties</i></p>
  </c:otherwise>
  </c:choose>
</div>

<c:if test="${cld.unqualifiedName != 'Chromosome'}">
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
          jQuery('#gBrowse a').html(this);
        })
        .error(function() {
          // 'remove' loading
          jQuery("#gBrowse div").removeClass('loading');
          // notify the user that the image could not be loaded
          jQuery('#gBrowse a').html("The genome browser could not be loaded.")
          .attr('style', 'color:#ff0000;font-weight:bold;');
        })
        // set the attributes of the image
        .attr('src', "${WEB_PROPERTIES['gbrowse_image.prefix']}/${gbrowseSource}/${link};width=500;b=1")
        .attr('style', 'border:1px solid #000;')
        .attr('title', 'GBrowse');
  });
</script>
</c:if>

</c:if>


<!-- /modmineGBrowse.jsp -->