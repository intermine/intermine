<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- historyQueryView.jsp -->
<html:xhtml/>

  <script type="text/javascript" src="js/tablesort.js"></script>
  <link rel="stylesheet" type="text/css" href="css/sorting.css"/>

<tiles:useAttribute id="type" name="type"/>

<c:choose>
  <c:when test="${type == 'saved'}">
    <c:set var="queryMap" value="${PROFILE.savedQueries}"/>
    <c:set var="messageKey" value="history.savedQueries.intro"/>
  </c:when>
  <c:otherwise>
    <c:set var="queryMap" value="${PROFILE.history}"/>
    <c:set var="messageKey" value="history.history.intro"/>
  </c:otherwise>
</c:choose>

<im:body id="${type}">
  <script LANGUAGE="JavaScript">
    <!--//<![CDATA[
    function confirmAction() {
      return confirm("Do you really want to delete the selected queries?")
    }

    function doQueryAction(name, url){
      queryName = document.getElementById('linkBag_'+name).innerHTML.replace(/[\t\n]/g, '');
        location.href = url + trim(queryName);
        return false;
    }
    //]]>-->
  </script>

  <p>
    <fmt:message key="${messageKey}"/>
      <c:if test="${!PROFILE.loggedIn}">
       - <a href="${WEB_PROPERTIES['webapp.baseurl']}/google"><fmt:message key="history.history.login"/></a>&nbsp;&nbsp;
      </c:if>
  </p>
