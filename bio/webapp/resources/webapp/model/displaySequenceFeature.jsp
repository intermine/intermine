<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<tiles:importAttribute name="feature" ignore="false"/>
<tiles:importAttribute name="idToHighlight" ignore="true"/>
<tiles:importAttribute name="singleLine" ignore="true"/>
<tiles:importAttribute name="alternate" ignore="true"/>

 <c:set var="detailsLink" value="/${WEB_PROPERTIES['webapp.path']}/report.do?id="/>

<c:if test="${!empty singleLine}">
  <c:set var="wrapStyle" value="white-space: nowrap;"/>
</c:if>

<c:choose>
  <c:when test="${!empty idToHighlight && idToHighlight == feature.id}">
    <%--<span class="highlight" style="${wrapStyle}">--%>
  </c:when>
  <c:otherwise>
    <%--<span style="${wrapStyle}">--%>
  </c:otherwise>
</c:choose>
  <td style="white-space: nowrap;" class='<c:if test="${alternate != null}">alt</c:if>'>
    <c:if test="${!empty feature.symbol}">
      <a href="${detailsLink}${feature.id}"><c:out value="${feature.symbol}"/></a>
    </c:if>
    <a href="${detailsLink}${feature.id}"><c:out value="${feature.primaryIdentifier}"/></a>
  <c:choose>
    <c:when test="${singleLine != null}">
      </td>
      <td style="text-align:right; ${wrapStyle}" class='<c:if test="${alternate != null}">alt</c:if>'>
    </c:when>
    <c:otherwise>
      <br />
    </c:otherwise>
  </c:choose>
    <c:if test="${!empty feature.length}">
      ${feature.length}
    </c:if>
    <c:set var="object" value="${feature}" scope="request"/>&nbsp;<tiles:insert page="/model/sequenceShortDisplayer.jsp"/>
  </td>
<%--</tr>--%>