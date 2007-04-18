<%@ tag body-content="scriptless" %>
<%@ attribute name="viewPaths" type="java.util.Map" required="true" %>
<%@ attribute name="path" required="true" %>
<%@ attribute name="idPrefix" required="true" %>
<%@ attribute name="idPostfix" required="false" %>
<%@ attribute name="test" type="java.lang.Boolean" required="false" %>
<%@ attribute name="disabled" type="java.lang.Boolean" required="false" %>
<%@ attribute name="errorPath" type="java.lang.Boolean" required="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%--
     Tag that surrounds an element that represents something that could be on
     the view list. It adds a span with the relevant JavaScript to highlight
     all the elements on the page representing the same view path.

     viewPaths - map where keys are paths currently on the view list
     path - path represented by enclosed elements
     errorPath - true if the path has an error (eg. doesn't match the model),
                 should be rendered differently and shouldn't be dragable
     idPrefix - for all the items on the page representing a single path, each
                idPrefix must be unique and all the idPrefixes must be passed
                to viewablejs (see viewablejs.tag)
--%>

<c:choose>
  <c:when test="${errorPath}">
    <div class="errorPath" title="this path doesn't match the current model">
      <jsp:doBody/>
    </div>
  </c:when>
  <c:otherwise>
    <c:if test="${viewPaths[path] && (empty test || test)}">
      <div class="viewpath" id="${idPrefix}${fn:replace(path,".","")}${idPostfix}"
           name="${idPrefix}${fn:replace(path,".","")}"
           title="drag the paths to change the column order"
        <c:if test="${(empty disabled || !disabled)}">
              onmouseover="enterPath('${fn:replace(path,".","")}')"
              onmouseout="exitPath('${fn:replace(path,".","")}')"
              onmousedown="exitPath('${fn:replace(path,".","")}'); disblePathHighlighting()"
        </c:if>
        >
    </c:if>
    <jsp:doBody/>
    <c:if test="${viewPaths[path] && (empty test || test)}">
    </div>
    </c:if>
  </c:otherwise>
</c:choose>
