<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- template.jsp -->


<tiles:importAttribute/>
<html:xhtml/>
<link rel="stylesheet" type="text/css" href="css/template.css"/>
<script type="text/javascript">
  <!--

  var fixedOps = new Array();

  /***********************************************************
   * Called when user chooses a constraint operator. If the
   * user picks an operator contained in fixedOptionsOps then
   * the input box is hidden and the user can only choose
   **********************************************************/
  function updateConstraintForm(index, attrOpElement, attrOptsElement, attrValElement)
  {
    if (attrOptsElement == null)
      return;

    for (var i=0 ; i<fixedOps[index].length ; i++)
    {
      if (attrOpElement.value == fixedOps[index][i])
      {
        document.getElementById("operandEditSpan" + index).style.display = "none";
        attrValElement.value = attrOptsElement.value; // constrain value
        return;
      }
    }

    document.getElementById("operandEditSpan" + index).style.display = "";
  }

  /***********************************************************
   * Use bag checkbox has been clicked.
   **********************************************************/
  function clickUseBag(index)
  {
    var useBag = document.templateForm["useBagConstraint("+index+")"].checked;

    document.templateForm["attributeOps("+index+")"].disabled=useBag;
    if (document.templateForm["attributeOptions("+index+")"])
      document.templateForm["attributeOptions("+index+")"].disabled=useBag;
    document.templateForm["attributeValues("+index+")"].disabled=useBag;
    document.templateForm["bag("+index+")"].disabled=!useBag;
    document.templateForm["bagOp("+index+")"].disabled=!useBag;
  }

  function initClickUseBag(index)
  {
    if(selectedBagName){
      document.templateForm["bag("+index+")"].value=selectedBagName;
      document.templateForm["useBagConstraint("+index+")"].checked = true;
    }
    clickUseBag(index);
  }

  /***********************************************************
   * Init attribute value with selected item and hide input box if
   * required
   **********************************************************/
  /*function initConstraintForm(index, attrOpElement, attrOptsElement, attrValElement)
  {
    if (attrOptsElement == null)
      return;

    attrValElement.value = attrOptsElement.value;
    updateConstraintForm(index, attrOpElement, attrOptsElement, attrValElement);
  }*/

  /***************Handle display of help windows******************************/
  var helpMsgArray = new Array();
  var ourDate;
  function displayHelpMsg(e, id) {
      // IE is retarded and doesn't pass the event object
          if (e == null)
              e = window.event; 

          // IE uses srcElement, others use target
          var target = e.target != null ? e.target : e.srcElement;
          // grab the clicked element's position
          _clientX = e.clientX;
          _clientY = e.clientY;
          
          // grab the clicked element's position
           _offsetX = ExtractNumber(target.style.left);
           _offsetY = ExtractNumber(target.style.top);
          
          $(id).style.left = (_clientX + _offsetX) + "px";
          $(id).style.top = (_clientY + _offsetY) + "px";
          Effect.Appear(id, { duration: 0.30 });
          helpMsgArray[helpMsgArray.length] = id;
          ourDate = new Date().getTime();
  }
  document.body.onclick = function clearHelpMsg() {
      newDate = new Date().getTime();
      if(newDate > (ourDate + 100)){
      for(var i=0;i<helpMsgArray.length;i++) {
          Effect.Fade(helpMsgArray[i], { duration: 0.30 });
      }
      }
  }
  function ExtractNumber(value)
  {
      var n = parseInt(value);

      return n == null || isNaN(n) ? 0 : n;
  }



  //-->
</script>

