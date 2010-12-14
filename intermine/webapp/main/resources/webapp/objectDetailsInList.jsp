<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- objectDetailsInList.jsp -->
<c:if test="${(!empty bagsWithId) || (! empty PROFILE.savedBags)}">
  <div class="heading">
    Lists
  </div>

<div class="body" style="padding:10px;border:1px #CCC solid">
<c:if test="${! empty bagsWithId}">
<div>
      Lists in which this
      <c:forEach items="${object.clds}" var="cld">
            ${cld.unqualifiedName}
      </c:forEach> can be found:
      <c:forEach items="${bagsWithId}" var="interMineBag" varStatus="status">
        <c:if test="${status.count > 2}">,</c:if>&nbsp;<html:link href="bagDetails.do?bagName=${interMineBag.name}"><c:out value="${interMineBag.name}"/></html:link>
      </c:forEach>
</div>
</c:if>

<%-- Add to list --%>
<c:if test="${!empty PROFILE.savedBags}">
<div style="margin-top:10px">
  <form action="<html:rewrite page="/addToBagAction.do"/>" method="POST">
    <fmt:message key="objectDetails.addToBag"/>
    <input type="hidden" name="__intermine_forward_params__" value="${pageContext.request.queryString}"/>
    <select name="bag">
      <option name="">--------</option>
      <c:forEach items="${PROFILE.savedBags}" var="entry">
       <c:if test="${empty filteredWebSearchables[entry.key]}">
        <option name="${entry.key}">${entry.key}</option>
       </c:if>
      </c:forEach>
    </select>
    <input type="hidden" name="object" value="${object.id}"/>
    <input type="submit" value="<fmt:message key="button.add"/>"/>
  </form>
</div>

</c:if>
</div>
</c:if>
<!-- /objectDetailsInList.jsp -->
