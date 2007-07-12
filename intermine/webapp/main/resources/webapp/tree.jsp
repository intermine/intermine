<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

<!-- tree.jsp -->
<html:xhtml/>

<div class="body">
  <p>
    <fmt:message key="tree.intro"/>
  </p>
</div>
<div class="body modelBrowser">
  <c:forEach var="node" items="${nodes}">
    <div>
      <c:forEach begin="0" end="${node.indentation}">
        &nbsp;&nbsp;&nbsp;&nbsp;
      </c:forEach>
      <c:choose>
        <c:when test="${node.leaf}">
          <img src="images/blank.gif" alt=" "/>
        </c:when>
        <c:when test="${node.open}">
          <html:link action="/changeTree?method=collapse&amp;node=${node.object.name}" anchor="${node.object.name}">
            <img border="0" src="images/minus.gif" alt="-"/>
          </html:link>
        </c:when>
        <c:otherwise>
          <html:link action="/changeTree?method=expand&amp;node=${node.object.name}" anchor="${node.object.name}">
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
        <a name="${node.object.name}"/>
        <c:choose>
          <c:when test="${node.object.name == 'org.intermine.model.InterMineObject'}">
            <im:unqualify className="${node.object.name}" var="name"/>${name}
          </c:when>
          <c:otherwise>
            <html:link action="/changeTree?method=select&amp;node=${node.object.name}">
              <im:unqualify className="${node.object.name}" var="name"/>${name}
            </html:link>
          </c:otherwise>
        </c:choose>
        <im:typehelp type="${name}"/>
      </span>
      </span>
      ${node.text}

      <c:if test="${IS_SUPERUSER}">

        <c:set var="descriptor" value="${node.object}"/>
        <tiles:insert name="inlineTagEditor.tile">
          <tiles:put name="taggable" beanName="descriptor"/>
          <tiles:put name="show" value="true"/>
        </tiles:insert>
      </c:if>

    </div>
  </c:forEach>
</div>
<!-- /tree.jsp -->
