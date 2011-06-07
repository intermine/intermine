<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


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
            <c:if test="${inlineResultsTable.hasMoreThanOneType}"><th>Class</th></c:if>
            <c:forEach items="${inlineResultsTable.tableFieldConfigs}" var="fc">
              <th>${fc.fieldExpr}</th>
            </c:forEach>
          </tr>
        </thead>
        <tbody>

          <c:forEach items="${inlineResultsTable.resultElementRows}" var="resultElementRow" varStatus="status">
            <tr<c:if test="${status.count % 2 == 0}"> class="even"</c:if>>
              <c:choose>
                <c:when test="${!empty(resultElementRow.items)}">
                  <c:forEach items="${resultElementRow.items}" var="resultElementColumn" varStatus="rowStatus">
                    <c:if test="${rowStatus.count == 1 && inlineResultsTable.hasMoreThanOneType}">
                      <td class="type <c:if test="${status.count % 2 == 0}">alt</c:if>">
                          ${resultElementRow.className}
                      </td>
                    </c:if>
                    <c:choose>
                      <c:when test="${!empty resultElementColumn}">
                        <c:choose>
                          <c:when test="${!resultElementColumn.hasDisplayer}">
                            <c:choose>
                              <c:when test="${resultElementColumn.isKeyField}">
                                <td<c:if test="${status.count % 2 == 0}"> class="alt"</c:if>>
                                <a href="report.do?id=${resultElementColumn.id}">${resultElementColumn.field}</a>
                                </td>
                              </c:when>
                              <c:otherwise>
                                <td<c:if test="${status.count % 2 == 0}"> class="alt"</c:if>>
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
                            <td<c:if test="${status.count % 2 == 0}"> class="alt"</c:if>>
                              <c:set var="interMineObject" value="${resultElementColumn.object}" scope="request"/>
                              <tiles:insert page="${resultElementColumn.fieldConfig.displayer}">
                                <tiles:put name="expr" value="${resultElementColumn.fieldConfig.fieldExpr}" />
                              </tiles:insert>
                            </td>
                          </c:otherwise>
                        </c:choose>
                      </c:when>
                      <c:otherwise>
                        <td<c:if test="${status.count % 2 == 0}"> class="alt"</c:if>>&nbsp;</td>
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
    <c:otherwise>InlineResultsTable.java is failing you</c:otherwise>
    </c:choose>

<!-- /reportCollectionTable -->