<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetails.jsp -->
<c:set var="helpUrl" 
       value="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualDetailedresults.html"/>

<im:box helpUrl="${helpUrl}"
        titleKey="objectDetails.heading.details">

<table width="100%">
<tr>
  <td valign="top" width="30%">
<im:heading id="summary">
Summary for selected
<c:forEach items="${object.clds}" var="cld">
  ${cld.unqualifiedName}
</c:forEach>
</im:heading>

<im:body id="summary">
<table cellpadding="5" rules="all">
  <c:forEach items="${object.keyAttributes}" var="attributeName">
    <tr>
      <td>
        <span class="attributeField">${attributeName}</span>
      </td>
      <td colspan="2"><span class="value">${object.attributes[attributeName]}</span></td>
    </tr>
  </c:forEach>
  <c:forEach items="${object.keyReferences}" var="referenceName">
    <c:set var="reference" value="${object.references[referenceName]}"/>
    <c:forEach items="${object.references[referenceName].keyAttributes}" var="attributeEntry" varStatus="status">
      <tr>
        <c:if test="${status.first}">
          <td rowspan="${fn:length(object.references[referenceName].keyAttributes)}">
            <html:link action="/objectDetails?id=${object.references[referenceName].id}">
              <span class="referenceField">${referenceName}</span>
            </html:link>
          </td>
        </c:if>
        <td>
          <span class="attributeField">${attributeEntry.key}</span>
        </td>
        <td>
          <span class="value">${attributeEntry.value}</span>
        </td>
      </tr>
    </c:forEach>
  </c:forEach>
</table>
</im:body>


<c:if test="${!empty object.attributes}">
  <im:heading id="fields">Fields</im:heading>
  <im:body id="fields">
    <table>
    <c:forEach items="${object.attributes}" var="entry">
      <tr>
        <td>
          <span class="attributeField">${entry.key}</span>
        </td>
        <td>
          <c:set var="maxLength" value="60"/>
          <c:choose>
            <c:when test="${entry.value.class.name == 'java.lang.String' && fn:length(entry.value) > maxLength}">
              <nobr>
                <span class="value">${fn:substring(entry.value, 0, maxLength)}</span>
                <html:link action="/getAttributeAsFile?object=${object.id}&field=${entry.key}">...</html:link>
              </nobr>
            </c:when>
            <c:otherwise>
              <span class="value">${entry.value}</span>
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:forEach>
    </table>
  </im:body>
</c:if>

<c:forEach items="${object.clds}" var="cld">
  <c:if test="${fn:length(DISPLAYERS[cld.name].longDisplayers) > 0}">
    <im:heading id="further">
      <nobr>Further information for this ${cld.unqualifiedName}</nobr>
    </im:heading>
    <im:body id="further">
      <c:forEach items="${DISPLAYERS[cld.name].longDisplayers}" var="displayer">
        <c:set var="object_bak" value="${object}"/>
        <c:set var="object" value="${object.object}" scope="request"/>
        <c:set var="cld" value="${cld}" scope="request"/>
        <tiles:insert beanName="displayer" beanProperty="src"/><br/>
        <c:set var="object" value="${object_bak}"/>
      </c:forEach>
    </im:body>
  </c:if>
</c:forEach>

</td>

