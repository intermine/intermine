<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<tiles:importAttribute name="resultElementMap" ignore="false"/>
<tiles:importAttribute name="columnNames" ignore="false"/>
<tiles:importAttribute name="issueType" ignore="false"/>
<tiles:importAttribute name="initialTypeMap" ignore="false"/>
<tiles:importAttribute name="arrayName" ignore="false"/>
<tiles:importAttribute name="messageKey" ignore="false"/>
<tiles:importAttribute name="messageParam" ignore="true"/>

<!-- bagUploadConfirmIssue.jsp -->
<table class="collection" cellspacing="0" width="95%">
  <caption>
    <span class="sectionAddRemove">
      <span id="${issueType}addAllLink" onclick="addAll('${issueType}', '${arrayName}');" class="fakelink">Add all</span> |
        <span id="${issueType}removeAllLink" onclick="removeAll('${issueType}', '${arrayName}');">Remove all</span>
    </span>
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
  </caption>
  <tr>
    <td>
      Identifier
    </td>
    <td width="10">
      <fmt:message key="objectDetails.class"/>
    </td>
    <c:forEach items="${columnNames}" var="name"
               varStatus="status">
      <td>
        <span class="attributeField" style="white-space:nowrap">
          ${name} <im:typehelp type="${columnNames[status.index]}"/>
        </span>
      </td>
    </c:forEach>
    <td width="10">
      &nbsp;<%--for IE--%>
    </td>
  </tr>
  <c:set var="idcounter" value="0"/>
  <c:forEach var="resultElementEntry" items="${resultElementMap}" >
    <c:set var="identifier" value="${resultElementEntry.key}"/>
    <c:set var="resultElementRowList" value="${resultElementEntry.value}"/>

    <c:forEach var="resultElementRow" items="${resultElementRowList}" varStatus="status">
      <c:set var="rowClass">
        <c:choose>
          <c:when test="${status.count % 2 == 1}">odd</c:when>
          <c:otherwise>even</c:otherwise>
        </c:choose>
      </c:set>

      <tr class="${rowClass}" id="tr_${identifier}"/>
        <c:if test="${status.index == 0}">
          <td border="1" rowSpan="${fn:length(resultElementRowList)}"
              valign="top" id="td_${issueType}_${identifier}">${identifier}</td>
          <c:if test="${issueType == 'converted' }">
             <td border="1" rowSpan="${fn:length(resultElementRowList)}"
                valign="top" id="row_${issueType}_${idcounter}">${initialTypeMap[identifier]}</td>
          </c:if>
        </c:if>
        <c:forEach var="resultElement" items="${resultElementRow}" varStatus="rowStatus">
          <td id="row_${issueType}_${idcounter}">
            <c:choose>
              <c:when test="${rowStatus.index == 0 && issueType != 'converted'}">
                <%-- special case: the first element is the class name --%>
                ${resultElement}
              </c:when>
              <c:when test="${rowStatus.index == fn:length(resultElementRow) - 1}">
                <%-- special case: the last element is the object id --%>
                <span id="add_${issueType}_${resultElementRow[rowStatus.index]}" onclick="addId2Bag('${resultElementRow[rowStatus.index]}','${idcounter}','${identifier}','${issueType}');" class="fakelink">Add</span>
                &nbsp;&nbsp;
                <span id="rem_${issueType}_${resultElementRow[rowStatus.index]}" onclick="removeIdFromBag('${resultElementRow[rowStatus.index]}','${idcounter}','${identifier}','${issueType}');">Remove</span>
              </c:when>
              <c:otherwise>
        <c:choose>
                <c:when test="${resultElement.keyField && !empty resultElement.field}">
          		  <html:link action="/objectDetails?id=${resultElement.id}" target="_new" styleClass="extlink">
                    ${resultElement.field}
                  </html:link>
                </c:when>
                <c:otherwise>
                  ${resultElement.field}
              </c:otherwise>
              </c:choose>
              </c:otherwise>
            </c:choose>
            &nbsp;
          </td>
        </c:forEach>
      </tr>
      <c:set var="idcounter" value="${idcounter + 1}"/>
    </c:forEach>
  </c:forEach>
</table>
<!-- /bagUploadConfirmIssue.jsp -->

