<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- template.jsp -->

<c:set var="index" value="${0}"/>

<tiles:importAttribute/>
<html:xhtml/>
<script type="text/javascript" src="js/templateForm.js"></script>
<script type="text/javascript" src="js/autocompleter.js"></script>
<link rel="stylesheet" href="css/autocompleter.css" type="text/css"/>
<link rel="stylesheet" href="css/template.css" type="text/css"/>

<%-- object trail --%>
<tiles:get name="objectTrail.tile"/>

<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
<html:form action="/templateAction">

    <%-- template title --%>
    <h2 class="templateTitle">
        <c:set var="templateTitle" value="${fn:replace(templateQuery.title,'-->','&nbsp;<img src=\"images/tmpl_arrow.png\" style=\"vertical-align:middle\">&nbsp;')}" />
        ${templateTitle}
        <tiles:insert name="setFavourite.tile">
            <tiles:put name="name" value="${templateQuery.name}"/>
            <tiles:put name="type" value="template"/>
        </tiles:insert>
    </h2>

    <%-- description --%>
    <div class="templateDescription">${templateQuery.description}</div>

    <ol class="templateForm">

        <%-- constraint list --%>
        <c:forEach items="${templateQuery.editableNodes}" var="node">

            <%-- what's this loop --%>
            <c:forEach items="${constraints[node]}" var="con" >

                <c:set var="index" value="${index+1}"/>
                <c:set var="validOps" value="${displayConstraints[con].validOps}"/>
                <c:set var="fixedOps" value="${displayConstraints[con].fixedOpIndices}"/>
                <c:set var="options" value="${displayConstraints[con].optionsList}"/>
                <c:remove var="bags"/>
                <c:remove var="bagType"/>
                <c:if test="${! empty constraintBags[con]}">
                  <c:set var="bags" value="${constraintBags[con]}"/>
                  <c:set var="bagType" value="${constraintBagTypes[con]}"/>
                </c:if>
                <c:if test="${!empty con.description}">
                  <li class="firstLine"><c:if test="${fn:length(templateQuery.editableNodes) > 1}"><span><c:out value="[${index}]"/></span></c:if><i><c:out value="${con.description}"/></i></li>
                </c:if>
                <li>

               <%-- this should be moved to the js file --%>
              <script type="text/javascript">
              <!--
                   fixedOps = new Array();
                   <c:forEach items="${fixedOps}" var="op" varStatus="oi">
                     fixedOps[${oi.count}] = "<c:out value="${op}"/>";
                   </c:forEach>
                    //-->
              </script>

              <%-- number --%>
              <c:if test="${empty con.description}">
                <c:if test="${fn:length(templateQuery.editableNodes) > 1}"><span><c:out value="[${index}]"/></span></c:if>
              </c:if>

              <%-- constraint name --%>
              <label>
                <c:out value="${displayConstraints[con].name}"/>:
              </label>

              <%-- operator --%>
              <c:choose>
                <c:when test="${fn:length(validOps) == 1}">
                  <input type="hidden" name="attributeOps(${index})" value="18"/>
                </c:when>
                <c:otherwise>
                  <span valign="top">
                    <html:select property="attributeOps(${index})" onchange="updateConstraintForm(${index-1}, document.templateForm['attributeOps(${index})'], document.templateForm['attributeOptions(${index})'], document.templateForm['attributeValues(${index})'])">
                      <c:forEach items="${validOps}" var="op">
                        <html:option value="${op.key}">
                          <c:out value="${op.value}"/>
                        </html:option>
                      </c:forEach>
                    </html:select>
                  </span>
                </c:otherwise>
              </c:choose>

              <%-- autocomplete --%>
              <span nowrap>
                <span id="operandEditSpan${index-1}">

                  <c:set var="pathString" value="${node.pathString}"/>
                  <c:set var="classDesc" value="${classDesc}"/>
                  <c:set var="fieldDesc" value="${fieldDesc}"/>
                  <c:set var="acPath" value="${classDesc[pathString]}.${fieldDesc[pathString]}"/>
                  <c:set var="hasAutoC" value="0"/>

                  <!-- TODO this shouldn't need to loop through map each time -->
                  <c:forEach items="${autoCompleterMap[acPath]}" var="useAC">
                    <%-- exist for this field a autocompleter --%>
                    <c:if test="${!empty useAC  and hasAutoC eq 0}">
                      <input name="attributeValues(${index})" id="attributeId_${index}" size="45" autocomplete="off"
                             style="background:#ffffc8"
                             value="${con.displayValue}"
                             onKeyDown="getId(this.id); isEnter(event);"
                             onKeyUp="readInput(event, '${classDesc[pathString]}', '${fieldDesc[pathString]}');"
                             onMouseOver="setMouseOver(${index});"
                             onMouseOut="setMouseOver(0);"
                             onBlur="if(MOUSE_OVER != ${index}) { removeList(); }"/>
                      <div class="error_auto_complete" id="attributeId_${index}_error" tabindex="-1"></div>
                      <iframe width="100%" height="0" id="attributeId_${index}_IEbugFixFrame" tabindex="-1"
                              marginheight="0" marginwidth="0" frameborder="0" style="position:absolute;" ></iframe>
                      <div class="auto_complete" id="attributeId_${index}_display" tabindex="-1"
                           onMouseOver="setMouseOver(${index});"
                           onMouseOut="setMouseOver(0);"
                           onBlur="if(MOUSE_OVER != ${index}) { removeList(); }"></div>
                      <c:set var="hasAutoC" value="1"/>
                    </c:if>
                  </c:forEach>

                  <%-- no auto completer exists --%>
                  <c:if test="${hasAutoC eq 0}">

                    <c:set var="datePickerClass" value=""/>
                    <c:if test="${node.type == 'Date'}">
                      <c:set var="datePickerClass" value="date-pick"/>
                    </c:if>

                    <%-- input box --%>
                    <html:text property="attributeValues(${index})" styleClass="${datePickerClass}" size="10" />

                    <c:if test="${node.type == 'Date'}">
                      <script type="text/javascript">
                        jQuery('.date-pick').datepicker(
                           {
                              buttonImage: 'images/calendar.png',
                              buttonImageOnly: true,
                              dateFormat: 'yy-mm-dd',
                              showOn: "both",
                              showAnim: 'blind',
                              showOptions: {speed: 'fast'}
                           }
                        );
                      </script>
                    </c:if>
                  </c:if>

                <%-- help link --%>
                <c:if test="${!empty keyFields[con]}">
                  <im:helplink text="Search multiple fields including:  ${keyFields[con]}"/>
                </c:if>

                <%-- might want to show up arrow --%>
                <c:if test="${!empty options}">
                  <img src="images/left-arrow.gif" title="&lt;-" border="0" height="13" width="13"/>
                </c:if>
              </span>

              <%-- dropdown --%>
              <c:if test="${!empty options}">
                <select name="attributeOptions(${index})" onchange="updateAttributeValues(${index});">
                  <c:forEach items="${options}" var="option">
                    <option value="${option}">
                      <c:out value="${option}"/>
                    </option>
                  </c:forEach>
                </select>
              </c:if>
            </span>

                <script type="text/javascript">
                  /* setting options popup value to correct initial state. */
                  if (document.templateForm["attributeOptions(${index})"] != null) {
                        var select = document.templateForm["attributeOptions(${index})"];
                        var value = document.templateForm["attributeValues(${index})"].value;
                        var set = false;
                        for (i=0 ; i<select.options.length ; i++) {
                            if (select.options[i].value == value) {
                                select.selectedIndex = i;
                                set = true;
                                break;
                            }
                        }
                        updateConstraintForm(${index-1}, document.templateForm["attributeOps(${index})"],
                                document.templateForm["attributeOptions(${index})"],
                                document.templateForm["attributeValues(${index})"]);
                  }
                </script>

         <%-- dropdown (probably organism) --%>
          <c:if test="${haveExtraConstraint[con]}">
              <c:if test="${empty keyFields[con]}">
                 </li>
              <li>
              </c:if>
              <span valign="top" colspan="4" style="color:#eee;">
                <label class="marg">
                  <fmt:message key="bagBuild.extraConstraint">
                    <fmt:param value="${extraBagQueryClass}"/>
                  </fmt:message>
                </label>
                <html:select property="extraValues(${index})">
                  <html:option value="">Any</html:option>
                  <c:forEach items="${extraClassFieldValues}" var="value">
                    <html:option value="${value}">
                      <c:out value="${value}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
              </span>
            </c:if>
          <c:if test="${empty keyFields[con]}">
            </li>
          </c:if>

          <%-- list constraint --%>

          <li>
            <span>
              &nbsp; <%-- for IE --%>
            </span>
            <span>
              <c:if test="${(!empty bagType) && (! empty constraintBags[con])}">
                <strong><fmt:message key="template.or"/></strong>
                <html:checkbox property="useBagConstraint(${index})" onclick="clickUseBag(${index})" disabled="${empty bags?'true':'false'}" />

                <fmt:message key="template.constraintobe"/>
                <html:select property="bagOp(${index})">
                  <c:forEach items="${bagOps}" var="bagOp">
                    <html:option value="${bagOp.key}">
                      <c:out value="${bagOp.value}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
                <fmt:message key="template.bag"/>
                <html:select property="bag(${index})" styleId="bagSelect">
                  <c:forEach items="${bags}" var="bag">
                    <html:option value="${bag.key}">
                      <c:out value="${bag.key}"/>
                    </html:option>
                  </c:forEach>
                </html:select>

                <c:if test="${empty bags}">
                  <div class="noBagsMessage">
                    <fmt:message key="template.nobags">
                      <fmt:param value="${bagType}"/>
                    </fmt:message>
                  </div>
                </c:if>

                <script type="text/javascript">
                  var selectedBagName = '${selectedBagNames[con]}';
                  if(selectedBagName){
                        initClickUseBag(${index});
                  }
                </script>
              </c:if>
            </span>

          </li>
        </c:forEach>
      </c:forEach>
    </ol>

