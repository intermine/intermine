<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

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
      <fmt:message key="query.constrain"/><im:manualLink section="manualPageQB.shtml#manualConstrainQB"/>
    </div>

    <div class="body">

      <c:if test="${editingConstraintIndex == null && fn:length(QUERY.allConstraints) > 0}">

        <h3><fmt:message key="query.andorHeading"/></h3>

        <fmt:message key="query.andor"/>
        <br/>
        <br/>
        <div align="center">
          <html:radio property="operator" value="and"/>AND&nbsp;&nbsp;
          <html:radio property="operator" value="or"/>OR
        </div>
      </c:if>

      <h3><fmt:message key="query.constraintHeading"/></h3>

      <!-- ATTRIBUTE TOGGLE -->
      <h4>
        <a href="javascript:swapInputs('attribute');">
          <img id='attributeToggle' src="images/disclosed.gif"/>
          <fmt:message key="query.filterValue"/>
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
          <fmt:message key="query.collection">
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
                        <option value="${op.key}"
                                <c:if test="${editingConstraintOperand == op.key}">
                                  selected
                                </c:if>
                                >
                          <c:out value="${op.value}"/>
                        </option>
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
                  <fmt:message key="query.submitConstraint"/>
                </html:submit>
              </td>
            </tr>
          </table>
        </c:when>
        <c:otherwise>


          <c:if test="${editingNode.indentation != 0 && !empty SUBCLASSES[editingNode.type]}">


            <!-- SUBCLASS -->

            <h5><fmt:message key="query.or"/></h5>

            <h4>
              <a href="javascript:swapInputs('subclass');">
                <img id='subclassToggle' src="images/undisclosed.gif"/>
                <fmt:message key="query.filterSubclass"/>
              </a>
            </h4>

            <p style="text-align: left;">
              <fmt:message key="query.subclassConstraint"/>
              <html:select property="subclassValue" styleId="subclass1" disabled="true">
                <c:forEach items="${SUBCLASSES[editingNode.type]}" var="subclass">
                  <html:option value="${subclass}">
                    <c:out value="${subclass}"/>
                  </html:option>
                </c:forEach>
              </html:select>
              <html:submit property="subclass" styleId="subclassSubmit" disabled="true">
                <fmt:message key="query.submitConstraint"/>
              </html:submit>
            </p>


          </c:if>
          <c:if test="${!empty loopQueryPaths && !empty loopQueryOps}">

            <!-- QUERY LOOP -->
            <h5><fmt:message key="query.or"/></h5>

            <h4>
              <a href="javascript:swapInputs('loopQuery');">
                <img id='loopQueryToggle' src="images/undisclosed.gif"/>
                <fmt:message key="query.filterLoopQuery"/>
              </a>
            </h4>

            <p style="text-align: left;">
              <fmt:message key="query.loopQueryConstraint"/>
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
                <fmt:message key="query.submitConstraint"/>
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
            <fmt:message key="query.filterBag"/>
          </a>
        </h4>

        <p style="text-align: left;">
          <fmt:message key="query.bagConstraint"/>
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
            <fmt:message key="query.submitConstraint"/>
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
            <fmt:message key="query.filterEmpty"/>
          </a>
        </h4>


        <p style="text-align: left;">
          <html:radio property="nullConstraint" styleId="empty1" value="NULL" disabled="true" /><fmt:message key="query.constraint.null"/>
          <html:radio property="nullConstraint" styleId="empty2" value="NotNULL" disabled="true" /><fmt:message key="query.constraint.notnull"/>
          &nbsp;
          <html:submit property="nullnotnull" styleId="emptySubmit" disabled="true">
            <fmt:message key="query.submitConstraint"/>
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

