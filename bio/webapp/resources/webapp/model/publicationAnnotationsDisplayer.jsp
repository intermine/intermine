<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>


<!-- publicationAnnotationsDisplayer.jsp -->

<div class="basic-table">

  <c:if test="${!empty results}">
    <c:forEach items="${results}" var="entry">
      <div class="collection-table" id="${entry.key}">
        <h3><c:out value="${imf:formatPathStr(entry.key, INTERMINE_API, WEBCONFIG)}"/>s</h3>
        <div class="clear"></div>

        <c:set var="inlineResultsTable" value="${entry.value}" />
        <tiles:insert page="/reportCollectionTable.jsp">
           <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
           <tiles:put name="object" beanName="reportObject.object" />
           <tiles:put name="fieldName" value="${entry.key}" />
        </tiles:insert>
      <br/>
      </div>
      <div class="clear"></div>
    </c:forEach>
  </c:if>


</div>

<!-- /publicationAnnotationsDisplayer.jsp -->
