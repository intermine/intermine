<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- tree.jsp -->
<div class="body modelBrowser">
  <c:forEach var="node" items="${nodes}">
    <div>
      <c:forEach begin="0" end="${node.indentation}">
        &nbsp;&nbsp;&nbsp;&nbsp;
      </c:forEach>
      <c:choose>
        <c:when test="${node.leaf}">
          <img src="images/blank.gif"/>
        </c:when>
        <c:when test="${node.open}">
          <html:link action="/changeTree?method=collapse&node=${node.object}" anchor="${node.object}">
            <img border="0" src="images/minus.gif" alt="-"/>
          </html:link>
        </c:when>
        <c:otherwise>
          <html:link action="/changeTree?method=expand&node=${node.object}" anchor="${node.object}">
            <img border="0" src="images/plus.gif" alt="+"/>
          </html:link>
        </c:otherwise>
      </c:choose>
      <span
         <c:if test="${node.open}">
  class="treeOpen"
         </c:if>
      >
      <span   
         <c:if test="${node.selected}">
  class="treeSelected"
         </c:if>
      >
      <a name="${node.object}"/>
      <html:link action="/changeTree?method=select&node=${node.object}">
        <im:unqualify className="${node.object}" var="name"/>
        <span class="type">${name}</span>
      </html:link>
      <im:typehelp type="${name}"/>
      </span>
      </span>
      ${node.text}
    </div>
  </c:forEach>
</div>
<!-- /tree.jsp -->
