<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- tree.jsp -->
<c:forEach var="node" items="${nodes}">
  <c:forEach begin="0" end="${node.indentation}">&nbsp;&nbsp;&nbsp;&nbsp;</c:forEach>
  <c:choose>
    <c:when test="${node.leaf}">
      <img src="images/blank.png"/>
    </c:when>
    <c:when test="${node.open}">
      <html:link action="/changeTree?method=collapse&node=${node.object}">
        <img border="0" src="images/minus.png" alt="-"/>
      </html:link>
    </c:when>
    <c:otherwise>
      <html:link action="/changeTree?method=expand&node=${node.object}">
        <img border="0" src="images/plus.png" alt="+"/></html:link>
    </c:otherwise>
  </c:choose>
  <font
    <c:if test="${node.open}">
class="treeOpen"
    </c:if>
  >
  <font
    <c:if test="${node.selected}">
class="treeSelected"
    </c:if>
  >
    <html:link action="/changeTree?method=select&node=${node.object}">
      <c:out value="${node.object}"/><br/>
    </html:link>
  </font>
  </font>
</c:forEach>
<!-- /tree.jsp -->