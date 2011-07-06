<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- submissionPropertiesDisplayer.jsp -->

<div class="basic-table" id="submission-properties-div">

<table>
  <tr>
    <td style="width:15%;">Organism:</td>
    <td>
      <c:forEach var="organism" items="${organismMap}" varStatus="status">
        <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${organism.key}" style="text-decoration: none;"><strong>${organism.value}</strong></a>
      </c:forEach>
    </td>
  </tr>
  <tr>
    <td valign="top">Cell Line:</td>
    <td>
      <c:choose>
        <c:when test="${not empty cellLineMap}">
          <c:forEach var="celline" items="${cellLineMap}" varStatus="status">
            <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${celline.key}" style="text-decoration: none;"><strong>${celline.value}</strong></a>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Antibody/Target:</td>
    <td id="antibodyContent">
      <c:choose>
        <c:when test="${not empty antibodyInfoList}">
          <c:forEach var="antibody" items="${antibodyInfoList}" varStatus="status">
            <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${antibody.id}" style="text-decoration: none;"><strong>${antibody.name}</strong></a>
            /
            <c:choose>
                <c:when test="${not empty antibody.target}">
                  <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${antibody.target.id}" style="text-decoration: none;"><strong>${antibody.targetName}</strong></a>
                </c:when>
                <c:otherwise>
                  <i>target not available</i>
                </c:otherwise>
            </c:choose>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Developmental Stage:</td>
    <td>
      <c:choose>
        <c:when test="${not empty developmentalStageMap}">
          <c:forEach var="developmentalstage" items="${developmentalStageMap}" varStatus="status">
            <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${developmentalstage.key}" style="text-decoration: none;"><strong>${developmentalstage.value}</strong></a>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Strain:</td>
    <td>
      <c:choose>
        <c:when test="${not empty strainMap}">
          <c:forEach var="strain" items="${strainMap}" varStatus="status">
            <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${strain.key}" style="text-decoration: none;"><strong>${strain.value}</strong></a>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Tissue:</td>
    <td>
      <c:choose>
        <c:when test="${not empty tissueMap}">
          <c:forEach var="tissue" items="${tissueMap}" varStatus="status">
            <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${tissue.key}" style="text-decoration: none;"><strong>${tissue.value}</strong></a>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Array:</td>
    <td>
      <c:choose>
        <c:when test="${not empty arrayMap}">
          <c:forEach var="array" items="${arrayMap}" varStatus="status">
            <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${array.key}" style="text-decoration: none;"><strong>${array.value}</strong></a>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <c:if test="${not empty submissionPropertyMap}">
    <c:forEach var="submissionproperties" items="${submissionPropertyMap}">
      <tr>
        <td valign="top">${submissionproperties.key}:</td>
        <td id="${submissionproperties.key}Content_${fn:length(submissionproperties.value)}">
          <c:forEach var="submissionproperty" items="${submissionproperties.value}" varStatus="status">
            <a href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${submissionproperty.key}" style="text-decoration: none;"><strong>${submissionproperty.value}</strong></a>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </td>
      </tr>
    </c:forEach>
  </c:if>
</table>

</div>

<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript">

        //TODO: concise code
        for (i=0;i<jQuery('td[id*="Content"]').length;i++)
        {
          if (jQuery('td[id*="Content"]').eq(i).attr("id") != "submissionDescriptionContent") {
            if (jQuery('td[id*="Content"]').eq(i).attr("id").indexOf("primerContent") != -1) {
                var id = jQuery('td[id*="Content"]').eq(i).attr("id");
                var count = id.substr(id.indexOf("_")+1);
                if (count > 15) {
                  jQuery('td[id*="Content"]').eq(i).expander({
                    slicePoint: 200,
                    expandText: 'read all ' + count + ' records'
                  });
                } else {
                  jQuery('td[id*="Content"]').eq(i).expander({
                    slicePoint: 200
                  });
                }
            } else {
                  jQuery('td[id*="Content"]').eq(i).expander({
                    slicePoint: 200
                  });
            }
          }
        }

</script>

<!-- /submissionPropertiesDisplayer.jsp -->