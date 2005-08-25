<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- historyBagView.jsp -->
<html:xhtml/>

<im:heading id="bagHistory">
  <fmt:message key="query.savedbags.header"/>
</im:heading>

<im:body id="bagHistory">

  <c:choose>
    <c:when test="${empty PROFILE.savedBags}">
      <div class="altmessage">
        None
      </div>
    </c:when>
    <c:otherwise>

      <html:form action="/modifyBag">
        <p>
          <fmt:message key="history.savedbags.help"/>
        </p>
        <table class="results history" cellspacing="0">
          <tr>
            <th>
              &nbsp;
            </th>
            <th align="left" colspan="2" nowrap>
              <fmt:message key="query.savedbags.namecolumnheader"/>
            </th>
            <th align="right" nowrap>
              <fmt:message key="query.savedbags.countcolumnheader"/>
            </th>
          </tr>
          <c:forEach items="${PROFILE.savedBags}" var="savedBag">
            <tr>
              <td>
                <html:multibox property="selectedBags">
                  <c:out value="${savedBag.key}"/>
                </html:multibox>
              </td>
                <%--
                <html:link action="/bagDetails?bagName=${savedBag.key}">
                  <c:out value="${savedBag.key}"/>
                </html:link>
                --%>
                
                <tiles:insert name="historyElementName.jsp">
                  <tiles:put name="name" value="${savedBag.key}"/>
                  <tiles:put name="type" value="bag"/>
                </tiles:insert>
              
              <td align="right">
                <c:out value="${savedBag.value.size}"/>
                <c:choose>
                  <c:when test="${fn:endsWith(savedBag.value.class.name, 'InterMineIdBag')}">objects</c:when>
                  <c:otherwise>values</c:otherwise>
                </c:choose>
              </td>
            </tr>
          </c:forEach>
        </table>
        <br/>
        <c:if test="${fn:length(PROFILE.savedBags) >= 2}">
          New bag name:
          <html:text property="newBagName" size="12"/>
          <html:submit property="union">
            <fmt:message key="history.union"/>
          </html:submit>
          <html:submit property="intersect">
            <fmt:message key="history.intersect"/>
          </html:submit>
        </c:if>
        <html:submit property="delete">
          <fmt:message key="history.delete"/>
        </html:submit>
      </html:form>
      <br/>

    </c:otherwise>
  </c:choose>

</im:body>
  
<!-- /historyBagView.jsp -->
