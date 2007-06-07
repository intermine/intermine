<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<%--
mainConstraint.jsp

Inputs:
editingConstraintIndex the index of the constraint we are editing, or null for a new constraint
editingTemplateConstraint
editingNode the PathNode of the field we are constraining
displayConstraint a DisplayConstraint object
loopQueryPaths a list of PathNodes that are a compatible type to the current node
loopQueryOps ClassConstraint.VALID_OPS

Calculated:
constraint
validOps displayConstraint.validOps
fixedOps displayConstraint.fixedOpIndices
options displayConstraint.optionsList

--%>

<!-- mainConstraint.jsp -->

<html:xhtml/>

<html:form action="/mainAction" styleId="mainForm">

  <html:hidden property="path" value="${editingNode.pathString}"/>

  <c:if test="${editingConstraintIndex != null}">
    <html:hidden property="cindex" value="${editingConstraintIndex}"/>
  </c:if>

  <c:if test="${TEMPLATE_BUILD_STATE != null && (editingTemplateConstraint)}">
    <c:set var="constraint" value="${editingNode.constraints[editingConstraintIndex]}" scope="request"/>
    <tiles:insert page="constraintSettings.jsp"/>
  </c:if>

  <c:if test="${!editingTemplateConstraint}">

    <div class="heading">
      <fmt:message key="query.constrain"/><%--Constraint--%><im:manualLink section="manualPageQB.shtml#manualConstrainQB"/>
    </div>

    <div class="body">

      <c:if test="${editingConstraintIndex == null && fn:length(QUERY.allConstraints) > 0}">

        <h3><fmt:message key="query.andorHeading"/></h3><%--1. Choose a logical conjuction--%>

        <fmt:message key="query.andor"/><%--Select AND below to filter your query to include only records where all constraints are true.  Select OR to filter your query to include records where any of the other constraints are true or this constraint is true.--%>
        <br/>
        <br/>
        <div align="center">
          <html:radio property="operator" value="and"/>AND&nbsp;&nbsp;
          <html:radio property="operator" value="or"/>OR
        </div>
      </c:if>

      <h3><fmt:message key="query.constraintHeading"/></h3> <%--2. Choose a filter--%>

      <!-- ATTRIBUTE TOGGLE -->
      <h4>
        <a href="javascript:swapInputs('attribute');">
          <img id='attributeToggle' src="images/disclosed.gif"/>
          <fmt:message key="query.filterValue"/><%--Filter query results on this field having a specific value.--%>
        </a>
      </h4>

      <!-- field name -->
      <c:choose>
        <c:when test="${empty editingNode.fieldName}">
          <span class="type">
            <c:out value="${editingNode.pathString}"/>
          </span>
        </c:when>
        <c:otherwise>
          <span class="attributeField">
            <c:out value="${editingNode.fieldName}"/>
          </span>
        </c:otherwise>
      </c:choose>
      <c:choose>
        <c:when test="${editingNode.collection}">
          <fmt:message key="query.collection"><%--collection--%>
            <fmt:param value="${editingNode.type}"/>
          </fmt:message>
        </c:when>
        <c:otherwise>
          <%-- <c:out value="${editingNode.type}"/> --%>
        </c:otherwise>
      </c:choose>

      <c:set var="validOps" value="${displayConstraint.validOps}"/>
      <c:set var="fixedOps" value="${displayConstraint.fixedOpIndices}"/>
      <c:set var="options" value="${displayConstraint.optionsList}"/>

      <script type="text/javascript">
      <!--

       fixedOps = new Array();

       fixedOps[0] = new Array();
        <c:forEach items="${fixedOps}" var="op" varStatus="status">
          fixedOps[0][${status.count}] = "<c:out value="${op}"/>";
        </c:forEach>

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
       * Init attribute value with selected item and hide input box if
       * required
       **********************************************************/
      function initConstraintForm(index, attrOpElement, attrOptsElement, attrValElement)
      {
        if (attrOptsElement == null)
          return;

        var init = '${editingConstraintValue}';
        attrValElement.value = (init != '') ? init : attrOptsElement.value;

        updateConstraintForm(index, attrOpElement, attrOptsElement, attrValElement);
      }

      //-->
      </script>


      <!-- input or radio buttons -->
      <c:choose>
        <c:when test="${editingNode.attribute}">
          <table border="0" cellspacing="0" cellpadding="1" border="0" class="noborder" >
            <tr>
              <c:choose>
                <c:when test="${editingNode.type == 'Boolean'}">
                  <td valign="top">

                    <html:hidden property="attributeOp" styleId="attribute1" value="0" disabled="false" />
                    <html:radio property="attributeValue" styleId="attribute2" value="true" disabled="false" /><fmt:message key="query.constraint.true"/>
                    <html:radio property="attributeValue" styleId="attribute3" value="false" disabled="false" /><fmt:message key="query.constraint.false"/>
                    <html:radio property="attributeValue" styleId="attribute4" value="NULL" disabled="false" /><fmt:message key="query.constraint.null"/>

                  </td>
                </c:when>
                <c:otherwise>
                  <td valign="top">
                    <html:select property="attributeOp" styleId="attribute5" onchange="updateConstraintForm(0, this.form.attributeOp, this.form.attributeOptions, this.form.attributeValue)">
                      <c:forEach items="${validOps}" var="op">
                        <c:if test="${!(editingNode.type == 'String' && (op.value == '<=' || op.value == '>='))}">
                          <option value="${op.key}"
                                  <c:if test="${editingConstraintOperand == op.key}">
                                    selected
                                  </c:if>
                                  >
                            <im:displayableOpName opName="${op.value}"
                                                  valueType="${editingNode.type}"/>
                          </option>
                        </c:if>
                      </c:forEach>
                    </html:select>
                  </td>
                  <td valign="top" align="center">
                    <span id="operandEditSpan0">
                      <html:text property="attributeValue" styleId="attribute6" value="${editingConstraintValue}"/>
                      <%-- might want to show up arrow --%>
                      <c:if test="${!empty options}">
                        <br/><im:vspacer height="2"/>
                        <img src="images/up-arrow.gif" alt="^^^" border="0" height="13" width="13"/>
                        <im:vspacer height="2"/>
                      </c:if>
                    </span>
                    <c:if test="${!empty options}">
                      <html:select property="attributeOptions" styleId="attribute7" onchange="this.form.attributeValue.value=this.value;">
                        <c:forEach items="${options}" var="option">
                          <option value="${option}"
                                  <c:if test="${editingConstraintValue == option}">
                                    selected
                                  </c:if>
                                  >
                            <c:out value="${option}"/>
                          </option>
                        </c:forEach>
                      </html:select>
                    </c:if>
                  </td>
                </c:otherwise>
              </c:choose>
              <td valign="top">&nbsp;
                <html:submit property="attribute"  styleId="attributeSubmit" disabled="false">
                  <fmt:message key="query.submitConstraint"/><%--Add to query--%>
                </html:submit>
              </td>
            </tr>
            <c:if test="${editingNode.type == 'String'}">
              <tr>
                <td colspan="3">
                  <span class="smallnote">
                    <fmt:message key="query.filterValue.wildcardHint"/>
                  </span>
                </td>
              </tr>
            </c:if>
          </table>
        </c:when>
        <c:otherwise>
        
          <!-- lookup constraint -->
          <c:if test="${!empty keyFields}">
            <p style="text-align: left;">
              <fmt:message key="query.lookupConstraintLabel"/><%--Search for:--%>
              <html:hidden property="attributeOp" styleId="attribute1" value="18" disabled="false" />
              <html:text property="attributeValue" styleId="attribute2" value="${editingConstraintValue}"/>

              <html:submit property="attribute" styleId="attributeSubmit" disabled="false" >
                <fmt:message key="query.submitConstraint"/><%--Add to query--%>
              </html:submit>
            </p>
            <p style="text-align: left;">
              <span class="smallnote">
                <fmt:message key="query.lookupConstraintHelp"><%--This will search...--%>
                  <fmt:param value="${keyFields}"/>
                </fmt:message>
              </span>
            </p>
          </c:if>
          
          <c:if test="${editingNode.indentation != 0 && !empty SUBCLASSES[editingNode.type]}">


            <!-- SUBCLASS -->

            <h5><fmt:message key="query.or"/></h5>

            <h4>
              <a href="javascript:swapInputs('subclass');">
                <img id='subclassToggle' src="images/undisclosed.gif"/>
                <fmt:message key="query.filterSubclass"/><%--Filter query results based on this field being a member of a specific class of objects.--%>
              </a>
            </h4>

            <p style="text-align: left;">
              <fmt:message key="query.subclassConstraint"/><%--Constraint to be subtype:--%>
              <html:select property="subclassValue" styleId="subclass1" disabled="true">
                <c:forEach items="${SUBCLASSES[editingNode.type]}" var="subclass">
                  <html:option value="${subclass}">
                    <c:out value="${subclass}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:submit property="subclass" styleId="subclassSubmit" disabled="true">
                <fmt:message key="query.submitConstraint"/><%--Add to query--%>
              </html:submit>
            </p>


          </c:if>
          <c:if test="${!empty loopQueryPaths && !empty loopQueryOps}">

            <!-- QUERY LOOP -->
            <h5><fmt:message key="query.or"/></h5>

            <h4>
              <a href="javascript:swapInputs('loopQuery');">
                <img id='loopQueryToggle' src="images/undisclosed.gif"/>
                <fmt:message key="query.filterLoopQuery"/><%--Filter query results on the query loop.--%>
              </a>
            </h4>

            <p style="text-align: left;">
              <fmt:message key="query.loopQueryConstraint"/><%--Constraint to another field:--%>
              <html:select property="loopQueryOp" styleId="loopQuery1" disabled="true">
                <c:forEach items="${loopQueryOps}" var="loopOp">
                  <html:option value="${loopOp.key}">
                    <c:out value="${loopOp.value}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:select property="loopQueryValue" styleId="loopQuery2" disabled="true">
                <c:forEach items="${loopQueryPaths}" var="loopPath">
                  <html:option value="${loopPath}">
                    <c:out value="${fn:replace(loopPath, '.', ' > ')}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:submit property="loop" styleId="loopQuerySubmit" disabled="true" >
                <fmt:message key="query.submitConstraint"/><%--Add to query--%>
              </html:submit>
            </p>
          </c:if>
        </c:otherwise>
      </c:choose>


      <c:if test="${!empty bags}">

        <!-- BAGS -->
        <h5><fmt:message key="query.or"/></h5>

        <h4>
          <a href="javascript:swapInputs('bag');">
            <img id='bagToggle' src="images/undisclosed.gif"/>
            <fmt:message key="query.filterBag"/><%--Filter query results on the contents of your bag--%>
          </a>
        </h4>

        <p style="text-align: left;">
          <fmt:message key="query.bagConstraint"/><%--Contained in bag:--%>
          <html:select property="bagOp" styleId="bag1" disabled="true">
            <c:forEach items="${bagOps}" var="bagOp">
              <html:option value="${bagOp.key}">
                <c:out value="${bagOp.value}"/>
              </html:option>
            </c:forEach>
          </html:select>
          <html:select property="bagValue" styleId="bag2" disabled="true">
            <c:forEach items="${bags}" var="bag">
              <html:option value="${bag.key}">
                <c:out value="${bag.key}"/>
              </html:option>
            </c:forEach>
          </html:select>
          <html:submit property="bag"  styleId="bagSubmit" disabled="true">
            <fmt:message key="query.submitConstraint"/><%--Add to query--%>
          </html:submit>
        </p>
      </c:if>


      <c:if test="${!editingNode.collection && !editingNode.reference &&
                  !empty editingNode.parent && editingNode.type != 'boolean'}">


        <!-- NULL OR NOT -->
        <h5><fmt:message key="query.or"/></h5>

        <h4>
          <a href="javascript:swapInputs('empty');">
            <img id='emptyToggle' src="images/undisclosed.gif"/>
            <fmt:message key="query.filterEmpty"/><%--Filter query results on this field having any value or not.--%>
          </a>
        </h4>


        <p style="text-align: left;">
          <html:radio property="nullConstraint" styleId="empty1" value="NULL" disabled="true" /><fmt:message key="query.constraint.null"/>
          <html:radio property="nullConstraint" styleId="empty2" value="NotNULL" disabled="true" /><fmt:message key="query.constraint.notnull"/>
          &nbsp;
          <html:submit property="nullnotnull" styleId="emptySubmit" disabled="true">
            <fmt:message key="query.submitConstraint"/><%--Add to query--%>
          </html:submit>
        </p>
      </c:if>

    </div>

    <script type="text/javascript">
    <!--
    initConstraintForm(0,
        $('mainForm').attributeOp,
        $('mainForm').attributeOptions,
        $('mainForm').attributeValue);
    //-->
    </script>

    </div>
    <div class="body" style="text-align:right">
      <span id="cancelButton"></span>
      <script language="JavaScript">
      <!--
        <c:if test="${param.deletePath}">
          <c:set var="deletePath" value="${param.deletePath}"/>
        </c:if>
        <c:choose>
          <c:when test="${!empty deletePath}">
            $('cancelButton').innerHTML='<input type="button" onclick="window.location.href=\'<html:rewrite action="/mainChange?method=removeNode&amp;path=${deletePath}"/>\'" value="<fmt:message key="query.cancelConstraint"/>"/>';
          </c:when>
          <c:otherwise>
            $('cancelButton').innerHTML='<input type="button" onclick="window.location.href=\'<html:rewrite action="/query"/>?cancel\'" value="<fmt:message key="query.cancelConstraint"/>"/>';
          </c:otherwise>
        </c:choose>
      //-->
      </script>
    </div>

  </c:if>

</html:form>


<!-- /mainConstraint.jsp -->

