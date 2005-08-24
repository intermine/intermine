<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- historyQueryView.jsp -->
<html:xhtml/>

<tiles:useAttribute id="type" name="type"/>

<c:choose>
  <c:when test="${type == 'saved'}">
    <c:set var="queryMap" value="${PROFILE.savedQueries}"/>
  </c:when>
  <c:otherwise>
    <c:set var="queryMap" value="${PROFILE.history}"/>
  </c:otherwise>
</c:choose>

  <im:heading id="${type}">
    <fmt:message key="${type == 'saved' ? 'query.savedqueries.header' : 'query.history.header'}"/>
  </im:heading>
  <im:body id="${type}">
  
    <%-- Choose the queries to display --%>
    <c:choose>
      <c:when test="${empty queryMap}">
        <div class="altmessage">
          None
        </div>
      </c:when>
      <c:otherwise>
        <html:form action="/modifyQuery">
        <table class="results history" cellspacing="0">
          <tr>
            <th>
              &nbsp;
            </th>
            <th align="left" colspan="2" nowrap>
              <fmt:message key="query.savedqueries.namecolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="query.savedqueries.datecreatedcolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="query.savedqueries.countcolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="query.savedqueries.startcolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="query.savedqueries.summarycolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="history.actionscolumnheader"/>
            </th>
          </tr>    
          <c:forEach items="${queryMap}" var="savedQuery">
            <tr>
              <td>
                <html:multibox property="selectedQueries">
                  <c:out value="${savedQuery.key}"/>
                </html:multibox>
              </td>
              <%--
              <tiles:insert page="historyElementName.jsp" flush="true">
                <tiles:put name="name" value="${savedQuery.key}"/>
                <tiles:put name="type" value="${type}"/>
              </tiles:insert>
              --%>
              <tiles:insert name="historyElementName.jsp">
                <tiles:put name="name" value="${savedQuery.key}"/>
                <tiles:put name="type" value="${type}"/>
              </tiles:insert>
              
              <%--
                <c:choose>
                  <c:when test="${param.action=='rename' && param.type==type && param.name==savedQuery.key}">
                  
                    <td align="left" colspan="2" nowrap>
                  
<script type="text/javascript">
<!--
window.onload = function() {
  document.getElementById("renameEntry").focus();
}
// -->
</script>
                    <input type="text" name="newName" value="${savedQuery.key}" size="10" id="renameEntry"/>
                    <input type="hidden" name="name" value="${savedQuery.key}"/>
                    <input type="hidden" name="type" value="${type}"/>
                    <input type="submit" name="rename" value="Rename"/>
                    
                    </td>
                    
                  </c:when>
                  <c:otherwise>
                    <td align="left" class="noRightBorder">
                    <html:link action="/modifyQueryChange?method=load&amp;name=${savedQuery.key}&type=${type}">
                      <c:out value="${savedQuery.key}"/>
                    </html:link>
                    </td>
                    <td align="right" valign="middle" width="1">
                    <html:link action="/history?action=rename&amp;name=${savedQuery.key}&type=${type}">
                      <img border="0" src="images/edit.gif" width="13" height="13" alt="x"/>
                    </html:link>
                    </td>
                  </c:otherwise>
                </c:choose>
                
                --%>
                
              </td>
              <td align="center" nowrap>
                <c:choose>
                  <c:when test="${savedQuery.value.dateCreated != null}">
                    <fmt:formatDate value="${savedQuery.value.dateCreated}" type="both" pattern="dd/M/yy K:mm a"/>
                  </c:when>
                  <c:otherwise>
                    n/a
                  </c:otherwise>
                </c:choose>
              </td>
              <td align="right">
                <c:choose>
                  <c:when test="${savedQuery.value.pathQuery.info != null}">
                    <c:out value="${savedQuery.value.pathQuery.info.rows}"/>
                  </c:when>
                  <c:otherwise>
                    n/a
                  </c:otherwise>
                </c:choose>
              </td>
              <td align="left" nowrap>
                <c:forEach items="${savedQuery.value.pathQuery.view}" var="item" varStatus="status">
                  <c:if test="${status.first}">
                    <c:choose>
                      <c:when test="${fn:indexOf(item, '.') > 0}">
                        <span class="historySummaryRoot">${fn:substringBefore(item, '.')}</span>
                      </c:when>
                      <c:otherwise>
                        <span class="historySummaryRoot">${item}</span>
                      </c:otherwise>
                    </c:choose>
                  </c:if>
                </c:forEach>
              </td>
              <td align="left" nowrap>
                <c:forEach items="${savedQuery.value.pathQuery.view}" var="item">
                  <im:unqualify className="${item}" var="text"/>
                  <span class="historySummaryShowing">${text}</span>
                </c:forEach>
              </td>
              <td align="center" nowrap>
                <c:if test="${type == 'history'}">
                  <html:link action="/modifyQueryChange?method=save&amp;name=${savedQuery.key}">
                    <fmt:message key="history.action.save"/>
                  </html:link>
                  |
                </c:if>
                <html:link action="/exportQuery?name=${savedQuery.key}&type=${type}">
                  <fmt:message key="history.action.export"/>
                </html:link>
              </td>
            </tr>
          </c:forEach>
        </table>
        <br/>
        <html:submit property="delete">
          <fmt:message key="history.delete"/>
        </html:submit>
        </html:form>
        <br/>
      </c:otherwise>
    </c:choose>
    
  </im:body>

<!-- /historyQueryView.jsp -->
