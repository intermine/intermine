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
    <c:set var="messageKey" value="history.savedQueries.help"/>
  </c:when>
  <c:otherwise>
    <c:set var="queryMap" value="${PROFILE.history}"/>
    <c:set var="messageKey" value="history.history.help"/>
  </c:otherwise>
</c:choose>

  <im:heading id="${type}">
    <fmt:message key="${type == 'saved' ? 'history.savedqueries.header' : 'history.history.header'}"/>
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
        <p>
          <fmt:message key="${messageKey}"/>
        </p>
        <html:form action="/modifyQuery">
        <table class="results history" cellspacing="0">
          <tr>
            <th>
              &nbsp;
            </th>
            <th align="left" colspan="2" nowrap>
              <fmt:message key="history.namecolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="history.datecreatedcolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="history.countcolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="history.startcolumnheader"/>
            </th>
            <th align="center" nowrap>
              <fmt:message key="history.summarycolumnheader"/>
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
              
              <tiles:insert name="historyElementName.jsp">
                <tiles:put name="name" value="${savedQuery.key}"/>
                <tiles:put name="type" value="${type}"/>
              </tiles:insert>
              
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
                  <c:if test="${!empty PROFILE.username}">
                    <html:link action="/modifyQueryChange?method=save&amp;name=${savedQuery.key}"
                               titleKey="history.action.save.hover">
                      <fmt:message key="history.action.save"/>
                    </html:link>
                    |
                  </c:if>
                </c:if>
                <html:link action="/exportQuery?name=${savedQuery.key}&type=${type}"
                           titleKey="history.action.export.hover">
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
