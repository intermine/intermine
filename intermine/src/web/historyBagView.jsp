<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html-el.tld" prefix="html" %>

<!-- historyBagView.jsp -->
<div class="historyView">
  <c:if test="${!empty SAVED_BAGS}">
    <span class="historyViewTitle">
      <fmt:message key="query.savedbags.header"/>
    </span>
    <br/><br/>
    <table class="results" cellspacing="0">
      <tr>
        <th align="left">
          <fmt:message key="query.savedbags.namecolumnheader"/>
        </th>
        <th align="right">
          <fmt:message key="query.savedbags.countcolumnheader"/>
        </th>
        <th/>
      </tr>
      <c:forEach items="${SAVED_BAGS}" var="savedBag">
        <tr>
          <td align="left">
            <html:link action="/bagDetails?bagName=${savedBag.key}">
              <c:out value="${savedBag.key}"/>
            </html:link>
          </td>
          <td align="right">
            <c:out value="${savedBag.value.size}"/>
          </td>
          <td>
            <html:link action="/deleteBag?name=${savedBag.key}">delete</html:link>
          </td>
        </tr>
      </c:forEach>
    </table>
  </c:if>
</div>
<!-- /historyBagView.jsp -->
