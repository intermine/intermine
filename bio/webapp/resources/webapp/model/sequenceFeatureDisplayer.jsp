<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- sequenceFeatureDisplayer.jsp -->

<div>

<c:set var="feature" value="${displayObject.object}"/>

<h3>Genome feature</h3>

<table>
  <tr>
    <td>
      feature: <strong><c:out value="${feature.symbol} ${feature.primaryIdentifier}"/></strong>
    </td>
    <td colspan="2">
      <c:if test="${!empty feature.sequenceOntologyTerm}">
        sequence ontology type: <strong><c:out value="${feature.sequenceOntologyTerm.name}"/></strong>
        <a href="#" title="${feature.sequenceOntologyTerm.description}">[definition]</a>
      </c:if>
    </td>
  </tr>
  <tr>
    <td>Location:
      <c:if test="${!empty feature.chromosomeLocation}">
          <strong>
            <c:set var="loc" value="${feature.chromosomeLocation}"/>
            <c:out value="${loc.locatedOn.primaryIdentifier}:${loc.start}-${loc.end}"/>
          </strong>
          <c:if test="${!empty loc.strand}">
             <span class="smallnote">
               <c:choose>
                 <c:when test="${loc.strand == 1}">
                   forward strand
                 </c:when>
                 <c:when test="${loc.strand == -1}">
                   reverse strand
                 </c:when>
               </c:choose>
             </span>
          </c:if>
      </c:if>
    </td>
    <td>
      <c:if test="${!empty feature.length}">
        length: <strong><c:out value="${feature.length}"/></strong>
      </c:if>
    </td>
    <td>
      [SEQUENCE]  [GFF3]
    </td>
  </tr>
</table>

<hr />

</div>

<!-- /sequenceFeatureDisplayer.jsp -->