<tiles:get name="objectTrail.tile"/>
<div class="body" align="center">
<im:boxarea stylename="plainbox" fixedWidth="60%">
  <html:form action="/templateAction">
    <h2 class="templateTitle">
    <c:set var="templateTitle" value="${fn:replace(templateQuery.title,'-->','&nbsp;<img src=\"images/tmpl_arrow.png\" style=\"vertical-align:middle\">&nbsp;')}" />
	${templateTitle}
    <tiles:insert name="setFavourite.tile">
        <tiles:put name="name" value="${templateQuery.name}"/>
        <tiles:put name="type" value="template"/>
    </tiles:insert></h2>
    <div class="templateDescription">${templateQuery.description}</div>
    <ol class="templateForm">
      <c:set var="index" value="${0}"/>
      <c:forEach items="${templateQuery.editableNodes}" var="node">
        <c:forEach items="${constraints[node]}" var="con">
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
            <li class="firstLine">
                <span><c:out value="[${index}]"/></span>
                <i><c:out value="${con.description}"/></i>
            </li>
          </c:if>
          <li>

            <script type="text/javascript">
              <!--
                  fixedOps[${index-1}] = new Array();
                  <c:forEach items="${fixedOps}" var="op" varStatus="oi">
                    fixedOps[${index-1}][${oi.count}] = "<c:out value="${op}"/>";
                  </c:forEach>
                  //-->
            </script>

            <c:if test="${empty con.description}">
              <span>
                <c:out value="[${index}]"/>
              </span>
            </c:if>
            <label>
              <c:out value="${names[con]}"/>:
            </label>
            <c:choose>
              <c:when test="${fn:length(validOps) == 1}">
                <%--<fmt:message key="query.lookupConstraintLabel"/>Search for:--%>
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
            <span nowrap>
              <span id="operandEditSpan${index-1}">
                <html:text property="attributeValues(${index})"/>
                <c:if test="${!empty keyFields[con]}">
                  <span onMouseDown="displayHelpMsg(event,'lookupHelp')" style="cursor:pointer">?</span>
                  <div class="smallnote helpnote" id="lookupHelp" style="display:none">
                    <fmt:message key="query.lookupConstraintHelp"><%--This will search...--%>
                      <fmt:param value="${keyFields[con]}"/>
                    </fmt:message>
                  </div>
                </c:if>
                <%-- might want to show up arrow --%>
                <c:if test="${!empty options}">
                  <img src="images/left-arrow.gif" title="&lt;-" border="0" height="13" width="13"/>
                </c:if>
              </span>
              <c:if test="${!empty options}">
                <select name="attributeOptions(${index})" onchange="this.form['attributeValues(${index})'].value=this.value;">
                  <c:forEach items="${options}" var="option">
                    <option value="${option}">
                      <c:out value="${option}"/>
                    </option>
                  </c:forEach>
                </select>
              </c:if>
            </span>
          <c:if test="${haveExtraConstraint[con]}">
          <c:if test="${empty keyFields[con]}">
            </li>
            <li>
          </c:if>
              <span valign="top" colspan="4">
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
          <li>
            <span>
              &nbsp; <%-- for IE --%>
            </span>
            <span valign="top" colspan="4">
              <c:if test="${(!empty bagType) && (! empty constraintBags[con])}">
                <html:checkbox property="useBagConstraint(${index})" onclick="clickUseBag(${index})" disabled="${empty bags?'true':'false'}" />

                <fmt:message key="template.or"/>
                <fmt:message key="template.constraintobe"/>
                <html:select property="bagOp(${index})">
                  <c:forEach items="${bagOps}" var="bagOp">
                    <html:option value="${bagOp.key}">
                      <c:out value="${bagOp.value}"/>
                    </html:option>
                  </c:forEach>
                </html:select>
                <fmt:message key="template.bag"/>
                <html:select property="bag(${index})">
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
              <script type="text/javascript">

                <!--
                /* setting options popup value to correct initial state. */
                if (document.templateForm["attributeOptions(${index})"] != null)
                {
                  var select = document.templateForm["attributeOptions(${index})"];
                  var value = document.templateForm["attributeValues(${index})"].value;
                  var set = false;
                  for (i=0 ; i<select.options.length ; i++)
                  {
                    if (select.options[i].value == value)
                    {
                      select.selectedIndex = i;
                      set = true;
                      break;
                    }
                  }
                  if (!set)
                  {
                    document.templateForm["attributeValues(${index})"].value = select.value;
                  }
                  updateConstraintForm(${index-1}, document.templateForm["attributeOps(${index})"],
                                       document.templateForm["attributeOptions(${index})"],
                                       document.templateForm["attributeValues(${index})"]);
                }
                //-->
              </script>
            </span>

          </li>
        </c:forEach>
      </c:forEach>
    </ol>

    <c:if test="${empty previewTemplate}">
      <br/>
      <html:hidden property="templateName"/>
      <html:hidden property="templateType"/>
      <html:submit property="skipBuilder"><fmt:message key="template.submitToResults"/></html:submit>
      <html:submit><fmt:message key="template.submitToQuery"/></html:submit>
      <c:if test="${IS_SUPERUSER}">
        <html:submit property="editTemplate"><fmt:message key="template.submitToQueryEdit"/></html:submit>
      </c:if>
    </c:if>
  </html:form>
  <c:if test="${empty PROFILE_MANAGER || empty PROFILE.username}">
    <p>
      <i>
        <fmt:message key="template.notlogged">
          <fmt:param>
            <im:login/>
          </fmt:param>
        </fmt:message>
      </i>
    </p>
  </c:if>
</im:boxarea>
</div>
