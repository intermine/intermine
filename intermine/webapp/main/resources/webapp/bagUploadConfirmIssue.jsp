<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="issueType" ignore="false"/>
<tiles:importAttribute name="initialTypeMap" ignore="false"/>
<tiles:importAttribute name="arrayName" ignore="false"/>
<tiles:importAttribute name="messageKey" ignore="false"/>
<tiles:importAttribute name="messageParam" ignore="true"/>
<tiles:importAttribute name="table" ignore="false"/>

<div class="description">
  <div class="sectionAddRemove">
    <span id="${issueType}addAllLink" onclick="addAll('${issueType}', '${arrayName}');" class="fakelink">Add all</span> |
      <span id="${issueType}removeAllLink" onclick="removeAll('${issueType}', '${arrayName}');">Remove all</span>
  </div>

  <span>
    <c:choose>
      <c:when test="${messageParam != null}">
        <fmt:message key="${messageKey}">
          <fmt:param value="${messageParam}"/>
        </fmt:message>
      </c:when>
      <c:otherwise>
        <fmt:message key="${messageKey}"/>
      </c:otherwise>
    </c:choose>
  </span>
</div>

<table border="0" cellspacing="0" class="inlineResultsTable" align="left">
  <thead>
    <tr>
      <td>Identifier You Entered</td>
      <c:if test="${table.hasMoreThanOneType}"><td>Class</td></c:if>
      <c:forEach items="${table.tableFieldConfigs}" var="fc">
        <td>${fc.displayName}</td>
      </c:forEach>
      <td>&nbsp;</td>
    </tr>
  </thead>
  <tbody>

    <c:set var="oldRowIdentifier" value="0" scope="request" />
    <c:set var="rowClass" value="even" scope="request" />
    <c:forEach items="${table.resultElementRows}" var="resultElementRow" varStatus="status">
      <c:set var="totalRowCount" value="${totalRowCount + 1}" scope="request" />
      <c:set var="newIdentifier" value="${resultElementRow.identifier}" scope="request" />
      <c:if test="${oldRowIdentifier != newIdentifier}">
        <c:choose>
          <c:when test="${rowClass == 'odd'}">
            <c:set var="rowClass" value="even" scope="request" />
          </c:when>
          <c:otherwise>
            <c:set var="rowClass" value="odd" scope="request" />
          </c:otherwise>
        </c:choose>
      </c:if>
      <tr class="${rowClass}" id="tr_${resultElementRow.identifier}">
      <c:set var="oldRowIdentifier" value="${newIdentifier}" scope="request" />

        <c:if test="${resultElementRow.showIdentifier}">
            <td
              class="identifier"
              id="td_${issueType}_${resultElementRow.identifier}"
              rowspan="${resultElementRow.rowSpan}">
                ${resultElementRow.identifier}
            </td>
        </c:if>
        <c:forEach
            items="${resultElementRow.items}"
            var="resultElementColumn"
            varStatus="rowStatus">
          <c:if test="${rowStatus.count == 1 && table.hasMoreThanOneType}"><td class="type">${resultElementRow.className}</td></c:if>
          <td class="row_${issueType}_${status.count -1}">
          <c:choose>
            <c:when test="${!empty resultElementColumn}">
              <c:choose>
                <c:when test="${!resultElementColumn.hasDisplayer}">
                  <c:choose>
                    <c:when test="${resultElementColumn.isKeyField}">
                      <a href="report.do?id=${resultElementColumn.id}">${resultElementColumn.field}</a>
                      <!--
                      <html:link action="/report?id=${resultElementColumn.id}">
                        <fmt:message key="${resultElementColumn.field}"/>
                      </html:link>
                      -->
                    </c:when>
                    <c:otherwise>
                      ${resultElementColumn.field}
                    </c:otherwise>
                  </c:choose>
                </c:when>
                <c:otherwise>
                    <c:set var="interMineObject" value="${resultElementColumn.object}" scope="request"/>
                    <tiles:insert page="${resultElementColumn.fieldConfig.displayer}">
                      <tiles:put name="expr" value="${resultElementColumn.fieldConfig.fieldExpr}" />
                    </tiles:insert>
                </c:otherwise>
              </c:choose>
            </c:when>
            <c:otherwise>
              &nbsp;
            </c:otherwise>
          </c:choose>
          </td>
          <c:if test="${rowStatus.count == fn:length(resultElementRow.items)}">
            <td class="right row_${issueType}_${status.count -1}">
              <span id="add_${issueType}_${resultElementRow.objectId}" class="fakelink"
                  onclick="addId2Bag('${resultElementRow.objectId}','${status.count -1}',
                                    '${resultElementRow.identifier}','${issueType}');">Add</span>
              &nbsp;&nbsp;
              <span id="rem_${issueType}_${resultElementRow.objectId}"
                  onclick="removeIdFromBag('${resultElementRow.objectId}','${status.count -1}',
                                        '${resultElementRow.identifier}','${issueType}');">Remove</span>
            </td>
          </c:if>
        </c:forEach>
      </tr>
    </c:forEach>
  </tbody>
</table>
<div style="clear:both;">&nbsp;</div>
