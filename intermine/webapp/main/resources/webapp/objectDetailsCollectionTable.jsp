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

<table border="0" cellspacing="0" cellpadding="0" width="100%">
  <tr>
    <td width="15">
      <img border="0" src="images/blank.gif" width="15" height="11"/>
    </td>
    <td>
    <table border="0" cellspacing="0" class="refSummary" align="right">
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
          <c:if test="${collection.table.ids[0] != null}">
            <td width="10">
              &nbsp;<%--for IE--%>
            </td>
          </c:if>
        </tr>
      </thead>
      <tbody>
        <c:forEach items="${collection.table.rowObjects}"
                   var="thisRowObject" varStatus="status">

          <c:set var="thisRowObject" value="${thisRowObject}" />
          <c:set var="rowValues" value="${collection.table.rowFieldValues[thisRowObject]}"/>

          <tr>
            <td width="1%" nowrap>

              <%-- class name of object being displayed on this row --%>
              <c:forEach items="${collection.table.types[status.index]}" var="cld">
                <span class="type">${cld.unqualifiedName}</span>
              </c:forEach>

              <%-- table displayer --%>
              <c:forEach items="${LEAF_DESCRIPTORS_MAP[thisRowObject]}" var="cld2">
                <c:if test="${WEBCONFIG.types[cld2.name].tableDisplayer != null}">
                  <tiles:insert page="${WEBCONFIG.types[cld2.name].tableDisplayer.src}">
                    <tiles:put name="cld" value="${cld2}" />
                    <tiles:put name="object" value="${thisRowObject}" />
                 </tiles:insert>
                </c:if>
              </c:forEach>
            </td>

            <%-- print each field configured for this object --%>
            <c:forEach items="${collection.table.fieldConfigs}" var="fieldConfig">
              <td>
                 <c:choose>

                 <%-- print each field configured for this object --%>
                  <c:when test="${!empty fieldConfig && !empty fieldConfig.displayer}">
                    <c:set var="interMineObject" value="${thisRowObject}" scope="request"/>
                    <span class="value">
                      <tiles:insert page="${fieldConfig.displayer}">
                        <tiles:put name="expr" value="${fieldConfig.fieldExpr}" />
                      </tiles:insert>
                    </span>
                  </c:when>
                  <c:when test="${!empty fieldConfig && !empty fieldConfig.fieldExpr}">
                    <c:set var="outVal" value="${rowValues[fieldConfig.fieldExpr]}"/>
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

            <%-- link to report page for this object --%>
            <c:if test="${collection.table.ids[status.index] != null}">
              <td width="10px" nowrap>
                [<html:link action="/objectDetails?id=${collection.table.ids[status.index]}&amp;trail=${param.trail}|${collection.table.ids[status.index]}">
                  <fmt:message key="results.details"/>
                </html:link>]
              </td>
            </c:if>
          </tr>
        </c:forEach>
      </tbody>
    </table>
  </td></tr>
</table>
<%-- if field isn't in webconfig, we don't know how to build the summary query --%>
<c:choose>
    <c:when test="${!empty collection.table.fieldConfigs}">
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
    </c:when>
    <c:otherwise>
        <!-- class not configured in webconfig-model.xml -->
        [<fmt:message key="results.showintable"/>]
    </c:otherwise>
</c:choose>

<!-- /objectDetailsCollectionTable -->
