<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- historyBagView.jsp -->
<div class="historyView">
  <c:if test="${!empty SAVED_BAGS}">
    <html:form action="/modifyBag">
      <span class="historyViewTitle">
        <fmt:message key="query.savedbags.header"/>
      </span>
      <br/><br/>
      <table class="results" cellspacing="0">
        <tr>
          <%-- add a space so that IE renders the borders --%>
          <th>
            &nbsp;
          </th>
          <th align="left">
            <fmt:message key="query.savedbags.namecolumnheader"/>
          </th>
          <th align="right">
            <fmt:message key="query.savedbags.countcolumnheader"/>
          </th>
        </tr>
        <c:forEach items="${SAVED_BAGS}" var="savedBag">
          <tr>
            <td>
              <html:multibox property="selectedBags">
                <c:out value="${savedBag.key}"/>
              </html:multibox>
            </td>
            <td align="left">
              <html:link action="/bagDetails?bagName=${savedBag.key}">
                <c:out value="${savedBag.key}"/>
              </html:link>
            </td>
            <td align="right">
              <c:out value="${savedBag.value.size}"/>
            </td>
          </tr>
        </c:forEach>
      </table>
      <html:submit property="action">
        <fmt:message key="history.delete"/>
      </html:submit>
      <html:submit property="action">
        <fmt:message key="history.union"/>
      </html:submit>
      <html:submit property="action">
        <fmt:message key="history.intersect"/>
      </html:submit>
    </html:form>
  </c:if>
</div>
<!-- /historyBagView.jsp -->