<%-- edit/submit buttons --%>
<c:if test="${empty previewTemplate}">
    <br/>
     <table width="100%">
     <tr>
       <td>
          <html:hidden property="name"/>
          <html:hidden property="type"/>
          <html:hidden property="actionType" value="" styleId="actionType"/>
          <html:submit property="skipBuilder" styleId="showResultsButton"><fmt:message key="template.submitToResults"/></html:submit>
          <html:submit property="editQuery"><fmt:message key="template.submitToQuery"/></html:submit>
          <c:if test="${IS_SUPERUSER}">
            <html:submit property="editTemplate"><fmt:message key="template.submitToQueryEdit"/></html:submit>
          </c:if>
       </td>
      <td align="right"><html:link action="/exportTemplates?scope=all&amp;name=${templateQuery.name}"><img src="images/xml.png" title="Export this template to XML"/></html:link></td>
    </tr>
    </table>
</c:if>
</html:form>

<%-- embed link --%>
<c:if test="${empty previewTemplate}">
    <div style="font-style: italic;"><b>NEW:</b> <a href="javascript:forwardToLinks()">Embed</a> this query. <a href="http://intermine.org/wiki/TemplateWebService#a2.2Templatewebservice">Help</a></div>
</c:if>

<%-- login msg --%>
<c:if test="${!PROFILE.loggedIn}">
    <p><i><fmt:message key="template.notlogged"><fmt:param><im:login/></fmt:param></fmt:message></i></p>
</c:if>
</im:boxarea>
</div>
<!-- /template.jsp -->
