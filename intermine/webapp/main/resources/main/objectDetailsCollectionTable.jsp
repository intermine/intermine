<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- objectDetailsCollectionTable -->

<tiles:importAttribute name="collection"/>

<table border="0" cellspacing="0" cellpadding="0" width="100%">
  <tr>
    <td width="15">
      <img border="0" src="images/blank.gif" alt="" width="15" height="11"/>
    </td>
    <td>
    <table border="0" cellspacing="0" class="refSummary" align="right">
      <thead style="text-align: center">
        <tr>
          <td width="10px">
            <fmt:message key="objectDetails.class"/>
          </td>
          <c:forEach items="${collection.table.columnNames}" var="fd"
                     varStatus="status">
            <td>
              <span class="attributeField" style="white-space:nowrap">
                ${fd} <im:typehelp type="${collection.table.columnFullNames[status.index]}"/>
              </span>
            </td>
          </c:forEach>
          <td width="10px">
            &nbsp;<%--for IE--%>
          </td>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${collection.table.rowObjects}" 
                   var="thisRowObject" varStatus="status">
          <%-- request scope for im:eval --%>
          <c:set var="thisRowObject" value="${thisRowObject}" 
                 scope="request"/>
          <tr>
            <td width="1%" nowrap>
              <c:forEach items="${collection.table.types[status.index]}" var="cld">
                <span class="type">${cld.unqualifiedName}</span>
              </c:forEach>
              <c:forEach items="${LEAF_DESCRIPTORS_MAP[thisRowObject]}" var="cld2">
                <c:if test="${WEBCONFIG.types[cld2.name].tableDisplayer != null}">
                  <c:set var="cld2" value="${cld2}" scope="request"/>
                  <c:set var="backup" value="${object}"/>
                  <c:set var="object" value="${thisRowObject}" scope="request"/>
                  <tiles:insert page="${WEBCONFIG.types[cld2.name].tableDisplayer.src}"/>
                  <c:set var="object" value="${backup}"/>
                </c:if>
              </c:forEach>
            </td>
            <c:forEach items="${collection.table.expressions}" var="expr">
              <td>
                 <c:choose>
                  <c:when test="${!empty expr}">
                    <im:eval evalExpression="thisRowObject.${expr}" evalVariable="outVal"/>
                    <span class="value">${outVal}</span>

                    <c:if test="${empty outVal}">
                      &nbsp;<%--for IE--%>
                    </c:if>
                  </c:when>
                  <c:otherwise>
                    &nbsp;<%--for IE--%>
                  </c:otherwise>
                </c:choose>
              </td>
            </c:forEach>
            <td width="10px" nowrap>
              [<html:link action="/objectDetails?id=${collection.table.ids[status.index]}&amp;trail=${param.trail}_${collection.table.ids[status.index]}">
                <fmt:message key="results.details"/>
              </html:link>]
            </td>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </td></tr>
</table>

<!-- /objectDetailsCollectionTable -->
