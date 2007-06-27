<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<!-- mainPaths.jsp -->

<html:xhtml/>

<script>
  <!--
  function editConstraint(path, index) {
    if (isExplorer()) {
      return true;
    }
    new Ajax.Updater('mainConstraint', '<html:rewrite action="/mainChange"/>',
      {parameters:'method=ajaxEditConstraint&path='+path+'&index='+index,
       asynchronous:true, evalScripts:true});
    return false;
  }
  //-->
</script>

<div class="heading">
  <fmt:message key="query.currentquery"/>
  <im:manualLink section="manualPageQB.shtml#manualConstrainListQB"/>
</div>
<div class="body">
<div>
  <fmt:message key="query.currentquery.detail"/>
</div>
<br/>
<c:choose>
  <c:when test="${empty QUERY.nodes}">
    <div class="smallnote altmessage"><fmt:message key="query.empty"/></div>
  </c:when>
  <c:otherwise>
    <c:forEach var="entry" items="${QUERY.nodes}" varStatus="status">
      <div>
        <div style="white-space: nowrap">
          <div>
            <c:set var="node" value="${entry.value}"/>
            <c:if test="${node.indentation > 0}">
              <c:forEach begin="1" end="${node.indentation}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </c:forEach>
            </c:if>
            <im:viewableSpan path="${node.pathString}" viewPaths="${viewPaths}" test="${!empty node.fieldName}" idPrefix="query">
              <span class="attributeField"><c:out value="${node.fieldName}"/></span>
            </im:viewableSpan>
            <span class="type">
              <c:choose>
                <c:when test="${node.attribute}">
                  <%--<c:out value="${node.type}"/>--%>
                </c:when>
                <c:otherwise>
                  <fmt:message key="query.changePath" var="changePathTitle">
                    <fmt:param value="${node.type}"/>
                  </fmt:message>
                  <html:link action="/mainChange?method=changePath&amp;prefix=${node.pathString}&amp;path=${node.type}"
                             title="${changePathTitle}">
                    <im:viewableSpan path="${node.pathString}" viewPaths="${viewPaths}" test="${empty node.fieldName}" idPrefix="query">
                      <span class="type"><c:out value="${node.type}"/></span>
                    </im:viewableSpan>
                  </html:link>
                  <c:if test="${node.collection}">
                    <fmt:message key="query.collection"/>
                  </c:if>
                </c:otherwise>
              </c:choose>
            </span>
            <c:choose>
              <c:when test="${node.indentation > 0}"> 
                <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
                  <fmt:param value="${node.fieldName}"/>
                </fmt:message>
              </c:when>
              <c:otherwise>
                <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
                  <fmt:param value="${node.type}"/>
                </fmt:message>
              </c:otherwise>
            </c:choose>
            <%--
            <html:link action="/mainChange?method=addConstraint&amp;path=${node.pathString}"
                       title="${addConstraintToTitle}">
              <img class="arrow" src="images/constrain.gif" width="70" height="13" alt="constrain" style="vertical-align:middle"/>
            </html:link>
            --%>
            <c:if test="${!lockedPaths[node.pathString]}">
              <fmt:message key="query.removeNodeTitle" var="removeNodeTitle">
                <fmt:param value="${node.fieldName}"/>
              </fmt:message>
              <html:link action="/mainChange?method=removeNode&amp;path=${node.pathString}"
                         title="${removeNodeTitle}">
                <img border="0" src="images/cross.gif" width="13" height="13" alt="x"/>
              </html:link>
            </c:if>
            <c:if test="${lockedPaths[node.pathString]}">
              <img border="0" src="images/discross.gif" width="13" height="13"
                   alt="x" title="<fmt:message key="query.disabledRemoveNodeTitle"/>"/>
            </c:if>
          </div>
          <c:forEach var="constraint" items="${node.constraints}" varStatus="status">
            <div>
              <c:forEach begin="0" end="${node.indentation}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </c:forEach>
              <span class="constraint">
                <im:displayableOpName opName="${constraint.op}" valueType="${node.type}"/>
                <c:choose>
                  <c:when test="${(node.reference || node.collection) && constraint.op == '='}">
                    <c:out value=" ${fn:replace(constraintDisplayValues[constraint], '.', ' > ')}"/>
                  </c:when>
                  <c:when test="${constraint.value.class.name == 'java.util.Date'}">
                    <fmt:formatDate dateStyle="SHORT" value="${constraint.value}"/>
                  </c:when>
                  <c:otherwise>
                    <c:out value=" ${constraintDisplayValues[constraint]}"/>
                  </c:otherwise>
                </c:choose>
              </span>
              <fmt:message key="query.removeConstraintTitle" var="removeConstraintTitle"/>
              <html:link action="/mainChange?method=removeConstraint&amp;path=${node.pathString}&amp;index=${status.index}"
                         title="${removeConstraintTitle}">
                <img border="0" src="images/cross.gif" width="13" height="13" alt="x"/>
              </html:link>
              <fmt:message key="query.editConstraintTitle" var="editConstraintTitle"/>
              <html:link action="/mainChange?method=editConstraint&amp;path=${node.pathString}&amp;index=${status.index}"
                         onclick="return editConstraint('${node.pathString}', '${status.index}')"
                         title="${editConstraintTitle}">
                <img border="0" src="images/edit.gif" width="13" height="13" alt="x"/>
              </html:link>
              <c:if test="${TEMPLATE_BUILD_STATE != null}">
                <c:choose>
                  <c:when test="${constraint.editableInTemplate}">
                    <html:link action="/mainChange?method=editTemplateConstraint&amp;path=${node.pathString}&amp;index=${status.index}"
                               titleKey="templateBuilder.editTemplateConstraint.linktitle">
                      <c:choose>
                        <c:when test="${constraint.editable}">
                          <img border="0" src="images/unlocked.gif" width="13" height="13" alt="unlocked"/>
                        </c:when>
                        <c:otherwise>
                          <img border="0" src="images/locked.gif" width="13" height="13" alt="locked"/>
                        </c:otherwise>
                      </c:choose>
                    </html:link>
                  </c:when>
                  <c:otherwise>
                    <img border="0" src="images/locked-disabled.gif" width="13" height="13" alt="locked"
                      title="<fmt:message key="templateBuilder.constraintNotEditable"/>"/>
                  </c:otherwise>
                </c:choose>
              </c:if>
              <c:if test="${fn:length(QUERY.allConstraints) > 1}">
                (<b>${constraint.code}</b>)
              </c:if>
            </div>
            <c:if test="${TEMPLATE_BUILD_STATE != null && constraint.editable && node.attribute}">
              <div>
                <c:forEach begin="0" end="${node.indentation+1}">
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                </c:forEach>
                <html:link action="/mainChange?method=editTemplateConstraint&amp;path=${node.pathString}&amp;index=${status.index}"
                           titleKey="templateBuilder.editTemplateConstraint.linktitle" styleClass="constraintDesc">
                  <c:choose>
                    <c:when test="${empty constraint.description}">
                      &lt;no label&gt;
                    </c:when>
                    <c:when test="${fn:length(constraint.description) > 30}">
                      ${fn:substring(constraint.description, 0, 30)}...
                    </c:when>
                    <c:otherwise>
                      ${constraint.description}
                    </c:otherwise>
                  </c:choose>
                </html:link>
              </div>
              <c:if test="${IS_SUPERUSER && !empty constraint.identifier}">
                <div>
                  <c:forEach begin="0" end="${node.indentation+1}">
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                  </c:forEach>
                  <html:link action="/mainChange?method=editTemplateConstraint&amp;path=${node.pathString}&amp;index=${status.index}"
                           titleKey="templateBuilder.editTemplateConstraint.linktitle" styleClass="constraintId">
                    ${constraint.identifier}
                  </html:link>
                </div>
              </c:if>
            </c:if>
          </c:forEach>
        </div>
      </div>
    </c:forEach>
  </c:otherwise>
</c:choose>
</div>

<!-- /mainPaths.jsp -->