<td valign="top" width="66%">
<im:heading id="other"><nobr>Other Information</nobr></im:heading>
<im:body id="other">
<table>
  <c:if test="${!empty object.refsAndCollections}">
    <c:forEach items="${object.refsAndCollections}" var="entry">
      <c:set var="collection" value="${entry.value}"/>
      <c:set var="verbose" value="${!empty object.verbosity[entry.key]}"/>
      <c:set var="fieldName" value="${entry.key}"/>
      <tr>
        <td width="10px">
          <nobr>
            <c:choose>
              <c:when test="${verbose}">
                <html:link action="/modifyDetails?method=unverbosify&field=${fieldName}">
                  <img border="0" src="images/minus.gif" alt="-"/>
                  <span class="collectionField">${fieldName}</span>
                </html:link>
              </c:when>
              <c:when test="${collection.size > 0}">
                <html:link action="/modifyDetails?method=verbosify&field=${fieldName}">
                  <img border="0" src="images/plus.gif" alt="+"/>
                  <span class="collectionField">${fieldName}</span>
                </html:link>
              </c:when>
              <c:otherwise>
                <img border="0" src="images/blank.gif" alt=" "/>
              </c:otherwise>
            </c:choose>
          </nobr>
        </td>
        <td>
          <span class="collectionDescription">
            ${collection.size} <span class="type">${collection.cld.unqualifiedName}</span>
          </span>
          <c:if test="${collection.size == 1 && empty object.verbosity[fieldName]}">
            [<html:link action="/objectDetails?id=${collection.table.ids[0]}">
              <fmt:message key="results.details"/>
            </html:link>]
          </c:if>
        </td>
      </tr>
      <c:if test="${verbose}">
        <tr>
          <td colspan="2">
            <table rules="all" width="100%" class="refSummary">
              <thead style="text-align: center">
                <tr>
                  <td width="10px">
                    <fmt:message key="objectDetails.class"/>
                  </td>
                  <c:forEach items="${collection.table.columnNames}" var="fd">
                    <td><span class="attributeField">${fd}</span></td>
                  </c:forEach>
                  <td width="10px">
                    &nbsp;<%--for IE--%>
                  </td>
                </tr>
              </thead>
              <tbody>
                <c:forEach items="${collection.table.rows}" var="row" varStatus="status">
                  <%-- request scope for im:eval --%>
                  <c:set var="thisRowObject" value="${collection.table.rowObjects[status.index]}"
                         scope="request"/>
                  <tr>
                    <td width="10px">
                      <c:forEach items="${collection.table.types[status.index]}" var="cld">
                        <span class="type">${cld.unqualifiedName}</span>
                      </c:forEach>
                    </td>
                    <c:forEach items="${row}" var="expr">
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
                    <td width="10px">
                      [<html:link action="/objectDetails?id=${collection.table.ids[status.index]}">
                        <fmt:message key="results.details"/>
                      </html:link>]
                    </td>
                  </tr>
                </c:forEach>
              </tbody>
            </table>
            <c:if test="${collection.size > WEB_PROPERTIES['inline.table.size']}">
              <div class="refSummary">
                [<html:link action="/collectionDetails?id=${object.id}&field=${fieldName}&pageSize=25">
                  <fmt:message key="results.showall"/>
                </html:link>]
              </div>
            </c:if>
          </td>
        </tr>
      </c:if>
    </c:forEach>
  </c:if>
</table>
</im:body>

</td>
</tr>
</table>


<c:if test="${RESULTS_TABLE != null && RESULTS_TABLE.size > 0}">
  <html:link action="/changeResults?method=reset">
    <fmt:message key="results.return"/>
  </html:link>
</c:if>

</im:box>

<im:vspacer height="12"/>

<c:set var="showTemplatesFlag" value="false"/>

<c:forEach items="${object.clds}" var="cld">
  <c:set var="className" value="${cld.name}"/>
  <c:if test="${!empty CLASS_CATEGORY_TEMPLATES[className]}">
    <c:set var="showTemplatesFlag" value="true"/>
  </c:if>
</c:forEach>

<c:if test="${showTemplatesFlag == 'true'}">
  <c:set var="helpUrl" 
         value="${WEB_PROPERTIES['project.sitePrefix']}/doc/manual/manualTemplatequeries.html"/>
  
  <im:box helpUrl="${helpUrl}"
          titleKey="objectDetails.heading.templates">
    <c:forEach items="${CATEGORIES}" var="category">
      <c:forEach items="${object.clds}" var="cld">
        <c:set var="className" value="${cld.name}"/>
        <c:if test="${!empty CLASS_CATEGORY_TEMPLATES[className][category]}">
          <div class="heading">${category}</div>
          <c:set var="interMineObject" value="${object.object}"/>
          <div class="body">
            <im:templateList type="global" category="${category}" className="${className}" 
                             interMineObject="${object.object}"/>
          </div>
          <im:vspacer height="5"/>
        </c:if>
      </c:forEach>
    </c:forEach>
  </im:box>
</c:if>

<!-- /objectDetails.jsp -->
