<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- queryBuilderBrowserLine.jsp -->

<html:xhtml/>

      <c:if test="${node.indentation > 0}">
        &nbsp;&nbsp;&nbsp;&nbsp;
        <c:forEach var="structure" items="${node.structure}">
          <!-- <img src="images/tree_${structure}.png" align="top" height="16" width="15"/> -->
          <span class="tree_${structure}">
            <span class="ver"></span>
            <span class="hor">-</span>
          </span>

        </c:forEach>
      </c:if>
      <a name="${node.pathString}">&nbsp;</a>
      <!-- empty collection/reference or attribute -->
      <c:set var="isNull" value="${node.isNull}"/>
      <c:if test="${isNull}">
      	<c:choose>
      		<c:when test="${node.button}"><span class="nullStrike"></c:when>
      		<c:otherwise><span class="nullAttribute"></c:otherwise>
      	</c:choose>
      </c:if>
      <%-- construct the real path for this node --%>
      <c:set var="fullpath" value="${node.pathString}"/>
      <c:choose>
      	<c:when test="${node.button == '+' || node.button == '-'}">
      		<c:choose>
		        <c:when test="${isNull}">
		          	<img class="toggle" border="0" src="images/plus-disabled.gif" width="11" height="11" title="+"/>
		        </c:when>
		        <c:when test="${node.button == '+'}">
		          <html:link action="/queryBuilderChange?method=changePath&amp;path=${node.pathString}"
		            title="${node.pathString}"
		            onclick="return toggleNode('${node.pathString}', '${node.pathString}')">
		            <img class="toggle" id="img_${node.pathString}" border="0" src="images/plus.gif" width="11" height="11" title="+"/>
		          </html:link>
		        </c:when>
		        <c:when test="${node.button == '-'}">
		          <html:link action="/queryBuilderChange?method=changePath&amp;path=${node.prefix}"
		            title="${node.pathString}"
		            onclick="return toggleNode('${node.pathString}', '${node.pathString}');">
		            <img class="toggle" id="img_${node.pathString}" border="0" src="images/minus.gif" width="11" height="11" title="-"/>
		          </html:link>
		        </c:when>
	        </c:choose>
	    </c:when>
        <c:otherwise>
          <img src="images/blank.gif" width="11" height="11" title=" "/>
        </c:otherwise>
      </c:choose>
      <c:if test="${node.reverseReference}">
        <fmt:message key="query.reverseReference" var="tooltipReverse"/>
        <img border="0" src="images/reverse.png" width="11" height="11" title="${tooltipReverse}"/>
      </c:if>
      <im:viewableSpan path="${fullpath}" node="${node}" idPrefix="browser">
        <c:if test="${node.indentation > 0}">
          <c:choose>
            <c:when test="${node.collection}">
              <c:set var="fieldNameClass" value="collectionField"/>
            </c:when>
            <c:when test="${node.reverseReference}">
              <c:set var="fieldNameClass" value="reverseReference"/>
            </c:when>
            <c:when test="${node.reference}">
              <c:set var="fieldNameClass" value="referenceField"/>
            </c:when>
            <c:otherwise>
              <c:set var="fieldNameClass" value="attributeField"/>
            </c:otherwise>
          </c:choose>
          <c:if test="${isNull}">
            <c:set var="fieldNameClass" value="${fieldNameClass} nullReferenceField"/>
          </c:if>
          <span class="${fieldNameClass}" id="drag_${node.pathString}">
              <im:displayfield path="${node.minimalPath}"/>
          </span>
          <im:typehelp type="${node.parentType}.${node.fieldName}"/>
        </c:if>
        <span class="collectionDescription ${isNull ? 'nullReferenceField' : ''}">
        <c:choose>
        <c:when test="${node.pathString == path}">
            <c:set var="type" value="typeSelected"/>
        </c:when>
        <c:otherwise>
            <c:set var="type" value="type"/>
        </c:otherwise>
        </c:choose>
          <c:if test="${node.type != 'String'}">
            <c:choose>
              <c:when test="${node.reverseReference && node.reference}">
                <span class="reverseReference"><c:out value="${imf:formatPathStr(node.type, INTERMINE_API, WEBCONFIG)}"/></span>
              </c:when>
              <c:when test="${node.origType != null}">
                <span class="${type}"><c:out value="${imf:formatPathStr(node.origType, INTERMINE_API, WEBCONFIG)}"/></span>
                <fmt:message key="query.usingSubclasses" var="tooltipSubclasses">
                  <fmt:param value="${node.origType}"/>
                  <fmt:param value="${node.type}"/>
                </fmt:message>
                <img class="arrow" src="images/usingSubclasses.png" title="${tooltipSubclasses}"/>
                <span class="subclass"><c:out value="${imf:formatPathStr(node.type, INTERMINE_API, WEBCONFIG)}"/></span><c:if test="${!isNull}">&nbsp;<im:typehelp type="${node.type}"/></c:if>
              </c:when>
              <c:when test="${node.hasSubclasses}">
                <span class="${type}">${imf:formatPathStr(node.type, INTERMINE_API, WEBCONFIG)}</span><c:if test="${!isNull}">&nbsp;<im:typehelp type="${node.type}"/></c:if>
                <fmt:message key="query.hasSubclasses" var="tooltipSubclasses">
                  <fmt:param value="${node.type}"/>
                </fmt:message>
                <img class="arrow" src="images/hasSubclasses.png" title="${tooltipSubclasses}"/>
              </c:when>
              <c:otherwise>
              <span class="${type}"">${imf:formatPathStr(node.type, INTERMINE_API, WEBCONFIG)}</span><c:if test="${!isNull}">&nbsp;<im:typehelp type="${node.type}"/></c:if>
              </c:otherwise>
            </c:choose>
          </c:if>
        </span>
      </im:viewableSpan>
      <c:choose>
        <c:when test="${node.indentation > 0}">
          <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
            <fmt:param value="${node.fieldName}"/>
          </fmt:message>
          <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
            <fmt:param value="${node.fieldName}"/>
          </fmt:message>
        </c:when>
        <c:otherwise>
          <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
            <fmt:param value="${node.type}"/>
          </fmt:message>
          <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
            <fmt:param value="${node.type}"/>
          </fmt:message>
        </c:otherwise>
      </c:choose>
      <c:set var="summary" value="${!node.attribute}" />
      <c:if test="${!(node.reverseReference && node.reference)}">
        <c:choose>
          <c:when test="${!node.selected && !isNull && summary && KEYLESS_CLASSES_MAP[node.type] == null}">
            <html:link action="/queryBuilderChange?method=addToView&amp;path=${node.pathString}#anchor=${node.pathString}" title="${selectNodeTitle}">
              <img class="arrow" src="images/show-ref.gif" width="60" height="13"/>
            </html:link>
          </c:when>
          <c:when test="${summary}"><img class="arrow" src="images/show-ref-disabled.gif" width="60" height="13" title="show"/></c:when>
          <c:when test="${!node.selected && !isNull}">
            <html:link action="/queryBuilderChange?method=addToView&amp;path=${node.pathString}#anchor=${node.pathString}" title="${selectNodeTitle}">
              <img class="arrow" src="images/show.gif" width="43" height="13"/>
            </html:link>
          </c:when>
          <c:otherwise><img class="arrow" src="images/show-disabled.gif" width="43" height="13" title="show"/></c:otherwise>
        </c:choose>
        <c:choose>
          <c:when test="${isNull || !node.canCreateConstraint}">
            <img class="arrow" src="images/constrain-disabled.gif" width="70"
                 height="13" title="constrain"/>
          </c:when>
          <c:otherwise>
            <c:if test="${node.type != 'ClobAccess'}">
	            <html:link action="/queryBuilderChange?method=newConstraint&path=${node.pathString}#${node.pathString}" title="${addConstraintToTitle}"
	                onclick="return addConstraint('${node.pathString}', '${imf:formatPathStr(node.pathString, INTERMINE_API, WEBCONFIG)}');" >
	              <img class="arrow" src="images/constrain.gif" width="70" height="13"/>
	            </html:link>
            </c:if>
          </c:otherwise>
        </c:choose>
        <c:if test="${isNull}">
          </span>
          <c:choose>
            <c:when test="${node.reference}">
              <fmt:message key="query.nullRefHelp" var="strikeThruHelp">
                <fmt:param value="${node.parentType}"/>
                <fmt:param value="${node.fieldName}"/>
              </fmt:message>
            </c:when>
            <c:when test="${node.collection}">
              <fmt:message key="query.emptyCollHelp" var="strikeThruHelp">
                <fmt:param value="${node.parentType}"/>
                <fmt:param value="${node.fieldName}"/>
              </fmt:message>
            </c:when>
            <c:otherwise>
              <%-- null attribute help? --%>
            </c:otherwise>
          </c:choose>
          <im:helplink text="${strikeThruHelp}"/>
        </c:if>
      </c:if>

<!-- /queryBuilderBrowserLine.jsp -->
