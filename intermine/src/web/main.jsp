<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- main.jsp -->
<table class="query" width="100%" cellspacing="0">
  <tr>
    <td rowspan="2" valign="top" width="50%">
      <fmt:message key="query.currentclass"/><br/>
      <c:if test="${!empty navigation}">
        <c:forEach items="${navigation}" var="entry" varStatus="status">
          <html:link action="/mainChange?method=changePath&prefix=${entry.value}&path=${QUERY[entry.value].type}">
            <c:out value="${entry.key}"/>
          </html:link>
          <c:if test="${!status.last}">&gt;</c:if>
        </c:forEach>
        <br/><br/>
      </c:if>
      <c:forEach var="node" items="${nodes}">
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
          <span class="metadata"><c:out value="${node.fieldName}"/></span>
        </c:if>
        <span class="type">
          <c:out value="${node.type}"/>
          <c:if test="${node.collection}"> collection</c:if>
        </span>
        <html:link action="/mainChange?method=addToView&path=${node.path}">
          <fmt:message key="query.selectNode"/>
        </html:link>
        <fmt:message key="query.addConstraintTitle" var="addConstraintToTitle"/>
        <c:if test="${node.indentation > 0}">
          <html:link action="/mainChange?method=addPath&path=${node.path}"
                     title="${addConstraintToTitle} ${node.fieldName}">
            <img class="arrow" src="images/right-arrow.png" alt="->"/>
          </html:link>
        </c:if>
        <br/>
      </c:forEach>
      
      
    </td>
    <td valign="top">
      <fmt:message key="query.currentquery"/><br/>
      
      <c:forEach var="entry" items="${QUERY}" varStatus="status">
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
              <html:link action="/mainChange?method=changePath&prefix=${node.path}&path=${node.type}">
                <c:out value="${node.type}"/>
              </html:link>
              <c:if test="${node.collection}"> collection</c:if>
            </c:otherwise>
          </c:choose>
        </span>
        <html:link action="/mainChange?method=addConstraint&path=${node.path}">
          <fmt:message key="query.addConstraint"/>
        </html:link>
        <c:if test="${node.indentation > 0}">
          <html:link action="/mainChange?method=removeNode&path=${node.path}">
            <fmt:message key="query.removeNode"/>
          </html:link>
        </c:if>
        <br/>
        <c:forEach var="constraint" items="${node.constraints}" varStatus="status">
          <c:forEach begin="0" end="${node.indentation}">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</c:forEach>
          <span class="constraint">
            <c:out value="${constraint.op}"/>
            <c:choose>
              <c:when test="${constraint.value.class.name == 'java.util.Date'}">
                <fmt:formatDate dateStyle="SHORT" value="${constraint.value}"/>
              </c:when>
              <c:otherwise>
                <c:out value=" ${constraint.value}"/>
              </c:otherwise>
            </c:choose>
          </span>
          <html:link action="/mainChange?method=removeConstraint&path=${node.path}&index=${status.index}">
            <fmt:message key="query.removeConstraint"/>
          </html:link>
          <br/>
        </c:forEach>
      </c:forEach>
      
      
    </td>
  </tr>
  

  <c:if test="${editingNode != null}">
    <tr>
      <td valign="top">
        Constrain
        <span class="metadata"><c:out value="${editingNode.fieldName}"/></span>
        <span class="type">
          <c:out value="${editingNode.type}"/>
          <c:if test="${node.collection}"> collection</c:if>:
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
              <html:submit property="attribute"/>
            </c:when>
            <c:otherwise>
              <c:if test="${editingNode.indentation != 0}">
                <fmt:message key="query.subclassConstraint"/>
                <html:select property="subclassValue">
                  <c:forEach items="${subclasses}" var="subclass">
                    <html:option value="${subclass}">
                      <c:out value="${subclass}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
              <html:submit property="subclass"/>
              </c:if>
            </c:otherwise>
          </c:choose>
          <c:if test="${!empty SAVED_BAGS}">
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
              <c:forEach items="${SAVED_BAGS}" var="bag">
                <html:option value="${bag.key}">
                  <c:out value="${bag.key}"/>
                </html:option>
              </c:forEach>
            </html:select>
            <html:submit property="bag"/>
          </c:if>
        </html:form>
      </td>
    </tr>      
  </c:if>
  
  
</table>
<!-- /main.jsp -->
