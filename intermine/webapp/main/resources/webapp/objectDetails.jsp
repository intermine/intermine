<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- objectDetails.jsp -->
<html:xhtml/>

<link rel="stylesheet" href="css/resultstables.css" type="text/css" />

<script type="text/javascript">
<!--//<![CDATA[
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
  var detailsType = 'object';
//]]>-->
</script>
<script type="text/javascript" src="js/inlinetemplate.js"></script>

<%-- figure out whether we should show templates or not --%>
<c:set var="showTemplatesFlag" value="false"/>


<tiles:get name="objectTrail.tile"/>
<c:if test="${!empty lookupResults}">
   <tiles:insert name="bagRunnerMsg.tile">
      <tiles:put name="lookupResults" beanName="lookupResults" />
    </tiles:insert>
 <%-- lookupReport --%>
</c:if>

<c:if test="${!empty object}">

  <table width="100%">
    <tr>
      <td valign="top" width="30%">

          <div class="heading">
            <c:if test="${not empty stableLink}">
              <a target="new" href="${stableLink}" onclick="jQuery('div.popup').show().find('input').select();return false;" id="permalink">
                Link
              </a>
              <div class="popup" style="display:none;">
                <span class="close" onclick="jQuery('div.popup').hide();return false;"></span>
                   <p>Paste the following link</p>
                   <input type="text" value="${stableLink}" />
              </div>
            </c:if>
            Summary for selected
            <c:forEach items="${object.clds}" var="cld">
              ${cld.unqualifiedName}
            </c:forEach>
          </div>

        <im:body id="summary">
          <table cellpadding="5" border="0" cellspacing="0" class="objSummary">

            <%-- Show the summary fields as configured in webconfig.xml --%>
            <c:forEach items="${object.fieldExprs}" var="expr">
              <c:choose>
              <c:when test="${object.fieldConfigMap[expr].showInSummary && ! empty object.fieldConfigMap[expr].displayer}">
              <tr>
                <td nowrap>
                    <b><span class="attributeField">${expr}</span></b>
                </td>
                <td nowrap>
                  <c:set var="interMineObject" value="${object.object}" scope="request"/>
                  <b> <span class="value">
                      <tiles:insert page="${object.fieldConfigMap[expr].displayer}">
                        <tiles:put name="expr" value="${expr}" />
                      </tiles:insert>
                  </span> </b>
                </td>
              </tr>
              </c:when>
              <c:when test="${object.fieldConfigMap[expr].showInSummary}">
                <tr>
                  <td nowrap>
                    <b><span class="attributeField">${expr}</span></b>
                    <c:forEach items="${object.clds}" var="cld">
                      <im:typehelp type="${cld.unqualifiedName}.${expr}"/>
                    </c:forEach>
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${empty object.fieldValues || empty object.fieldValues[expr]}">
                        &nbsp;
                      </c:when>
                      <c:otherwise>
                       <b><im:value>${object.fieldValues[expr]}</im:value></b>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:when>
              </c:choose>
            </c:forEach>

            <%-- Show all other fields --%>
            <c:forEach items="${object.attributes}" var="entry">
              <c:if test="${! object.fieldConfigMap[entry.key].showInSummary && !object.fieldConfigMap[entry.key].sectionOnRight}">
                <tr>
                  <td>
                    <span class="attributeField">${entry.key}</span>
                    <c:forEach items="${object.clds}" var="cld">
                      <im:typehelp type="${cld.unqualifiedName}.${entry.key}"/>
                    </c:forEach>
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${object.longAttributes[entry.key] != null}">
                        <span class="value" style="white-space:nowrap">
                          ${object.longAttributes[entry.key]}
                          <c:if test="${object.longAttributesTruncated[entry.key] != null}">
                            <html:link action="/getAttributeAsFile?object=${object.id}&amp;field=${entry.key}">
                              <fmt:message key="objectDetails.viewall"/>
                            </html:link>
                          </c:if>
                        </span>
                      </c:when>
                      <c:otherwise>
                      <im:value>${entry.value}</im:value>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:if>
            </c:forEach>
          </table>

          <im:vspacer height="15"/>

          <c:forEach items="${object.clds}" var="cld">
            <tiles:insert page="/objectDetailsRefsCols.jsp">
              <tiles:put name="object" beanName="object"/>
              <tiles:put name="placement" value="im:summary"/>
            </tiles:insert>
          </c:forEach>

          <%-- Show the *table* displayers for this object type --%>
          <c:forEach items="${LEAF_DESCRIPTORS_MAP[object.object]}" var="cld2">
            <c:if test="${WEBCONFIG.types[cld2.name].tableDisplayer != null}">
              <p><tiles:insert page="${WEBCONFIG.types[cld2.name].tableDisplayer.src}"/></p>
            </c:if>
          </c:forEach>

        </im:body>

      </td>

      <td valign="top" width="66%">

        <%-- Long displayers --%>
        <tiles:insert page="/objectDetailsDisplayers.jsp">
          <tiles:put name="placement" value=""/>
          <tiles:put name="displayObject" beanName="object"/>
          <tiles:put name="heading" value="true"/>
        </tiles:insert>

        <%-- Fields that are set to 'sectionOnRight' --%>
        <c:forEach items="${object.attributes}" var="entry">
          <c:if test="${object.fieldConfigMap[entry.key].sectionOnRight}">

            <imutil:disclosure id="${objectType}objectDetailsRight-${entry.key}" opened="true" type="consistent">
              <imutil:disclosureHead>
                <imutil:disclosureTitle>
                  ${object.fieldConfigMap[entry.key].sectionTitle}
                </imutil:disclosureTitle>
              </imutil:disclosureHead>
              <imutil:disclosureBody>
           <c:set var="maxLength" value="80"/>
           <c:choose>
             <c:when test="${entry.value.class.name ==
                           'java.lang.String' && fn:length(entry.value) > maxLength
                           && !object.fieldConfigMap[entry.key].doNotTruncate}">
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
              </imutil:disclosureBody>
            </imutil:disclosure>
          </c:if>
        </c:forEach>

   <%-- bags that contain this object --%>
   <tiles:insert name="objectDetailsInList.tile">
     <tiles:put name="objectid" value="${object.id}"/>
   </tiles:insert>
   </td>

    </tr>

  </table>