<br/>
    <%-- Choose the queries to display --%>
    <c:choose>
      <c:when test="${empty queryMap}">
        <div class="altmessage">
          <fmt:message key="msgs.noResults"/>
        </div>
      </c:when>
      <c:otherwise>

        <html:form action="/modifyQuery">
        <input type="hidden" name="type" value="${type}"/>
        <table class="sortable-onload-3-reverse rowstyle-alt colstyle-alt no-arrow" cellspacing="0">
        <thead>
          <tr>
            <th>
              <input type="checkbox" id="selected_${type}"
                     onclick="selectColumnCheckbox(this.form, '${type}')">
            </th>
            <th align="left" nowrap class="sortable">
              <fmt:message key="history.namecolumnheader"/>
            </th>
            <th align="center" class="sortable">
              <fmt:message key="history.datecreatedcolumnheader"/>
            </th>
            <th align="center" class="sortable">
              <fmt:message key="history.countcolumnheader"/>
            </th>
            <th align="center" class="sortable">
              <fmt:message key="history.startcolumnheader"/>
            </th>
            <th align="center">
              <fmt:message key="history.summarycolumnheader"/>
            </th>
            <th align="center">
              <fmt:message key="history.actionscolumnheader"/>
            </th>
          </tr>
          </thead>
          <tbody>
          <c:forEach items="${queryMap}" var="savedQuery" varStatus="status">
            <c:if test="${!empty savedQuery.key && !empty savedQuery.value}">
              <c:set var="validQuery" value="${savedQuery.value.pathQuery.valid}"/>
              <tr>
                <td class="sorting" align="center">
                  <html:multibox property="selectedQueries"
                                 styleId="selected_${type}_${status.index}"
                                 onclick="setDeleteDisabledness(this.form, '${type}')">
                    <c:out value="${savedQuery.key}" escapeXml="false"/>
                  </html:multibox>
                </td>

                <td class="sorting">
                <c:choose>
                  <c:when test="${!validQuery}">
                      <span id="linkBag_${savedQuery.key}" class="brokenTmplLink">
                      ${savedQuery.value.name}
                      </span>
                  </c:when>
                  <c:otherwise>
                    <tiles:insert name="renamableElement.jsp">
                      <tiles:put name="name" value="${savedQuery.key}"/>
                      <tiles:put name="type" value="${type}"/>
                      <tiles:put name="index" value="${status.count-1}"/>
                    </tiles:insert>
                  </c:otherwise>
                </c:choose>

                </td>
                <td class="sorting" align="center" nowrap>
                  <c:choose>
                    <c:when test="${savedQuery.value.dateCreated != null}">
                      <im:dateDisplay date="${savedQuery.value.dateCreated}"/>
                    </c:when>
                    <c:otherwise>
                      n/a
                    </c:otherwise>
                  </c:choose>
                </td>
                <td class="sorting" align="right">
                  <c:choose>
                    <c:when test="${infoCache[savedQuery.value.pathQuery] != null}">
                      <c:out value="${infoCache[savedQuery.value.pathQuery].rows}"/>
                    </c:when>
                    <c:otherwise>
                      n/a
                    </c:otherwise>
                  </c:choose>
                </td>
                <td class="sorting" align="left" nowrap>
                  <c:forEach items="${savedQuery.value.pathQuery.view}" var="item" varStatus="status">
                    <c:if test="${status.first}">
                      <c:choose>
                        <c:when test="${fn:indexOf(item, '.') > 0}">
                            <c:set var="rootClass" value="${fn:substringBefore(item, '.')}"/>
                            <span class="historySummaryRoot">
                                <im:displaypath path="${rootClass}"/>
                            </span>
                        </c:when>
                        <c:otherwise>
                            <span class="historySummaryRoot">
                                <im:displaypath path="${item}"/>
                            </span>
                        </c:otherwise>
                      </c:choose>
                    </c:if>
                  </c:forEach>
                </td>
                <td class="sorting" align="left">
                  <c:forEach items="${savedQuery.value.pathQuery.view}" var="item">
                    <im:unqualify className="${item}" var="text"/>
                    <span class="historySummaryShowing">
                        <im:displayfield path="${item}"/>
                    </span>
                  </c:forEach>
                </td>
                <td class="sorting" align="center" nowrap>
                  <c:choose>
                    <c:when test="${validQuery}">
                      <span class="fakelink"
                         onclick="return doQueryAction('${savedQuery.key}', '<html:rewrite action='/modifyQueryChange'/>?method=run&amp;type=${type}&trail=|query&amp;name=')"
                         titleKey="history.action.execute.hover">
                        <fmt:message key="history.action.execute"/></span>
                    </c:when>
                    <c:otherwise>
                      <fmt:message key="history.action.execute"/>
                    </c:otherwise>
                  </c:choose>
                  |
                  <c:choose>
                  <c:when test="${validQuery}">
                  <span class="fakelink"
                     onclick="return doQueryAction('${savedQuery.key}', '<html:rewrite action='/modifyQueryChange'/>?method=load&type=${type}&amp;name=')"
                     titleKey="history.action.edit.hover">
                    <fmt:message key="history.action.edit"/>
                  </span>
                  </c:when>
                  <c:otherwise><fmt:message key="history.action.edit"/></c:otherwise>
                  </c:choose>
                  |
                  <c:if test="${type == 'history'}">
                    <c:if test="${PROFILE.loggedIn}">
                      <c:set var="urlPrefix" value=""/>
                      <span class="fakelink"
                         onclick="return doQueryAction('${savedQuery.key}', '<html:rewrite action='/modifyQueryChange'/>?method=save&amp;name=')"
                         titleKey="history.action.save.hover">
                        <fmt:message key="history.action.save"/></span>
                      |
                    </c:if>
                  </c:if>

                  <span class="fakelink"
                     onclick="return doQueryAction('${savedQuery.key}', '<html:rewrite action='/exportQuery'/>?type=${type}&amp;name=')"
                     titleKey="history.action.export.hover">
                    <fmt:message key="history.action.export"/></span>
                </td>
              </tr>
            </c:if>
          </c:forEach>
          </tbody>
        </table>
        <br/>
        <html:submit property="delete" disabled="true" styleId="delete_button"
                     onclick="return confirmAction()">
          <fmt:message key="history.delete"/>
        </html:submit>
        <html:submit property="export" disabled="true" styleId="export_button">
          <fmt:message key="history.exportSelected"/>
        </html:submit>
        </html:form>
        <br/>

      </c:otherwise>
    </c:choose>
        <html:link action="/importQueries">
          <fmt:message key="begin.importQuery" />
        </html:link>


<script type="text/javascript">
(function() {
	jQuery(window).load(function(){
		<%-- sort queries by a remembered column --%>
		var order = im.getCookie("mymine.queries.order");
		if (order && parseInt(order)) {
			fdTableSort.jsWrapper(jQuery("form#modifyQueryForm table").attr("id"), order);
		}
		
		<%-- callback saving sort order of tables into a cookie --%>
		window.sortCompleteCallback = function() {
			var table = jQuery("form#modifyQueryForm table");
			var th = table.find("th.forwardSort");
			if (!jQuery(th).exists()) {
				th = table.find("th.reverseSort");
			}
			im.setCookie("mymine.queries.order", th.attr("class").replace(/[^0-9.]/g, ""));
		};
	});
})();
</script>


  </im:body>


<!-- /historyQueryView.jsp -->
