<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- sequenceFeatureDisplayer.jsp -->

<div class="feature">

<c:set var="feature" value="${displayObject.object}"/>

<h3>Genome feature</h3>

<table border="0" cellspacing="0">
  <tr>
    <td>Feature:</td>
    <td><strong><c:out value="${feature.symbol} ${feature.primaryIdentifier}"/></strong></td>
    <c:choose>
      <c:when test="${!empty feature.sequenceOntologyTerm}">
        <td class="border-left">Sequence ontology type:</td>
        <td colspan="2">
          <strong><c:out value="${feature.sequenceOntologyTerm.name}"/></strong>
          <img alt="?" title="${feature.sequenceOntologyTerm.description}"
          src="images/icons/information-small-blue.png" style="padding-bottom: 4px;"
          class="tinyQuestionMark" />
        </td>
      </c:when>
      <c:otherwise>
        <td colspan="3">&nbsp;</td>
      </c:otherwise>
    </c:choose>
  </tr>
  <tr class="even">
    <td class="theme-3-border theme-6-background">Location:</td>
    <td class="theme-3-border theme-6-background">
      <c:choose>
        <c:when test="${!empty feature.chromosomeLocation}">
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
        </c:when>
        <c:otherwise>
          <c:forEach items="${col}" var="loc" varStatus="statei">
            <strong>
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
          </c:forEach>
        </c:otherwise>
      </c:choose>
    </td>
    <c:choose>
      <c:when test="${!empty feature.length}">
        <td class="theme-3-border theme-6-background border-left">Length:</td>
        <td class="theme-3-border theme-6-background">
          <tiles:insert page="/model/sequenceShortDisplayerWithField.jsp">
            <tiles:put name="expr" value="${feature.length}" />
          </tiles:insert>
        </td>
      </c:when>
      <c:otherwise>&nbsp;</c:otherwise>
    </c:choose>
    <td class="theme-3-border theme-6-background">[SEQUENCE] [GFF3]</td>
  </tr>
</table>

</div>

<!-- /sequenceFeatureDisplayer.jsp -->