<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- objectDetailsCollectionTable -->

<% if (pageContext.getAttribute("org.apache.struts.taglib.tiles.CompContext", PageContext.REQUEST_SCOPE) != null) { %>
  <tiles:importAttribute name="collection"/>
  <tiles:importAttribute name="fieldName"/>
<% } %>

<html:xhtml/>

    <table border="0" cellspacing="0" class="refSummary" align="left">
      <thead style="text-align: center">
        <tr>
          <td width="10">
            <fmt:message key="objectDetails.class"/>
          </td>

          <%-- column names --%>
          <c:forEach items="${collection.table.columnNames}" var="fd"
                     varStatus="status">
            <td>
              <span class="attributeField" style="white-space:nowrap">
                ${fd} <im:typehelp type="${collection.table.columnFullNames[status.index]}"/>
              </span>
            </td>
          </c:forEach>
        </tr>
      </thead>
      <tbody>

      <%-- ########################################################## --%>
        <tr><td style="background:red;"><code>Number of columns: ${collection.table.columnWidth}</code></td></tr>

        <!-- traverse the columns names for each row -->
        <c:forEach items="${collection.table.columnFullNames}" var="resultElementRow">
          <tr>
            <td style="background:orange;">[Class]</td>
            <c:forEach items="${resultElementRow}" var="resultElementColumn">
              <td style="background:orange;">${resultElementColumn}</td>
            </c:forEach>
          </tr>
        </c:forEach>

        <!-- collection of row objects -->
        <c:forEach items="${collection.table.listOfRowObjects}" var="rowObject">
          <tr><td style="background:green;">${rowObject}</td></tr>
        </c:forEach>

        <!-- working old code -->
        <c:forEach items="${collection.table.rowObjects}" var="thisRowObject">

          <c:set var="rowValues" value="${collection.table.rowFieldValues[thisRowObject]}"/>

          <tr>
            <td style="background:yellow;">[Class]</td>
            <c:forEach items="${collection.table.fieldConfigs}" var="fieldConfig">
              <td style="background:yellow;">
                <c:if test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
                  <span class="value">${rowValues[fieldConfig.fieldExpr]}</span>
                </c:if>
              </td>
            </c:forEach>
          </tr>
        </c:forEach>

        <%--
        <c:forEach items="${collection.table.resultElementRows}" var="resultElementRow">
          <tr>
            <td>[Class]</td>
            <c:forEach items="${resultElementRow}" var="resultElementColumn">
              <td>${resultElementColumn}</td>
            </c:forEach>
          </tr>
        </c:forEach>
        %-->

        <%-- ########################################################## --%>

      </tbody>
    </table>
<%-- if field isn't in webconfig, we don't know how to build the summary query --%>
<c:choose>
    <c:when test="${!empty collection.table.fieldConfigs}">
    <%--
        <div class="refSummary">
          [<html:link action="/collectionDetails?id=${object.id}&amp;field=${fieldName}&amp;trail=${param.trail}">
            <c:choose>
               <c:when test="${collection.size > WEB_PROPERTIES['inline.table.size']}">
                <fmt:message key="results.showallintable"/>
              </c:when>
              <c:otherwise>
                <fmt:message key="results.showintable"/>
              </c:otherwise>
            </c:choose>
          </html:link>]
        </div>
        --%>
    </c:when>
    <c:otherwise>
        <!-- class not configured in webconfig-model.xml -->
        [<fmt:message key="results.showintable"/>]
    </c:otherwise>
</c:choose>

<!-- /objectDetailsCollectionTable -->