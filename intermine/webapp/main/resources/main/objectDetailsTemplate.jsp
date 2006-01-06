<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetailsTemplate.jsp -->

<html:xhtml/>

<tiles:importAttribute name="displayObject" ignore="true"/>
<tiles:importAttribute name="templateQuery"/>
<tiles:importAttribute name="aspect"/>
<tiles:importAttribute name="type"/>

<%-- from controller: --%>
<tiles:importAttribute name="unconstrainedCount" ignore="true"/>
<tiles:importAttribute name="table" ignore="true"/>
<tiles:importAttribute name="viewNodesAreAttributes" ignore="true"/>

<c:set var="templateName" value="${templateQuery.name}"/>

<c:set var="aspectAndField" value="${aspect}_${templateName}"/>
<c:if test="${!empty displayObject}">
  <c:set var="verbose" value="${!empty displayObject.verbosity[aspectAndField]}"/>
  <c:set var="interMineObject" value="${displayObject.object}"/>
</c:if>

<div>
  <c:choose>
    <c:when test="${empty displayObject}">

    </c:when>
    <c:when test="${!empty templateCounts[templateName] &&
                  templateCounts[templateName] == 0}">
      <img border="0" src="images/blank.gif" alt=" " width="11" height="11"/>
      <c:set var="cssClass" value="nullStrike"/>
    </c:when>
    <c:when test="${!viewNodesAreAttributes || unconstrainedCount > 0}">
      <img border="0" src="images/blank.gif" alt=" " width="11" height="11"/>
    </c:when>
    <c:when test="${verbose}">
      <html:link action="/modifyDetails?method=unverbosify&amp;field=${templateName}&amp;aspect=${aspect}&amp;id=${object.id}&amp;trail=${param.trail}">
        <img border="0" src="images/minus.gif" alt="-" width="11" height="11"/>
      </html:link>
    </c:when>
    <c:otherwise>
      <html:link action="/modifyDetails?method=verbosify&amp;field=${templateName}&amp;aspect=${aspect}&amp;id=${object.id}&amp;trail=${param.trail}">
        <img border="0" src="images/plus.gif" alt="+" width="11" height="11"/>
      </html:link>
    </c:otherwise>
  </c:choose>
  <span class="${cssClass}">

    <im:templateLine type="${type}" templateQuery="${templateQuery}"
                     className="${className}" interMineObject="${interMineObject}"/>
  </span>
  <c:if test="${verbose}">
    <c:if test="${!empty displayObject.object && !empty table.inlineResults}">
      <table border="0" cellspacing="0" cellpadding="0" width="100%">
        <tr>
          <td width="15">
            <img border="0" src="images/blank.gif" alt="" width="15" height="11"/>
          </td>
          <td>
            <table border="0" cellspacing="0" class="refSummary" align="right">
              <thead style="text-align: center">
                <tr>
                  <c:forEach items="${table.columnNames}" var="columnName"
                             varStatus="status">
                    <td>
                      <span class="attributeField" style="white-space:nowrap">
                        ${columnName}
                      </span>
                    </td>
                  </c:forEach>
                </tr>
              </thead>
              <tbody>
                <c:forEach items="${table.inlineResults}" var="row" varStatus="status">
                  <tr>
                    <c:forEach items="${row}" var="col">
                      <td>
                        <c:choose>
                          <c:when test="${empty col}">
                            <fmt:message key="objectDetails.nullField"/>
                          </c:when>
                          <c:otherwise>
                            ${col}
                          </c:otherwise>
                        </c:choose>
                      </td>
                    </c:forEach>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
          </td>
        </tr>
      </table>
    </c:if>
  </c:if>
  <hr class="seperator"/>
</div>
<!-- /objectDetailsTemplate.jsp -->
