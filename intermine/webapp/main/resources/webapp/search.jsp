<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- search.jsp -->

<html:xhtml/>

<script type="text/javascript">
<!--
window.onload = function() { document.getElementById("queryString").focus(); }
// -->
</script>

<div class="body">
  <c:if test="${!empty results}">
    <p>
      <b>${resultCount}</b> results for <b>${queryString}</b>. <span class="searchTime">(${querySeconds} seconds)</span>
      <c:if test="${empty PROFILE_MANAGER || empty PROFILE.username}">
        <br/><br/>
        <i>
          <fmt:message key="${type}.notlogged">
            <fmt:param>
              <im:login/>
            </fmt:param>
          </fmt:message>
        </i>
      </c:if>
    </p>
    <c:forEach items="${results}" var="entry" varStatus="status">
      <%--
         <span style="width:111px"><fmt:formatNumber value="${entry.value*100}" maxFractionDigits="0"/>%</span>
      --%>
      <fmt:formatNumber value="${entry.value*10}" maxFractionDigits="0" var="heat"/>
      <img class="searchHeatImg" src="images/heat${heat}.gif" width="${heat*2}" height="10"
           style="margin-right:${24-(heat*2)}px"/>
      <c:choose>
        <c:when test="${type == 'template'}">
          <im:templateLine scope="${resultScopes[entry.key]}" 
                           templateQuery="${entry.key}" 
                           name="${highlighted[entry.key]}"
                           descr="${descriptions[entry.key]}" />
        </c:when>
        <c:when test="${type == 'bag'}">
          <im:bagLine scope="${resultScopes[entry.key]}" 
                      interMineBag="${entry.key}" desc="${highlighted[entry.key]}"/>
        </c:when>
        <c:otherwise>
          <%-- unknown type --%>
        </c:otherwise>
      </c:choose>
      <c:if test="${!status.last}">
        <hr class="tmplSeperator"/>
      </c:if>
    </c:forEach>
  </c:if>
  
  <c:if test="${empty results && !empty param.queryString}">
    <p>
      <b><fmt:message key="search.noresults"/></b>
    </p>
  </c:if>
  
  <p>
    <html:link action="/templates?scope=global"><fmt:message key="search.viewall"/></html:link>
  </p>
  
</div>


<!-- /search.jsp -->
