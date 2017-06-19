<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>


<!-- reportInList.jsp -->

<tiles:importAttribute name="object" ignore="false"/>

  <div class="heading">
    Lists
  </div>

<div class="body">

<div>
  <c:choose>
   <c:when test="${bagsWithId.count == 0}">
      This <c:out value="${object.type}"/> isn't in any lists. <html:link href="/${WEB_PROPERTIES['webapp.path']}/bag.do?subtab=upload">Upload a list</html:link>.
    </c:when>
    <c:when test="${bagsWithId.count == 1}">
      This <c:out value="${object.type}"/> is in one list:
    </c:when>
    <c:otherwise>
      This <c:out value="${object.type}"/> is in ${bagsWithId.count} lists:
    </c:otherwise>
  </c:choose>
  <div class="listsObjectIsIn">
    <c:forEach items="${bagsWithId.collection}" var="bag" varStatus="status">
      <html:link href="bagDetails.do?bagName=${bag.name}"><c:out value="${bag.name}"/></html:link>&nbsp;(<c:out value="${bag.size}"/>)<br/>
    </c:forEach>
  </div>
</div>

<%-- Add to list --%>
<c:if test="${!empty bagsToAddTo}">
  <div>
    <form action="<html:rewrite page="/addToBagAction.do"/>" method="POST">
      Add this ${object.type} to one of your lists:<br/>
      <input type="hidden" name="__intermine_forward_params__" value="${pageContext.request.queryString}"/>
      <select name="bag">
        <c:forEach items="${bagsToAddTo}" var="bagOfType">
          <option name="${bagOfType.name}">${bagOfType.name}</option>
        </c:forEach>
      </select>
      <input type="hidden" name="object" value="${object.id}"/>
      <input type="submit" value="<fmt:message key="button.add"/>"/>
    </form>
  </div>
</c:if>

</div>
<br />

<!-- /reportInList.jsp -->
