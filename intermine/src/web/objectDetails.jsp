<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- objectDetails.jsp -->
<table width="100%">
<tr>
  <td valign="top" width="34%">
<div class="heading">
Summary for selected
<c:forEach items="${object.clds}" var="cld">
  <span class="type">${cld.unqualifiedName}</span>
</c:forEach>
</div>

<div class="body">
<table cellpadding="5" rules="all">
  <c:forEach items="${object.keyAttributes}" var="entry">
    <tr>
      <td>
        <span class="attributeField">${entry.key}</span>
      </td>
      <td colspan="2"><span class="value">${entry.value}</span></td>
    </tr>
  </c:forEach>

  <c:forEach items="${object.keyReferences}" var="keyRefEntry">
    <c:forEach items="${keyRefEntry.value.identifiers}" var="entry" varStatus="status">
      <tr>
        <c:if test="${status.first}">
          <td rowspan="${fn:length(keyRefEntry.value.identifiers)}">
            <html:link action="/objectDetails?id=${keyRefEntry.value.id}">
              <span class="referenceField">${keyRefEntry.key}</span>
            </html:link>
          </td>
        </c:if>
        <td>
          <span class="attributeField">${entry.key}</span>
        </td>
        <td>
          <span class="value">${entry.value}</span>
        </td>
      </tr>
    </c:forEach>
  </c:forEach>
</table>
</div>
<br/>

<div class="heading">Fields</div>

<table>
<c:if test="${!empty object.attributes}">
  <div style="margin-left: 20px">
    <c:forEach items="${object.attributes}" var="entry">
      <tr>
        <td>
          <img border="0" src="images/blank.png" alt=" "/>
          <span class="attributeField">${entry.key}</span>
        </td>
        <td>
          <c:set var="maxLength" value="60"/>
          <c:choose>
            <c:when test="${entry.value.class.name == 'java.lang.String' && fn:length(entry.value) > maxLength}">
              <span class="value">${fn:substring(entry.value, 0, maxLength)}</span>
              <html:link action="/getAttributeAsFile?object=${object.id}&field=${entry.key}">...</html:link>
            </c:when>
            <c:otherwise>
              <span class="value">${entry.value}</span>
            </c:otherwise>
          </c:choose>
        </td>
      </tr>
    </c:forEach>
  </div>
  <tr>
    <td  height="10px">
    </td>
  </tr>
</c:if>
</table>

<c:forEach items="${object.clds}" var="cld">
  <c:if test="${fn:length(DISPLAYERS[cld.name].longDisplayers) > 0}">
    <div class="heading">
      Further information for this <span
      class="type">${cld.unqualifiedName}</span>
    </div>
    <div style="margin-left: 20px">
      <c:forEach items="${DISPLAYERS[cld.name].longDisplayers}" var="displayer">
        <c:set var="object_bak" value="${object}"/>
        <c:set var="object" value="${object.object}" scope="request"/>
        <c:set var="cld" value="${cld}" scope="request"/>
        <tiles:insert beanName="displayer" beanProperty="src"/><br/>
        <c:set var="object" value="${object_bak}"/>
      </c:forEach>
    </div>
  </c:if>
</c:forEach>
<br/>


</td>

<td valign="top" width="66%">
<div class="heading">Other Information</div>
<div class="body">
<table>
<c:if test="${!empty object.references}">
  <div style="margin-left: 20px">
    <c:forEach items="${object.references}" var="entry">
      <tr>
        <td width="10px">
          <c:set var="reference" value="${entry.value}"/>
          <c:set var="verbose" value="${!empty object.verbosity[entry.key]}"/>
          <nobr>
            <c:choose>
              <c:when test="${verbose}">
                <html:link action="/modifyDetails?method=unverbosify&field=${entry.key}">
                  <img border="0" src="images/minus.png" alt="-"/>
                  <span class="referenceField">${entry.key}</span>
                </html:link>
              </c:when>
              <c:otherwise>
                <html:link action="/modifyDetails?method=verbosify&field=${entry.key}">
                  <img border="0" src="images/plus.png" alt="+"/>
                  <span class="referenceField">${entry.key}</span>
                </html:link>
              </c:otherwise>
            </c:choose>
          </nobr>
        </td>
        <td>
          <span class="collectionDescription">          
            <span class="type">
              ${reference.cld.unqualifiedName}
            </span>
          </span>
          <c:if test="${!verbose}">
            [<html:link action="/objectDetails?id=${reference.id}">
              <fmt:message key="results.details"/>
            </html:link>]
          </c:if>
        </td>
      </tr>
      <c:if test="${verbose}">
        <tr>
          <td colspan="2">
            <table rules="all" width="100%" class="refSummary">
              <thead>
                <tr>
                  <td width="10px">
                    <fmt:message key="objectDetails.class"/>
                  </td>
                  <c:forEach items="${reference.identifiers}" var="entry">
                    <td>
                      <span class="attributeField">${entry.key}</span>
                    </td>
                  </c:forEach>
                  <td width="10px">
                    &nbsp;
                  </td>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td width="10px">
                    <span class="type">
                      ${reference.cld.unqualifiedName}
                    </span>
                  </td>
                  <c:forEach items="${reference.identifiers}" var="entry">
                    <td>
                      <span class="value">${entry.value}</span><br/>
                    </td>
                  </c:forEach>
                  <td width="10px">
                    [<html:link action="/objectDetails?id=${reference.id}">
                      <fmt:message key="results.details"/>
                    </html:link>]
                  </td>
                </tr>
              </tbody>
            </table>
          </td>
        </tr>
      </c:if>
    </c:forEach>
  </div>
