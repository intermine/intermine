<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- overlappingFeaturesDisplayer.jsp -->

<div>
  <h3>Overlapping Features</h3>
  <p style="color: red;">[UNDER CONSTRUCTION!]</p>
  <p>Genome features that overlap coordinates of this ${reportObject.type}</p>
  <c:forEach items="${featureCounts}" var="entry">
    <c:out value="${entry.key}: ${entry.value}"/>
  </c:forEach>

  <c:if test="${!empty featureTables}">
    <c:forEach items="${featureTables}" var="entry">
      <br/>${entry.value}
      <%--
      <tiles:insert page="/reportCollectionTable.jsp">
         <tiles:put name="inlineResultsTable" beanName="entry.value" />
         <tiles:put name="object" beanName="reportObject.object" />
         <tiles:put name="fieldName" value="${entry.key}" />
      </tiles:insert>
      --%>
    </c:forEach>
  </c:if>

</div>

<!-- /overlappingFeaturesDisplayer.jsp -->
