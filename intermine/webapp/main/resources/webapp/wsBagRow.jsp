<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="wsName"/>
<tiles:importAttribute name="scope"/>
<tiles:useAttribute id="webSearchable" name="webSearchable" 
                    classname="org.intermine.web.logic.search.WebSearchable"/>
<tiles:importAttribute name="showDescriptions" ignore="true"/>
<tiles:importAttribute name="statusIndex"/>
<tiles:importAttribute name="wsCheckBoxId" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<!-- wsBagRow.jsp -->

<c:if test="${!empty makeCheckBoxes}">
  <td>
    <html:multibox property="selectedBags" styleId="${wsCheckBoxId}">
      <c:out value="${webSearchable.name}" escapeXml="false"/>
    </html:multibox>
  </td>
</c:if>

  <c:choose>
    <c:when test="${scope == 'user'}">
      <tiles:insert name="renamableElement.jsp">
        <tiles:put name="name" value="${webSearchable.name}"/>
        <tiles:put name="type" value="bag"/>
        <tiles:put name="index" value="${statusIndex}"/>
      </tiles:insert>
    </c:when>
    <c:otherwise>
      <td colspan="2">
        <c:set var="nameForURL"/>
        <str:encodeUrl var="nameForURL">${name}</str:encodeUrl>
        <html:link action="/bagDetails?bagName=${nameForURL}">
          <c:out value="${webSearchable.name}"/>
        </html:link>
      </td>
    </c:otherwise>
  </c:choose>

<c:if test="${IS_SUPERUSER}">
  <td>
    <c:set var="taggable" value="${webSearchable}"/>
    <tiles:insert name="inlineTagEditor.tile">
      <tiles:put name="taggable" beanName="taggable"/>
      <tiles:put name="vertical" value="true"/>
      <tiles:put name="show" value="true"/>
    </tiles:insert>
  </td>
</c:if>

<td><c:out value="${webSearchable.type}" /></td>

<c:if test="${showDescriptions}">
  <td>
    <c:choose>
      <c:when test="${empty webSearchable.description}">
        &nbsp;  <!-- so that IE 6 renders the cell borders -->
      </c:when>
      <c:otherwise>
        <c:out value="${webSearchable.description}" />
      </c:otherwise>
    </c:choose>
  </td>
</c:if>

<td>
  <c:if test="${!empty webSearchable.dateCreated}">
    <im:dateDisplay type="short" date="${webSearchable.dateCreated}"/>
  </c:if>
</td>

<td align="right">
  <c:out value="${webSearchable.size}"/>
  <c:if test="${webSearchable.size == 1}">
    value
  </c:if>
  <c:if test="${webSearchable.size != 1}">
    unique values
  </c:if>
</td>

<!-- /wsBagRow.jsp -->
