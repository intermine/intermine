<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute name="wsName"/>
<tiles:useAttribute id="ws" name="webSearchable" 
                    classname="org.intermine.web.logic.search.WebSearchable"/>
<tiles:importAttribute name="statusIndex"/>
<tiles:importAttribute name="wsCheckBoxId" ignore="true"/>
<tiles:importAttribute name="makeCheckBoxes" ignore="true"/>

<!-- wsBagRow.jsp -->
<c:if test="${!empty makeCheckBoxes}">
  <td>
    <html:multibox property="selectedBags" styleId="${wsCheckBoxId}">
      <c:out value="${ws.key}" escapeXml="false"/>
    </html:multibox>
  </td>
</c:if>

<tiles:insert name="renamableElement.jsp">
  <tiles:put name="name" value="${ws.name}"/>
  <tiles:put name="type" value="bag"/>
  <tiles:put name="index" value="${statusIndex}"/>
</tiles:insert>

<c:if test="${IS_SUPERUSER}">
  <td>
    <c:set var="taggable" value="${ws}"/>
    <tiles:insert name="inlineTagEditor.tile">
      <tiles:put name="taggable" beanName="taggable"/>
      <tiles:put name="vertical" value="true"/>
      <tiles:put name="show" value="true"/>
    </tiles:insert>
  </td>
</c:if>

<td><c:out value="${ws.type}" /></td>

<td>
  <c:choose>
    <c:when test="${empty ws.description}">
      &nbsp;  <!-- so that IE 6 renders the cell borders -->
    </c:when>
    <c:otherwise>
      <c:out value="${ws.description}" />
    </c:otherwise>
  </c:choose>
</td>

<td>
  <c:if test="${!empty ws.dateCreated}">
    <im:dateDisplay date="${ws.dateCreated}"/>
  </c:if>
</td>

<td align="right">
  <c:out value="${ws.size}"/>
  <c:if test="${ws.size == 1}">
    value
  </c:if>
  <c:if test="${ws.size != 1}">
    unique values
  </c:if>
</td>


<!-- /wsBagRow.jsp -->
