<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

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
      &nbsp;&nbsp;&nbsp;&nbsp;
      <c:forEach var="structure" items="${node.structure}">
        <img src="images/tree_${structure}.png" align="top"/>
      </c:forEach>
      <c:choose>
        <c:when test="${node.leaf}">
          <img src="images/blank.gif" />
        </c:when>
        <c:when test="${node.open}">
          <html:link action="/changeTree?method=collapse&amp;node=${node.object.name}" anchor="${node.object.name}">
            <img border="0" src="images/minus.gif" title="Click here to hide children"/>
          </html:link>
        </c:when>
        <c:otherwise>
          <html:link action="/changeTree?method=expand&amp;node=${node.object.name}" anchor="${node.object.name}">
            <img border="0" src="images/plus.gif" title="Click here to show children"/>
          </html:link>
        </c:otherwise>
      </c:choose>
      <c:if test="${node.open}">
        <c:set var="cssClass" value="treeOpen"/>
      </c:if>
      <span class="${cssClass}">
        <c:if test="${node.selected}">
            <c:set var="cssClass" value="treeSelected"/>
        </c:if>
        <span class="${cssClass}">
            <a name="${node.object.name}"/>
            <im:unqualify className="${node.object.name}" var="name"/>
            <c:set var="displayName" value="${imf:formatPathStr(name, INTERMINE_API, WEBCONFIG)}"/>
            <c:choose>
            <c:when test="${node.object.name == 'org.intermine.model.InterMineObject'}">
                    ${displayName}
            </c:when>
            <c:otherwise>
                <html:link action="/changeTree?method=select&amp;node=${node.object.name}">
                    ${displayName}
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
