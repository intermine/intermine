<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- ecolimineGBrowse.jsp -->

<c:if test="${((!empty object.chromosomeLocation && !empty object.chromosome)
                || cld.unqualifiedName == 'Chromosome')}">

    <c:set var="start" value="${object.chromosomeLocation.start}"/>
    <c:set var="end" value="${object.chromosomeLocation.end}"/>

    <%-- display starts  --%>

    <div id="gBrowse">
      <h3>Genome Browser</h3>

      <c:choose>
      <c:when test="${WEB_PROPERTIES['gbrowse.prefix'] != null}">
        <div class="loading" align="center">
          <html:link href="${WEB_PROPERTIES['gbrowse.prefix']}${cord}&width=750"></html:link>
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
                .attr('src', "${WEB_PROPERTIES['gbrowse_image.prefix']}${cord_ext}&width=500")
                .attr('style', 'border:1px solid #000;')
                .attr('title', 'GBrowse');
          });
        </script>
    </c:if>

</c:if>


<!-- /ecolimineGBrowse.jsp -->