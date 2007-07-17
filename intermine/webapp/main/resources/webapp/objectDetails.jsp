<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- objectDetails.jsp -->
<html:xhtml/>



<script type="text/javascript">
<!--//<![CDATA[
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
  var detailsType = 'object';
//]]>-->
</script>
<script type="text/javascript" src="js/inlinetemplate.js">
  var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
</script>

<%-- figure out whether we should show templates or not --%>
<c:set var="showTemplatesFlag" value="false"/>

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
                <im:eval evalExpression="object.object.${expr}" evalVariable="outVal"/>
                <tr>
                  <td nowrap>
                    <b><span class="attributeField">${expr}</span></b>
                    <c:forEach items="${object.clds}" var="cld">
                      <im:typehelp type="${cld.unqualifiedName}.${expr}"/>
                    </c:forEach>
                  </td>
                  <td>
                    <c:choose>
                      <c:when test="${empty outVal}">
                        &nbsp;
                      </c:when>
                      <c:otherwise>
                        <b><im:value>${outVal}</im:value></b>
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
                    <c:set var="maxLength" value="60"/>
                    <c:choose>
                      <c:when test="${entry.value.class.name ==
                                    'java.lang.String' && fn:length(entry.value) > maxLength
                                    && ! object.fieldConfigMap[entry.key].doNotTruncate}">
                        <im:value>
                          ${fn:substring(entry.value, 0, maxLength/2)}
                        </im:value>
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

          <im:vspacer height="15"/>

          <c:forEach items="${object.clds}" var="cld">
            <tiles:insert page="/objectDetailsRefsCols.jsp">
              <tiles:put name="object" beanName="object"/>
              <tiles:put name="placement" value="placement:summary"/>
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

        <%-- Long displayers not tied to a particular aspect --%>
        <tiles:insert page="/objectDetailsDisplayers.jsp">
          <tiles:put name="placement" value=""/>
          <tiles:put name="displayObject" beanName="object"/>
          <tiles:put name="heading" value="true"/>
        </tiles:insert>

        <tiles:insert name="externalLinks.tile">
          <tiles:put name="object" beanName="object"/>
        </tiles:insert>

        <%-- Fields that are set to 'sectionOnRight' --%>
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
<c:if test="${empty object}">
  <%-- Display message if object not found --%>
  <im:vspacer height="12"/>
  <div class="altmessage">
    <fmt:message key="objectDetails.noSuchObject"/>
  </div>
  <im:vspacer height="12"/>
</c:if>

</im:box>

<c:if test="${!empty object}">
  <im:vspacer height="12"/>
  <im:box helpUrl="${helpUrl}"
          titleKey="objectDetails.heading.byaspect">

    <%-- Each aspect --%>
    <c:forEach items="${CATEGORIES}" var="aspect">
      <tiles:insert name="objectDetailsAspect.tile">
        <tiles:put name="placement" value="aspect:${aspect}"/>
        <tiles:put name="displayObject" beanName="object"/>
        <tiles:put name="trail" value="${request.trail}"/>
      </tiles:insert>
    </c:forEach>

    <%-- All other references and collections --%>
    <im:heading id="Misc">
      Miscellaneous
    </im:heading>
    <im:body id="Misc">
      <tiles:insert page="/objectDetailsRefsCols.jsp">
        <tiles:put name="object" beanName="object"/>
        <tiles:put name="placement" value="aspect:Miscellaneous"/>
      </tiles:insert>
    </im:body>

    <%-- Add to bag --%>
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
    
       <%-- bags that contain this object --%>
    <div class="body" style="width:50%">
        <%-- public bags --%>
        <b>Example lists that contain this object:</b>  
        <div class="webSearchable">
    	  <tiles:insert name="webSearchableList.tile">
         	<tiles:put name="list" value="${publicBagsWithThisObject}"/>
            <tiles:put name="type" value="bag"/>
            <tiles:put name="scope" value="global"/>
            <tiles:put name="showDescriptions" value="true"/>
            <tiles:put name="showSearchBox" value="false"/>
	      </tiles:insert>
		</div>
        
        <%-- my bags --%>
        <b>My lists that contain this object:</b>
        <div class="webSearchable">
      	 <tiles:insert name="webSearchableList.tile">
      	    <tiles:put name="list" value="${myBagsWithThisObject}"/>
            <tiles:put name="type" value="bag"/>
            <tiles:put name="scope" value="user"/>
            <tiles:put name="showDescriptions" value="true"/>
             <tiles:put name="showSearchBox" value="false"/>
     	 </tiles:insert>
		</div>
     </div>

  </im:box>
  
  
  
</c:if>

<!-- /objectDetails.jsp -->
