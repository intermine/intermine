<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>

<tiles:importAttribute/>

<!-- main.jsp -->
<table class="query" width="100%" cellspacing="0">
  <tr>
    <td rowspan="2" valign="top" width="50%">
      Current class:<br/>
      <br/>

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
        <c:choose>
          <c:when test="${node.indentation == 0}">
            <span class="metadata"><c:out value="${node.fieldName}"/></span>
          </c:when>
          <c:otherwise>
            <html:link action="/mainChange?method=addPath&path=${node.path}">
              <span class="metadata"><c:out value="${node.fieldName}"/></span>
            </html:link>
          </c:otherwise>
        </c:choose>
        <span class="type"><c:out value="${node.type}"/><c:if test="${node.collection}"><c:out value=" collection"/></c:if></span>
        <html:link action="/mainChange?method=addToView&path=${node.path}">view</html:link>
        <br/>
      </c:forEach>
      
      
    </td>
    <td valign="top">
      Current query:<br/>
      <br/>
      
      
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
        <html:link action="/mainChange?method=addConstraint&path=${node.path}">constrain</html:link>
        <c:if test="${node.indentation > 0}">
          <html:link action="/mainChange?method=removeNode&path=${node.path}">remove</html:link>
        </c:if>
        <br/>
        <c:forEach var="constraint" items="${node.constraints}" varStatus="status">
          <c:forEach begin="0" end="${node.indentation}">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</c:forEach>
          <span class="constraint"><c:out value="${constraint.op} ${constraint.value}"/></span>
          <html:link action="/mainChange?method=removeConstraint&path=${node.path}&index=${status.index}">remove</html:link>
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
        <span class="type"><c:out value="${editingNode.type}"/></span>:
        <br/>
        <html:form action="/mainAction">
          <html:hidden property="path" value="${editingNode.path}"/>
          <c:choose>
            <c:when test="${editingNode.attribute}">
              <html:select property="constraintOp">
                <c:forEach items="${validOps}" var="validOp">
                  <html:option value="${validOp.key}">
                    <c:out value="${validOp.value}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:text property="constraintValue"/>
            </c:when>
            <c:otherwise>
              <c:if test="${editingNode.indentation != 0}">
                <fmt:message key="query.subclassconstraint"/>
                <html:select property="subclass">
                  <c:forEach items="${subclasses}" var="subclass">
                    <html:option value="${subclass}">
                      <c:out value="${subclass}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
              </c:if>
              <c:if test="${!empty SAVED_BAGS}">
                <br/>
                <fmt:message key="query.bagconstraint"/>
                <html:select property="constraintOp">
                  <c:forEach items="${validOps}" var="validOp">
                    <html:option value="${validOp.key}">
                      <c:out value="${validOp.value}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
                <html:select property="constraintValue">
                  <c:forEach items="${SAVED_BAGS}" var="bag">
                    <html:option value="${bag.key}">
                      <c:out value="${bag.key}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
              </c:if>
            </c:otherwise>
          </c:choose>
          <html:submit/>
        </html:form>
      </td>
    </tr>      
  </c:if>
  

</table>
<br/>
<!-- /main.jsp -->
