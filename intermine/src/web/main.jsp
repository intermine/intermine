<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- main.jsp -->
<table class="query" width="100%" cellspacing="0">
  <tr>
    <td rowspan="2" valign="top" width="50%">
      <fmt:message key="query.currentclass"/><br/>

      <c:if test="${!empty navigation}">
        <c:forEach items="${navigation}" var="entry" varStatus="status">
          <fmt:message key="query.changePath" var="changePathTitle">
            <fmt:param value="${entry.key}"/>
          </fmt:message>
          <html:link action="/mainChange?method=changePath&prefix=${entry.value}&path=${QUERY.nodes[entry.value].type}"
                     title="${changePathTitle}">
            <c:out value="${entry.key}"/>
          </html:link>
          <c:if test="${!status.last}">&gt;</c:if>
        </c:forEach>
        <br/><br/>
      </c:if>
      <c:forEach var="node" items="${nodes}">
        <div>
          <nobr>
            <c:if test="${node.indentation > 0}">
              <c:forEach begin="1" end="${node.indentation}">
                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
              </c:forEach>
            </c:if>
            <c:choose>
              <c:when test="${node.button == '+'}">
                <html:link action="/mainChange?method=changePath&path=${node.path}">
                  <img border="0" src="images/plus.png" alt="+"/>
                </html:link>
              </c:when>
              <c:when test="${node.button == '-'}">
                <html:link action="/mainChange?method=changePath&path=${node.prefix}">
                  <img border="0" src="images/minus.png" alt="-"/>
                </html:link>
              </c:when>
              <c:otherwise>
                <img src="images/blank.png" alt=" "/>
              </c:otherwise>
            </c:choose>
            <c:if test="${node.indentation > 0}">
              <span class="metadata">
                <c:out value="${node.fieldName}"/>
              </span>
            </c:if>
            <span class="type">
              <c:out value="${node.type}"/><c:if test="${!empty classDescriptions[node.type]}"><sup><html:link action="/classDescription?class=${node.type}">?</html:link></sup></c:if>
              <c:if test="${node.collection}">
                <fmt:message key="query.collection"/>
              </c:if>
            </span>
            <c:choose>
              <c:when test="{node.indentation > 0">
                <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
                  <fmt:param value="${node.fieldName}"/>
                </fmt:message>
              </c:when>
              <c:otherwise>
                <fmt:message key="query.showNodeTitle" var="selectNodeTitle">
                  <fmt:param value="${node.type}"/>
                </fmt:message>
              </c:otherwise>
            </c:choose>
            <html:link action="/mainChange?method=addToView&path=${node.path}"
                       title="${selectNodeTitle}">
              <fmt:message key="query.showNode"/>
            </html:link>
            <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle">
              <fmt:param value="${node.fieldName}"/>
            </fmt:message>
            <html:link action="/mainChange?method=addPath&path=${node.path}"
                       title="${addConstraintToTitle}">
              <img class="arrow" src="images/right-arrow.png" alt="->"/>
            </html:link>
          </nobr>
        </div>
      </c:forEach>


    </td>
    <td valign="top">
      <fmt:message key="query.currentquery"/><br/>

      <c:choose>
        <c:when test="${empty QUERY.nodes}">
          <fmt:message key="query.empty"/>
        </c:when>
        <c:otherwise>
          <c:forEach var="entry" items="${QUERY.nodes}" varStatus="status">
            <div>
              <nobr>
                <div>
                  <c:set var="node" value="${entry.value}"/>
                  <c:if test="${node.indentation > 0}">
                    <c:forEach begin="1" end="${node.indentation}">
                      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    </c:forEach>
                  </c:if>
                  <span class="metadata"><c:out value="${node.fieldName}"/></span>
                  <span class="type">
                    <c:choose>
                      <c:when test="${node.attribute}">
                        <c:out value="${node.type}"/>
                      </c:when>
                      <c:otherwise>
                        <fmt:message key="query.changePath" var="changePathTitle">
                          <fmt:param value="${node.type}"/>
                        </fmt:message>
                        <html:link action="/mainChange?method=changePath&prefix=${node.path}&path=${node.type}"
                                   title="${changePathTitle}">
                          <c:out value="${node.type}"/>
                        </html:link>
                        <c:if test="${node.collection}">
                          <fmt:message key="query.collection"/>
                        </c:if>
                      </c:otherwise>
                    </c:choose>
                  </span>
                  <c:choose>
                    <c:when test="{node.indentation > 0"> 
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
                  <html:link action="/mainChange?method=addConstraint&path=${node.path}"
                             title="${addConstraintToTitle}">
                    <fmt:message key="query.addConstraint"/>
                  </html:link>
                  <fmt:message key="query.removeNodeTitle" var="removeNodeTitle">
                    <fmt:param value="${node.fieldName}"/>
                  </fmt:message>
                  <html:link action="/mainChange?method=removeNode&path=${node.path}"
                             title="${removeNodeTitle}">
                    <img border="0" src="images/cross.png" alt="x"/>
                  </html:link>
                </div>
                <c:forEach var="constraint" items="${node.constraints}" varStatus="status">
                  <div>
                    <c:forEach begin="0" end="${node.indentation}">
                      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                    </c:forEach>
                    <span class="constraint">
                      <c:out value="${constraint.op}"/>
                      <c:choose>
                        <c:when test="${constraint.value.class.name == 'java.util.Date'}">
                          <fmt:formatDate dateStyle="SHORT" value="${constraint.value}"/>
                        </c:when>
                        <c:otherwise>
                          <c:out value=" ${constraint.displayValue}"/>
                        </c:otherwise>
                      </c:choose>
                    </span>
                    <fmt:message key="query.removeConstraintTitle" var="removeConstraintTitle"/>
                    <html:link action="/mainChange?method=removeConstraint&path=${node.path}&index=${status.index}"
                               title="${removeConstraintTitle}">
                      <img border="0" src="images/cross.png" alt="x"/>
                    </html:link>
                  </div>
                </c:forEach>
              </nobr>
            </div>
          </c:forEach>
        </c:otherwise>
      </c:choose>


    </td>
  </tr>


  <c:if test="${editingNode != null}">
    <tr>
      <td valign="top">
        <fmt:message key="query.constrain"/>
        <span class="metadata">
          <c:choose>
            <c:when test="${empty editingNode.fieldName}">
              <c:out value="${editingNode.path}"/>
            </c:when>
            <c:otherwise>
              <c:out value="${editingNode.fieldName}"/>
            </c:otherwise>
          </c:choose>
        </span>
        <span class="type">
          <c:choose>
            <c:when test="${editingNode.collection}">
              <fmt:message key="query.collection">
                <fmt:param value="${editingNode.type}"/>
              </fmt:message>
            </c:when>
            <c:otherwise>
              <c:out value="${editingNode.type}"/>
            </c:otherwise>
          </c:choose>
        </span>
        <br/><br/>
        <html:form action="/mainAction">
          <html:hidden property="path" value="${editingNode.path}"/>
          <c:choose>
            <c:when test="${editingNode.attribute}">
              <html:select property="attributeOp">
                <c:forEach items="${attributeOps}" var="attributeOp">
                  <html:option value="${attributeOp.key}">
                    <c:out value="${attributeOp.value}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:text property="attributeValue"/>
              <html:submit property="attribute">
                <fmt:message key="query.submitConstraint"/>
              </html:submit>
            </c:when>
            <c:otherwise>
              <c:if test="${editingNode.indentation != 0 && !empty subclasses}">
                <fmt:message key="query.subclassConstraint"/>
                <html:select property="subclassValue">
                  <c:forEach items="${subclasses}" var="subclass">
                    <html:option value="${subclass}">
                      <c:out value="${subclass}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
                <html:submit property="subclass">
                  <fmt:message key="query.submitConstraint"/>
                </html:submit>
              </c:if>
            </c:otherwise>
          </c:choose>
          <c:if test="${!empty PROFILE.savedBags}">
            <br/>
            <fmt:message key="query.bagConstraint"/>
            <html:select property="bagOp">
              <c:forEach items="${bagOps}" var="bagOp">
                <html:option value="${bagOp.key}">
                  <c:out value="${bagOp.value}"/>
                </html:option>
              </c:forEach>
            </html:select>
            <html:select property="bagValue">
              <c:forEach items="${PROFILE.savedBags}" var="bag">
                <html:option value="${bag.key}">
                  <c:out value="${bag.key}"/>
                </html:option>
              </c:forEach>
            </html:select>
            <html:submit property="bag">
              <fmt:message key="query.submitConstraint"/>
            </html:submit>
          </c:if>
        </html:form>
      </td>
    </tr>
  </c:if>


</table>
<!-- /main.jsp -->