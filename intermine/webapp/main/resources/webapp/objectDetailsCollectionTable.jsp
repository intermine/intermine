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
          <c:if test="${collection.table.hasMoreThanOneType}"><td class="theme-5-background theme-3-border">Class</td></c:if>
          <c:forEach items="${collection.table.tableFieldConfigs}" var="fc">
            <td class="theme-5-background theme-3-border">${fc.fieldExpr}</td>
          </c:forEach>
        </tr>
      </thead>
      <tbody>

        <c:forEach items="${collection.table.resultElementRows}" var="resultElementRow" varStatus="status">
          <tr<c:if test="${status.count % 2 == 0}"> class="even"</c:if>>
            <c:forEach items="${resultElementRow.items}" var="resultElementColumn" varStatus="rowStatus">
              <c:if test="${rowStatus.count == 1 && collection.table.hasMoreThanOneType}">
                <td class="type <c:if test="${status.count % 2 == 0}">theme-3-border theme-6-background</c:if>">
                    ${resultElementRow.className}
                </td>
              </c:if>
              <c:choose>
                <c:when test="${!empty resultElementColumn}">
                  <c:choose>
                    <c:when test="${!resultElementColumn.hasDisplayer}">
                      <c:choose>
                        <c:when test="${resultElementColumn.isKeyField}">
                          <td<c:if test="${status.count % 2 == 0}"> class="theme-3-border theme-6-background"</c:if>>
                          <a class="theme-1-color" href="objectDetails.do?id=${resultElementColumn.id}">${resultElementColumn.field}</a>
                          <!--
                          <html:link action="/objectDetails?id=${resultElementColumn.id}">
                            <fmt:message key="${resultElementColumn.field}"/>
                          </html:link>
                          -->
                          </td>
                        </c:when>
                        <c:otherwise>
                          <td<c:if test="${status.count % 2 == 0}"> class="theme-3-border theme-6-background"</c:if>>
                            ${resultElementColumn.field}
                          </td>
                        </c:otherwise>
                      </c:choose>
                    </c:when>
                    <c:otherwise>
                      <td<c:if test="${status.count % 2 == 0}"> class="theme-3-border theme-6-background"</c:if>>
                        <c:set var="interMineObject" value="${resultElementColumn.object}" scope="request"/>
                        <tiles:insert page="${resultElementColumn.fieldConfig.displayer}">
                          <tiles:put name="expr" value="${resultElementColumn.fieldConfig.fieldExpr}" />
                        </tiles:insert>
                      </td>
                    </c:otherwise>
                  </c:choose>
                </c:when>
                <c:otherwise>
                  <td<c:if test="${status.count % 2 == 0}"> class="theme-3-border theme-6-background"</c:if>>&nbsp;</td>
                </c:otherwise>
              </c:choose>
            </c:forEach>
          </tr>
        </c:forEach>

      </tbody>
    </table>

<!-- /objectDetailsCollectionTable -->