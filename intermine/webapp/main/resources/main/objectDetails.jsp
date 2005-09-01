<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetails.jsp -->
<html:xhtml/>
<c:set var="helpUrl"
       value="${WEB_PROPERTIES['project.helpLocation']}/manual/manualObjectDetails.html"/>

<%-- figure out whether we should show templates or not --%>
<c:set var="showTemplatesFlag" value="false"/>
<c:set var="showImportantTemplatesFlag" value="false"/>

<c:forEach items="${object.clds}" var="cld">
  <c:set var="className" value="${cld.name}"/>
  <c:if test="${!empty CLASS_CATEGORY_TEMPLATES[className]}">
    <c:set var="showTemplatesFlag" value="true"/>
    <c:forEach items="${CLASS_CATEGORY_TEMPLATES[className]}" var="cats">
      <c:forEach items="${cats.value}" var="tmpl">
        <c:if test="${tmpl.important}">
          <c:set var="showImportantTemplatesFlag" value="true"/>
        </c:if>
      </c:forEach>
    </c:forEach>
  </c:if>
</c:forEach>

<im:box helpUrl="${helpUrl}"
        titleKey="objectDetails.heading.details">

<tiles:get name="objectTrail.tile"/>

<c:if test="${!empty object}">

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
          <table cellpadding="5" border="0" cellspacing="0" class="objSummary">
            <c:forEach items="${object.fieldExprs}" var="expr">
              <c:if test="${object.fieldConfigMap[expr].showInSummary}">
                <im:eval evalExpression="object.object.${expr}" evalVariable="outVal"/>
                <tr>
                  <td>
                    <b><span class="attributeField">${expr}</span></b>
                  </td>
                  <td>
                    <c:choose>                      
                      <c:when test="${empty outVal}">
                        <%-- add a space so that IE renders the borders --%>
                        &nbsp
                      </c:when>
                      <c:otherwise>
                        <span class="value"><b>${outVal}</b></span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
          
            <c:forEach items="${object.attributes}" var="entry">
              <c:if test="${! object.fieldConfigMap[entry.key].showInSummary && !object.fieldConfigMap[entry.key].sectionOnRight}">
                <tr>
                  <td>
                    <span class="attributeField">${entry.key}</span>
                  </td>
                  <td>
                    <c:set var="maxLength" value="60"/>
                    <c:choose>
                      <c:when test="${entry.value.class.name ==
                                    'java.lang.String' && fn:length(entry.value) > maxLength
                                    && ! object.fieldConfigMap[entry.key].doNotTruncate}">
                        <span class="value">
                          ${fn:substring(entry.value, 0, maxLength/2)}
                        </span>
                        <span class="value" style="white-space:nowrap">
                          ${fn:substring(entry.value, maxLength/2, maxLength)}
                          <html:link action="/getAttributeAsFile?object=${object.id}&amp;field=${entry.key}">
                            <fmt:message key="objectDetails.viewall"/>
                          </html:link>
                        </span>
                      </c:when>
                      <c:otherwise>
                        <span class="value">${entry.value}</span>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
          </table>
        </im:body>

        <c:forEach items="${object.clds}" var="cld">
          <c:if test="${fn:length(WEBCONFIG.types[cld.name].longDisplayers) > 0}">
            <im:heading id="further">
              <span style="white-space:nowrap">Further information for this ${cld.unqualifiedName}</span>
            </im:heading>
            <im:vspacer height="3"/>
            <im:body id="further">
              <c:forEach items="${WEBCONFIG.types[cld.name].longDisplayers}" var="displayer">
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

        <%-- show important templates here --%>
        <c:if test="${showImportantTemplatesFlag == 'true'}">
          <im:heading id="important">Predefined template queries</im:heading>
          <im:vspacer height="3"/>
          <im:body id="important">
            <c:forEach items="${CATEGORIES}" var="category">
              <c:forEach items="${object.clds}" var="cld">
                <c:set var="className" value="${cld.name}"/>
                <c:if test="${!empty CLASS_CATEGORY_TEMPLATES[className][category]}">
                  <%--<div class="heading">${category}</div>--%>
                  <c:set var="interMineObject" value="${object.object}"/>
                  <!--<div class="body">-->
                    <im:templateList type="global" category="${category}" className="${className}"
                                     interMineObject="${object.object}" important="true"/>
                  <!--</div>-->
                  <%--<im:vspacer height="5"/>--%>
                </c:if>
              </c:forEach>
            </c:forEach>
          </im:body>
          <im:vspacer height="6"/>
        </c:if>

        <im:heading id="other">Other Information<im:helplink key="objectDetails.help.otherInfo"/></im:heading>
        <im:body id="other">
          <table border="0">
            <c:if test="${!empty object.refsAndCollections}">
              <c:forEach items="${object.refsAndCollections}" var="entry">
                <c:set var="collection" value="${entry.value}"/>
                <c:set var="verbose" value="${!empty object.verbosity[entry.key]}"/>
                <c:set var="fieldName" value="${entry.key}"/>
                <tr>
                  <td width="10px">
                    <div style="white-space:nowrap">
                      <c:choose>
                        <c:when test="${verbose}">
                          <html:link action="/modifyDetails?method=unverbosify&amp;field=${fieldName}&amp;id=${object.id}&amp;trail=${param.trail}">
                            <img border="0" src="images/minus.gif" alt="-" width="11" height="11"/>
                            <span class="collectionField">${fieldName}</span>
                          </html:link>
                        </c:when>
                        <c:when test="${collection.size > 0}">
                          <html:link action="/modifyDetails?method=verbosify&amp;field=${fieldName}&amp;id=${object.id}&amp;trail=${param.trail}">
                            <img border="0" src="images/plus.gif" alt="+" width="11" height="11"/>
                            <span class="collectionField">${fieldName}</span>
                          </html:link>
                        </c:when>
                        <c:otherwise>
                          <span class="nullStrike">
                            <img border="0" src="images/plus-disabled.gif" alt=" " width="11" height="11"/>
                            <span class="collectionField nullReferenceField">${fieldName}</span>
                          </span>
                        </c:otherwise>
                      </c:choose>
                    </div>
                  </td>
                  <td>
                    <span class="collectionDescription ${collection.size == 0 ? 'nullReferenceField' : ''}">
                      ${collection.size} <span class="type">${collection.cld.unqualifiedName}</span>
                    </span>
                    <c:if test="${collection.size == 1 && empty object.verbosity[fieldName]}">
                      [<html:link action="/objectDetails?id=${collection.table.ids[0]}&amp;trail=${param.trail}_${collection.table.ids[0]}">
                        <fmt:message key="results.details"/>
                      </html:link>]
                    </c:if>
                    <c:if test="${collection.size == 0}">
                      </span>
                    </c:if>
                  </td>
                </tr>
                <c:if test="${verbose}">
                  <tr>
                    <td colspan="2">
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
                      <c:choose>
                        <c:when test="${collection.size > WEB_PROPERTIES['inline.table.size']}">
                          <div class="refSummary">
                            [<html:link action="/collectionDetails?id=${object.id}&amp;field=${fieldName}&amp;pageSize=25&amp;trail=${param.trail}">
                              <fmt:message key="results.showallintable"/>
                            </html:link>]
                          </div>
                        </c:when>
                        <c:otherwise>
                          <div class="refSummary">
                            [<html:link action="/collectionDetails?id=${object.id}&amp;field=${fieldName}&amp;pageSize=25&amp;trail=${param.trail}">
                              <fmt:message key="results.showintable"/>
                            </html:link>]
                          </div>
                        </c:otherwise>
                      </c:choose>
                    </td>
                  </tr>
                </c:if>
              </c:forEach>
            </c:if>
          </table>
        </im:body>
        
        <c:forEach items="${object.attributes}" var="entry">
          <c:if test="${object.fieldConfigMap[entry.key].sectionOnRight}">
          	<im:heading id="right-${entry.key}">
          	  ${object.fieldConfigMap[entry.key].sectionTitle}
          	</im:heading>
          	<im:body id="right-${entry.key}">
          	
          	       <c:set var="maxLength" value="80"/>
                    <c:choose>
                      <c:when test="${entry.value.class.name ==
                                    'java.lang.String' && fn:length(entry.value) > maxLength
                                    && ! object.fieldConfigMap[entry.key].doNotTruncate}">
                        <span class="value">
                          ${fn:substring(entry.value, 0, maxLength/2)}
                        </span>
                        <span class="value" style="white-space:nowrap">
                          ${fn:substring(entry.value, maxLength/2, maxLength)}
                          <html:link action="/getAttributeAsFile?object=${object.id}&amp;field=${entry.key}">
                            <fmt:message key="objectDetails.viewall"/>
                          </html:link>
                        </span>
                      </c:when>
                      <c:otherwise>
                        <span class="value">${entry.value}</span>
                      </c:otherwise>
                    </c:choose>
                    
          	</im:body>
          </c:if>
        </c:forEach>    
        
      </td>

    </tr>
  </table>

</c:if>

<div class="body">
            <c:if test="${!empty PROFILE.savedBags}">
              <form action="<html:rewrite page="/addToBagAction.do"/>" method="POST">
                <fmt:message key="objectDetails.addToBag"/>
                <input type="hidden" name="__intermine_forward_params__" value="${pageContext.request.queryString}"/>
                <select name="bag">
                  <c:forEach items="${PROFILE.savedBags}" var="entry">
                    <option name="${entry.key}">${entry.key}</option>
                  </c:forEach>
                </select>
                <input type="hidden" name="object" value="${object.id}"/>
                <input type="submit" value="<fmt:message key="button.add"/>"/>
              </form>
            </c:if>
</div>

<c:if test="${empty object}">
  <%-- Display message if object not found --%>
  <div class="altmessage">
    <fmt:message key="objectDetails.noSuchObject"/>
  </div>
</c:if>

</im:box>

<im:vspacer height="12"/>

<c:if test="${showTemplatesFlag == 'true'}">
  <c:set var="helpUrl"
         value="${WEB_PROPERTIES['project.helpLocation']}/manual/manualTemplatequeries.html"/>

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
