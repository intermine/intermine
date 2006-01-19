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
<tiles:importAttribute name="table" ignore="true"/>

<c:set var="templateName" value="${templateQuery.name}"/>

<c:set var="aspectAndField" value="${aspect}_${templateName}"/>
<c:if test="${!empty displayObject}">
  <c:set var="verbose" value="${!empty displayObject.verbosity[aspectAndField]}"/>
  <c:set var="interMineObject" value="${displayObject.object}"/>
</c:if>

<div class="templateLine">
  <c:choose>
    <c:when test="${empty displayObject}">

    </c:when>
    <c:when test="${!empty templateCounts[templateName] &&
                  templateCounts[templateName] == 0}">
      <img border="0" src="images/blank.gif" alt=" " width="11" height="11"/>
    </c:when>
    <c:when test="${empty table}">
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

  <im:templateLine type="${type}" templateQuery="${templateQuery}"
                   className="${className}" interMineObject="${interMineObject}"/>
  <c:if test="${IS_SUPERUSER}">
    <tiles:insert name="inlineTagEditor.tile">
      <tiles:put name="taggable" beanName="templateQuery"/>
      <tiles:put name="vertical" value="true"/>
      <tiles:put name="show" value="true"/>
    </tiles:insert>
  </c:if>

  <c:if test="${verbose}">
    <div style="overflow-x: auto">
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
                    <c:forEach items="${table.columnNames}" var="columnName" varStatus="status">
                      <td>
                        <span class="attributeField" style="white-space:nowrap">
                          <c:out value="${fn:replace(columnName, '.', '&nbsp;> ')}" 
                                 escapeXml="false"/>
                        </span>
                      </td>
                      <c:if test="${table.resultsSize > 0}">
                        <c:set var="object" value="${table.inlineResults[0][status.index]}"/>
                        <c:if test="${!empty LEAF_DESCRIPTORS_MAP[object]}">
                          <c:set var="displayObject" value="${DISPLAY_OBJECT_CACHE[object]}"/>
                          <c:forEach items="${displayObject.fieldExprs}" var="expr">
                            <c:if test="${displayObject.fieldConfigMap[expr].showInResults}">
                              <td>
                                <span class="attributeField">${expr}</span>
                              </td>
                            </c:if>
                          </c:forEach>
                        </c:if>
                      </c:if>
                    </c:forEach>
                  </tr>
                </thead>
                <tbody>
                  <c:forEach items="${table.inlineResults}" var="row" varStatus="status">
                    <tr>
                      <c:forEach items="${row}" var="object">
                        <c:choose>
                          <c:when test="${empty object}">
                            <td><fmt:message key="objectDetails.nullField"/></td>
                          </c:when>
                          <c:otherwise>
                            <c:set var="leafClds" value="${LEAF_DESCRIPTORS_MAP[object]}"/>
                            <td>
                              <c:choose>
                                <c:when test="${empty leafClds}">
                                  ${object}
                                </c:when>
                                <c:otherwise>
                                  <c:set var="displayObject" value="${DISPLAY_OBJECT_CACHE[object]}"/>
                                  <%-- Link to object --%>
                                  <c:set var="linkAction" value="/objectDetails?id=${object.id}&amp;trail=${prepend}${param.trail}_${object.id}" scope="request"/>
                                  <span style="white-space:nowrap">
                                    <c:forEach var="cld" items="${leafClds}">
                                      <span class="type"><c:out value="${cld.unqualifiedName}"/></span>
                                    </c:forEach>
                                    [<html:link action="${linkAction}">
                                      <fmt:message key="results.details"/>
                                    </html:link>]
                                  </td>
                              
                                  <%-- Cell for each field expr --%>
                                  <c:forEach items="${displayObject.fieldExprs}" var="expr">
                                    <c:set var="object2" value="${object}" scope="request"/>
                                    <im:eval evalExpression="object2.${expr}" evalVariable="outVal"/>
                                    <c:if test="${displayObject.fieldConfigMap[expr].showInResults}">
                                      <td>
                                      <c:set var="style" value="white-space:nowrap"/>
                                      <c:if test="${outVal.class.name == 'java.lang.String' && fn:length(outVal) > 25}">
                                        <c:if test="${fn:length(outVal) > 65}">
                                          <c:set var="outVal" value="${fn:substring(outVal, 0, 60)}..." scope="request"/>
                                        </c:if>
                                        <c:set var="style" value=""/>
                                      </c:if>
                                      <div style="${style}">
                                        <im:value>${outVal}</im:value>
                                      </div>
                                      </td>
                                    </c:if>
                                  </c:forEach>
                                </c:otherwise>
                              </c:choose>
                              
                            </span>
                            <%-- /Link to object --%>
                          </c:otherwise>
                        </c:choose>
                      </c:forEach>
                    </tr>
                  </c:forEach>
                </tbody>
              </table>
            </td>
          </tr>
        </table>
      </c:if>
    </div>
  </c:if>
</div>
<!-- /objectDetailsTemplate.jsp -->