</c:if>

<c:if test="${!empty object.collections}">
  <div style="margin-left: 20px">
    <c:forEach items="${object.collections}" var="entry">
      <tr>
        <td width="10px">
          <c:set var="collection" value="${entry.value}"/>
          <c:set var="verbose" value="${!empty object.verbosity[entry.key]}"/>
          <nobr>
            <c:choose>
              <c:when test="${verbose}">
                <html:link action="/modifyDetails?method=unverbosify&field=${entry.key}">
                  <img border="0" src="images/minus.png" alt="-"/>
                  <span class="collectionField">${entry.key}</span>
                </html:link>
              </c:when>
              <c:when test="${collection.size > 0}">
                <html:link action="/modifyDetails?method=verbosify&field=${entry.key}">
                  <img border="0" src="images/plus.png" alt="+"/>
                  <span class="collectionField">${entry.key}</span>
                </html:link>
              </c:when>
              <c:otherwise>
                <img border="0" src="images/blank.png" alt=" "/>
              </c:otherwise>
            </c:choose>
          </nobr>
        </td>
        <td>
          <span class="collectionDescription">
            ${collection.size} <span class="type">${collection.cld.unqualifiedName}</span> objects
          </span>
          <c:if test="${collection.size == 1 && empty object.verbosity[entry.key]}">
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
                  <td width="10px" rowspan="2">
                    <fmt:message key="objectDetails.class"/>
                  </td>
                  <c:forEach items="${collection.table.keyAttributes}" var="fd">
                    <td rowspan="2"><span class="attributeField">${fd.name}</span></td>
                  </c:forEach>
                  <c:forEach items="${collection.table.keyReferences}" var="keyEntry">
                    <td colspan="${fn:length(keyEntry.value)}">
                      <span class="attributeField">${keyEntry.key.name}</span>
                    </td>
                  </c:forEach>
                  <td width="10px" rowspan="2"/>
                </tr>
                <tr>
                  <c:forEach items="${collection.table.keyReferences}" var="keyEntry">
                    <c:forEach items="${keyEntry.value}" var="fd">
                      <td><span class="attributeField">${fd.name}</span></td>
                    </c:forEach>
                  </c:forEach>
                </tr>
              </thead>
              <tbody>
                <c:forEach items="${collection.table.rows}" var="row" varStatus="status">
                  <tr>
                    <td width="10px">
                      <c:forEach items="${collection.table.types[status.index]}" var="cld">
                        <span class="type">${cld.unqualifiedName}</span>
                      </c:forEach>
                    </td>
                    <c:forEach items="${row}" var="obj">
                      <td><span class="value">${obj}</span></td>
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
            <c:if test="${collection.size > 10}">
              <div class="refSummary">
                [<html:link action="/collectionDetails?id=${object.id}&field=${entry.key}&pageSize=25">
                  <fmt:message key="results.showall"/>
                </html:link>]
              </div>
            </c:if>
          </td>
        </tr>
      </c:if>
    </c:forEach>
  </div>
</c:if>
</table>
</div>
<br/>

</td>
</tr>
</table>


<c:if test="${RESULTS_TABLE != null && RESULTS_TABLE.size > 0}">
  <html:link action="/changeResults?method=reset">
    <fmt:message key="results.return"/>
  </html:link>
</c:if>
<!-- /objectDetails.jsp -->
