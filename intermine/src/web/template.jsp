<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<tiles:importAttribute/>

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

  //-->
</script>


<div class="body">
  <html:form action="/templateAction">
    <c:out value="${templateQuery.description}"/><br/><br/>
    <table border="0" class="templateForm">
      <c:set var="index" value="${0}"/>
      <c:forEach items="${templateQuery.nodes}" var="node">
        <c:forEach items="${constraints[node]}" var="con">
          <c:set var="index" value="${index+1}"/>
          <c:set var="validOps" value="${displayConstraints[con].validOps}"/>
          <c:set var="fixedOps" value="${displayConstraints[con].fixedOpIndices}"/>
          <c:set var="options" value="${displayConstraints[con].optionsList}"/>
          
          <script type="text/javascript">
            <!--
              fixedOps[${index-1}] = new Array();
              <c:forEach items="${fixedOps}" var="op" varStatus="oi">
                fixedOps[${index-1}][${oi.count}] = "<c:out value="${op}"/>";
              </c:forEach>
            //-->
          </script>
          
          <c:if test="${!empty con.description}">
            <tr>
              <td align="right" valign="top" rowspan="2">
                <c:out value="[${index}]"/>
              </td>
              <td colspan="3">
                <i><c:out value="${con.description}"/></i>
              </td>
            </tr>
          </c:if>
          <tr>
            <c:if test="${empty con.description}">
              <td align="right">
                <c:out value="[${index}]"/>
              </td>
            </c:if>
            <td>
              <c:out value="${names[con]}"/>
            </td>
            <td valign="top">
              <html:select property="attributeOps(${index})" onchange="updateConstraintForm(${index-1}, document.templateForm['attributeOps(${index})'], document.templateForm['attributeOptions(${index})'], document.templateForm['attributeValues(${index})'])">
                <c:forEach items="${validOps}" var="op">
                  <html:option value="${op.key}">
                    <c:out value="${op.value}"/>
                  </html:option>
                </c:forEach>
              </html:select>
            </td>
            <td valign="top" align="left">
              <span id="operandEditSpan${index-1}">
               <html:text property="attributeValues(${index})"/>
                <%-- might want to show up arrow --%>
                <c:if test="${!empty options}">
                  <im:vspacer width="5"/>
                  <img src="images/left-arrow.gif" alt="&lt;-" border="0" height="13" width="13"/>
                  <im:vspacer width="5"/>
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
              <script type="text/javascript">
                <!--
                /* setting options popup value to correct initial state. */
                if (document.templateForm["attributeOptions(${index})"] != null)
                {
                  document.templateForm["attributeOptions(${index})"].value = document.templateForm["attributeValues(${index})"].value;
                  updateConstraintForm(${index-1}, document.templateForm["attributeOps(${index})"], document.templateForm["attributeOptions(${index})"], document.templateForm["attributeValues(${index})"]);
                }
                //-->
              </script>
            </td>
          </tr>
        </c:forEach>
      </c:forEach>
    </table>
    <c:if test="${empty previewTemplate}">
      <br/>
      <html:submit property="skipBuilder"><fmt:message key="template.submitToResults"/></html:submit>
      <html:submit><fmt:message key="template.submitToQuery"/></html:submit>
    </c:if>
  </html:form>
</div>

