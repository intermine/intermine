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

    function doBagAction(name, url){
        location.href = url + document.getElementById('name_'+name).innerHTML.replace(/[ \t\n]/g, '');
        return false;
    }
    //]]>-->
  </script>

  <p>
    <fmt:message key="${messageKey}"/>
        <c:if test="${empty PROFILE.username && queryMap == '${PROFILE.history}'}">
        	<fmt:message key="history.history.login"/>
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
        <table class="sortable-onload-3-reverse rowstyle-alt no-arrow" cellspacing="0">
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
                   
                      <html:link action="/templateProblems?name=${savedQuery.key}&amp;type=saved" styleClass="brokenTmplLink">
                      <strike>${savedQuery.value.name}</strike>
                      </html:link>
                    
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
                    <c:when test="${savedQuery.value.pathQuery.info != null}">
                      <c:out value="${savedQuery.value.pathQuery.info.rows}"/>
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
                          <span class="historySummaryRoot">${fn:substringBefore(item, '.')}</span>
                        </c:when>
                        <c:otherwise>
                          <span class="historySummaryRoot">${item}</span>
                        </c:otherwise>
                      </c:choose>
                    </c:if>
                  </c:forEach>
                </td>
                <td class="sorting" align="left">
                  <c:forEach items="${savedQuery.value.pathQuery.view}" var="item">
                    <im:unqualify className="${item}" var="text"/>
                    <span class="historySummaryShowing">${text}</span>
                  </c:forEach>
                </td>
                <td class="sorting" align="center" nowrap>
                  <c:choose>
                    <c:when test="${validQuery}">
                      <a href="<html:rewrite action='/modifyQueryChange'/>?method=run&amp;type=${type}&trail=|query&amp;name=${savedQuery.key}"
                         onclick="return doBagAction('${savedQuery.key}', '<html:rewrite action='/modifyQueryChange'/>?method=run&amp;type=${type}&trail=|query&amp;name=')"
                         titleKey="history.action.execute.hover">
                        <fmt:message key="history.action.execute"/>
                      </a>
                    </c:when>
                    <c:otherwise>
                      <fmt:message key="history.action.execute"/>
                    </c:otherwise>
                  </c:choose>
                  |
                  <a href="<html:rewrite action='/modifyQueryChange'/>?method=load&type=${type}&amp;name=${savedQuery.key}"
                     onclick="return doBagAction('${savedQuery.key}', '<html:rewrite action='/modifyQueryChange'/>?method=load&type=${type}&amp;name=')"
                     titleKey="history.action.edit.hover">
                    <fmt:message key="history.action.edit"/>
                  </a>
                  |
                  <c:if test="${type == 'history'}">
                    <c:if test="${!empty PROFILE.username}">
                      <c:set var="urlPrefix" value=""/>
                      <a href="<html:rewrite action='/modifyQueryChange'/>?method=save&amp;name=${savedQuery.key}"
                         onclick="return doBagAction('${savedQuery.key}', '<html:rewrite action='/modifyQueryChange'/>?method=save&amp;name=')"
                         titleKey="history.action.save.hover">
                        <fmt:message key="history.action.save"/>
                      </a>
                      |
                    </c:if>
                  </c:if>

                  <a href="<html:rewrite action='/exportQuery'/>?type=${type}&amp;name=${savedQuery.key}"
                     onclick="return doBagAction('${savedQuery.key}', '<html:rewrite action='/exportQuery'/>?type=${type}&amp;name=')"
                     titleKey="history.action.export.hover">
                    <fmt:message key="history.action.export"/>
                  </a>
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
  </im:body>

<!-- /historyQueryView.jsp -->
