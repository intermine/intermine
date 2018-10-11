<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- sequenceFeatureDisplayer.jsp -->

<div id="sequence-feature-displayer" class="collection-table column-border-by-2">

<style>
<%-- this bad boy sometimes shows in the top of the page --%>
#object_header #sequencefeaturedisplayer-wrapper.wrapper { margin-bottom:10px; }
#object_header #sequence-feature-displayer { margin:0 0 0 3px; }
#object_header #sequence-feature-displayer h3 { display:none; }
#object_header #sequence-feature-displayer table { width:0%; }
#object_header #sequence-feature-displayer table td { padding:0; }
#object_header #sequence-feature-displayer table,
#object_header #sequence-feature-displayer table tr,
#object_header #sequence-feature-displayer table td,
#object_header #sequence-feature-displayer table th { border:0; background:transparent; }
#object_header #sequence-feature-displayer table tr td:not(:first-child) { padding-right:20px; }
</style>

<c:set var="feature" value="${reportObject.object}"/>

<c:choose>
  <c:when test="${locationsCollection != null}">
    <h3>This feature maps to ${locationsCollectionSize} genome locations</h3>
  </c:when>
  <c:otherwise>
    <h3>Genome feature</h3>
  </c:otherwise>
</c:choose>

<table border="0" cellspacing="0">
  <%-- sequence ontology type and length --%>
  <c:choose>
    <c:when test="${!empty feature.sequenceOntologyTerm}">
      <tr>
        <c:choose>
          <c:when test="${!empty feature.length}">
            <td class="label">Region:</td>
            <td>
              <strong><c:out value="${feature.sequenceOntologyTerm.name}"/></strong>
              <im:helplink text="${feature.sequenceOntologyTerm.description}"/>
            </td>
            <td class="label">Length:</td>
            <td>
              <c:set var="interMineObject" value="${reportObject.object}" scope="request" />
              <tiles:insert page="/model/sequenceShortDisplayerWithField.jsp">
                <tiles:put name="expr" value="length" />
                <tiles:put name="objectClass" value="${objectClass}" />
              </tiles:insert>
            </td>
          </c:when>
          <c:otherwise>
            <td class="label">Region:</td>
            <td colspan="3">
              <strong><c:out value="${feature.sequenceOntologyTerm.name}"/></strong>
              <im:helplink text="${feature.sequenceOntologyTerm.description}"/>
            </td>
          </c:otherwise>
        </c:choose>
      </tr>
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${!empty feature.length}">
          <tr>
            <td class="label">Length:</td>
            <td colspan="3">
              <c:set var="interMineObject" value="${reportObject.object}" scope="request" />
              <tiles:insert page="/model/sequenceShortDisplayerWithField.jsp">
                <tiles:put name="expr" value="length" />
                <tiles:put name="objectClass" value="${objectClass}" />
              </tiles:insert>
            </td>
          </tr>
        </c:when>
      </c:choose>
    </c:otherwise>
  </c:choose>

  <tr>
    <td class="label">Location:</td>
      <c:choose>
        <c:when test="${!empty feature.chromosomeLocation}">
          <td>
            <strong>
              <c:set var="loc" value="${feature.chromosomeLocation}"/>
              <c:out value="${loc.locatedOn.primaryIdentifier}:${loc.start}-${loc.end}"/>
            </strong>
            <c:if test="${!empty loc.strand}">
              <span class="smallnote">
                <c:choose>
                  <c:when test="${loc.strand == '+1'}">forward strand</c:when>
                  <c:when test="${loc.strand == '-1'}">reverse strand</c:when>
                </c:choose>
              </span>
            </c:if>
          </td>
        </c:when>
        <c:when test="${locationsCollection != null}">
          <td>
            <div class="collection-table column-border nomargin locations-table">
              <table><tbody><tr>
                <c:forEach items="${locationsCollection}" var="loc" varStatus="statei">
                  <td class="<c:if test="${(statei.count + 1) % 3 == 0}">centered</c:if>">
                    <strong><c:out value="${loc.locatedOn.primaryIdentifier}:${loc.start}-${loc.end}"/></strong>
                    <c:if test="${!empty loc.strand}">
                      <span class="smallnote">
                        <c:choose>
                          <c:when test="${loc.strand == '+1'}">forward strand</c:when>
                          <c:when test="${loc.strand == '-1'}">reverse strand</c:when>
                        </c:choose>
                      </span>
                    </c:if>
                  </td>
                  <c:if test="${statei.count % 3 == 0}">
                    </tr>
                    <c:choose>
                      <c:when test="${statei.count >= 9}"><tr style="display:none;"></c:when>
                      <c:otherwise><tr></c:otherwise>
                    </c:choose>
                  </c:if>
                </c:forEach>
              </tr></tbody></table>
              <c:if test="${locationsCollectionSize >= 9}">
                <div class="toggle">
                  <a class="more">Show more rows</a>
                </div>
              </c:if>
            </div>
            <div style="display:none;" class="show-in-table">
              <html:link action="/collectionDetails?id=${feature.id}&amp;field=locations&amp;trail=${param.trail}">
                Show all in a table
              </html:link>
            </div>
        </c:when>
        <c:otherwise>
          <c:choose>
            <c:when test="${empty cytoLocation && empty mapLocation}">
              <td colspan="3">
            </c:when>
            <c:otherwise>
              <td colspan="4">
            </c:otherwise>
          </c:choose>
            No location information in ${WEB_PROPERTIES['project.title']}
        </c:otherwise>
      </c:choose>
    </td>
    <c:choose>
      <c:when test="${!empty cytoLocation}">
        <td class="label">Cyto location:</td>
        <td>
          <strong><c:out value="${cytoLocation}"/></strong>
        </td>
      </c:when>
      <c:when test="${!empty mapLocation}">
        <td class="label">Map location:</td>
        <td>
          <strong><c:out value="${mapLocation}"/></strong>
        </td>
      </c:when>
    </c:choose>
  </tr>
</table>

<script type="text/javascript">
(function() {
  jQuery("#sequence-feature-displayer div.toggle a.more").click(function() {
    jQuery("#sequence-feature-displayer div.locations-table tr:hidden").each(function(index) {
        if (index < 3) {
            jQuery(this).show();
        }
    });
    if (jQuery("#sequence-feature-displayer div.locations-table tr:hidden").length <= 0) {
        jQuery("#sequence-feature-displayer div.toggle a.more").remove();
    }
  });

  <%-- fixup number of columns --%>
  var l = jQuery('#sequence-feature-displayer table tr:first td').length,
       m = jQuery('#sequence-feature-displayer table tr:last td').length;
  if (l != m) {
    jQuery('#sequence-feature-displayer table tr:last td:last').attr('colspan', l - m + 1);
  }
})();
</script>

</div>

<!-- /sequenceFeatureDisplayer.jsp -->