</c:if>
<c:if test="${empty object}">
  <%-- Display message if object not found --%>
  <im:vspacer height="12"/>
  <div class="altmessage">
    <fmt:message key="objectDetails.noSuchObject"/>
  </div>
  <im:vspacer height="12"/>
</c:if>


<c:if test="${!empty object}">
  <im:vspacer height="12"/>


        <%-- Long displayers to show on left --%>
        <tiles:insert page="/objectDetailsDisplayers.jsp">
          <tiles:put name="placement" value=""/>
          <tiles:put name="displayObject" beanName="object"/>
          <tiles:put name="heading" value="false"/>
          <tiles:put name="showOnLeft" value="true"/>
        </tiles:insert>

<c:set value="${fn:length(CATEGORIES)}" var="aspectCount"/>
<c:set var="templateIdPrefix" value="objectDetailsTemplate${objectType}"/>
<c:set var="miscId" value="objectDetailsMisc${objectType}"/>



<div class="heading">Further Information by Category&nbsp;&nbsp;&nbsp;<span style="font-size:0.8em;">
 (<a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'expand', '${miscId}', true);">expand all <img src="images/disclosed.gif"/></a> / <a href="javascript:toggleAll(${aspectCount}, '${templateIdPrefix}', 'collapse', '${miscId}', true);">collapse all <img src="images/undisclosed.gif"/></a>)</span></div>

    <%-- Each aspect --%>
    <c:forEach items="${CATEGORIES}" var="aspect" varStatus="status">
      <tiles:insert name="objectDetailsAspect.tile">
        <tiles:put name="placement" value="im:aspect:${aspect}"/>
        <tiles:put name="displayObject" beanName="object"/>
        <tiles:put name="trail" value="${request.trail}"/>
        <tiles:put name="aspectId" value="${templateIdPrefix}${status.index}" />
        <tiles:put name="opened" value="${status.index == 0}" />
      </tiles:insert>
    </c:forEach>

    <%-- All other references and collections --%>
  <imutil:disclosure id="${miscId}" opened="false" type="consistent">
    <imutil:disclosureHead>
      <imutil:disclosureTitle>
        Miscellaneous
      </imutil:disclosureTitle>
      <imutil:disclosureDetails styleClass="templateResultsToggle">
        (Expand this section for more information)
      </imutil:disclosureDetails>
    </imutil:disclosureHead>
    <imutil:disclosureBody styleClass="disclosureBody">
        <tiles:insert page="/objectDetailsRefsCols.jsp">
            <tiles:put name="object" beanName="object"/>
            <tiles:put name="placement" value="im:aspect:Miscellaneous"/>
          </tiles:insert>
    </imutil:disclosureBody>
  </imutil:disclosure>
</div>
</c:if>

<!-- /objectDetails.jsp -->
