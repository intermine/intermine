<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>


<!-- reportCollectionTable -->

<% if (pageContext.getAttribute("org.apache.struts.taglib.tiles.CompContext", PageContext.REQUEST_SCOPE) != null) { %>
  <tiles:importAttribute name="inlineResultsTable"/>
  <tiles:importAttribute name="fieldName"/>
<% } %>

<html:xhtml/>

    <c:choose>
    <c:when test="${true}">
      <table>
        <thead>
          <tr>
            <c:if test="${inlineResultsTable.hasMoreThanOneType}">
                <th>Class</th>
            </c:if>

            <c:forEach items="${inlineResultsTable.tableFieldConfigs}" var="fc" varStatus="status">
              <th>
              <c:choose>
                <c:when test="${!empty fc.label}">
                    <c:set var="columnDisplayName" value="${fc.displayName}"/>
                </c:when>
                <c:when test="${fc.isDottedPath && !empty WEBCONFIG && !empty INTERMINE_API}">
                    <c:set var="pathString" value="${fc.classConfig.unqualifiedClassName}.${fc.fieldExpr}"/>
                    <c:set var="columnDisplayName" value="${imf:formatPathStr(pathString, INTERMINE_API, WEBCONFIG)}"/>
                </c:when>
                <c:otherwise>
                    <c:set var="columnDisplayName" value="${fc.displayName}"/>
                </c:otherwise>
              </c:choose>
              <im:columnName columnName="${columnDisplayName}" noHead="true"/>
              </th>

            </c:forEach>
          </tr>
        </thead>
        <tbody>

          <c:forEach items="${inlineResultsTable.resultElementRows}" var="resultElementRow" varStatus="status">
            <tr>
              <c:choose>
                <c:when test="${!empty (resultElementRow.items)}">
                  <c:forEach items="${resultElementRow.items}" var="resultElementColumn" varStatus="rowStatus">
                    <c:if test="${rowStatus.count == 1 && inlineResultsTable.hasMoreThanOneType}">
                      <td class="type">
                          ${resultElementRow.className}
                      </td>
                    </c:if>
                    <c:choose>
                      <c:when test="${!empty resultElementColumn}">
                        <c:choose>
                          <c:when test="${!resultElementColumn.hasDisplayer}">
                            <c:choose>
                              <c:when test="${resultElementColumn.isKeyField}">
                                <td>
                                <a href="report.do?id=${resultElementColumn.id}">${resultElementColumn.field}</a>
                                </td>
                              </c:when>
                              <c:otherwise>
                                <td>
                                  <c:choose>
                                    <c:when test="${resultElementColumn.field != null}">
                                      ${resultElementColumn.field}
                                    </c:when>
                                    <c:otherwise>
                                      &nbsp;
                                    </c:otherwise>
                                  </c:choose>
                                </td>
                              </c:otherwise>
                            </c:choose>
                          </c:when>
                          <c:otherwise>
                            <td>
                              <c:set var="interMineObject" value="${resultElementColumn.object}" scope="request"/>
                              <tiles:insert page="${resultElementColumn.fieldConfig.displayer}">
                                <tiles:put name="expr" value="${resultElementColumn.fieldConfig.fieldExpr}" />
                              </tiles:insert>
                            </td>
                          </c:otherwise>
                        </c:choose>
                      </c:when>
                      <c:otherwise>
                        <td>&nbsp;</td>
                      </c:otherwise>
                    </c:choose>
                  </c:forEach>
                </c:when>
                <c:otherwise>
                  <td><b style="color:red;">No resultElementColumns! Check webconfig-model.xml</b></td>
                </c:otherwise>
              </c:choose>
            </tr>
          </c:forEach>

        </tbody>
      </table>
    </c:when>
    <c:otherwise><im:debug message="InlineResultsTable.java is failing you"/></c:otherwise>
</c:choose>

<!-- /reportCollectionTable -->
