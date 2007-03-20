<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- objectDetailsTemplateTable.jsp -->

<% if (pageContext.getAttribute("org.apache.struts.taglib.tiles.CompContext", PageContext.REQUEST_SCOPE) != null) { %>
  <tiles:importAttribute/>
<% } %>

<html:xhtml/>

<div style="overflow: auto; padding: 3px">
  <c:if test="${(displayObject != null || interMineIdBag !=null) && table != null && !empty table.inlineResults}">
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
                  <td class="object">
                    <span class="attributeField" style="white-space:nowrap">
                      <c:out value="${fn:replace(columnName, '.', '&nbsp;> ')}" 
                             escapeXml="false"/>
                    </span>
                  </td>
                  <c:if test="${table.resultsSize > 0}">
                    <c:set var="object" value="${table.inlineResults[0][status.index]}"/>
                    <c:if test="${!empty LEAF_DESCRIPTORS_MAP[object]}">
                      <c:set var="displayObject2" value="${DISPLAY_OBJECT_CACHE[object]}"/>
                      <c:forEach items="${displayObject2.fieldExprs}" var="expr">
                        <c:if test="${displayObject2.fieldConfigMap[expr].showInResults}">
                          <td class="attrib">
                            <span class="attributeField">${fn:replace(expr, '.', '&nbsp;> ')}</span>
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
                  <c:forEach items="${row}" var="resultElement">
                    <c:choose>
                      <c:when test="${empty resultElement}">
                        <td><fmt:message key="objectDetails.nullField"/></td>
                      </c:when>
                      <c:otherwise>
                        <c:set var="leafClds" value="${LEAF_DESCRIPTORS_MAP[resultElement]}"/>
                        <td>
                          <c:choose>
                            <c:when test="${empty leafClds}">
                              <c:set var="resultElement" value="${resultElement}" scope="request"/>
                              <tiles:insert name="objectView.tile" />
                            </c:when>
                            <c:otherwise>
                              <c:set var="object" value="${resultElement.object}"/>
                              <c:set var="displayObject2" value="${DISPLAY_OBJECT_CACHE[object]}"/>
                              <%-- Link to object --%>
                              <c:set var="linkAction" value="/objectDetails?id=${object.id}&amp;trail=${param.trail}_${object.id}" scope="request"/>
                              <span style="white-space:nowrap">
                                <c:forEach var="cld" items="${leafClds}">
                                  <span class="type"><c:out value="${cld.unqualifiedName}"/></span>
                                </c:forEach>
                                [<html:link action="${linkAction}">
                                  <fmt:message key="results.details"/>
                                </html:link>]
                              </td>
                          
                              <%-- Cell for each field expr --%>
                              <c:forEach items="${displayObject2.fieldExprs}" var="expr">
                                <c:set var="object2" value="${object}" scope="request"/>
                                <im:eval evalExpression="object2.${expr}" evalVariable="outVal"/>
                                <c:if test="${displayObject2.fieldConfigMap[expr].showInResults}">
                                  <td class="attrib">
                                  <c:set var="style" value="white-space:nowrap"/>
                                  <c:if test="${outVal.class.name == 'java.lang.String'}">
                                    <c:if test="${fn:length(outVal) > 65}">
                                      <c:set var="outVal" value="${fn:substring(outVal, 0, 60)}..." scope="request"/>
                                    </c:if>
                                    <c:if test="${fn:length(outVal) == 0}">
                                      <c:set var="outVal" value="&nbsp;"/>
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

<%-- Produce show in table link --%>

<c:set var="extra" value=""/>
<c:if test="${!empty fieldExprMap}">
  <c:choose>
  <c:when test="${! empty displayObject}">
  <c:forEach items="${fieldExprMap[templateQuery]}" var="fieldExpr">
    <c:set var="fieldName" value="${fn:split(fieldExpr, '.')[1]}"/>
    <c:set var="fieldValue" value="${displayObject.object[fieldName]}"/>
    <c:set var="extra" value="${extra}&amp;${fieldExpr}_value=${fieldValue}"/>
  </c:forEach>
  </c:when>
  <c:otherwise>
    <c:set var="extra" value="&amp;bagName=${interMineIdBag.name}&amp;useBagNode=${fieldExprMap[templateQuery]}"/>
  </c:otherwise>
  </c:choose>
</c:if>
[<html:link action="/modifyDetails?method=runTemplate&amp;name=${templateQuery.name}&amp;type=global${extra}&amp;trail=${param.trail}">
  Show in table...
</html:link>]

<%-- Update ui given results of this template --%>

<c:choose>
  <c:when test="${table == null}">
    <script type="text/javascript">
      <%-- Please don't CDATA this script element. See [762] --%>
      $('img_${fn:replace(placement, ' ', '_')}_${templateQuery.name}').src='images/blank.gif';
    </script>
  </c:when>
  <c:otherwise>
    <script type="text/javascript">
      <%-- Please don't CDATA this script element. See [762] --%>
      id = '${fn:replace(placement, ' ', '_')}_${templateQuery.name}';
      if (${table.resultsSize} == 0) {
        $('img_'+id).src='images/plus-disabled.gif';
        $('label_'+id).className='nullStrike';
        $('count_'+id).innerHTML='no results';
        $('img_'+id).parentNode.href='#';
        $('img_'+id).parentNode.onclick = function(){return false;};
      } else {
        $('count_'+id).innerHTML='<a href=\"modifyDetails.do?method=runTemplate&amp;name=${templateQuery.name}&amp;type=global${extra}\" title=\"View in table\">${table.resultsSize} results</a>';
      }
    </script>
  </c:otherwise>
</c:choose>


<!-- /objectDetailsTemplateTable.jsp -->
