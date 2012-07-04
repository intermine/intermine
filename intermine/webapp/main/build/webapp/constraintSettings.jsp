<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- constraintSettings.jsp -->


<html:xhtml/>
<div class="heading">
  Template constraint settings
</div>

<c:set var="editable" value="${constraint.editableInTemplate}"/>
<div style="body">
    <table border="0" width="100%">
      <tr>
        <td>
        Should users of the template query edit this constraint?
        </td>
      </tr>
      <tr>
        <td align="center">
          <fmt:message key="templateBuilder.editable"/>
          <input type="checkbox" name="editable" id="editable" <c:if test="${editable}">checked</c:if> 
             onClick="toggleConstraintDetails(this.checked);"/>
        </td>

      </tr>
    </table>
    
    <br />
    
    <c:set var="switchable" value="${constraint.switchable}"/>
    
    <c:set var="editableOptionsClass" value=""/>
    <c:if test="${editable}">
      <c:set var="editableOptionsClass" value="constraintEditableOptionsDisabled"/>
    </c:if>

    <div id="editableConstraintOptions" class="editableOptionsClass">
    <table border="0" width="100%">
       <tr>
        <td align="right" nowrap><fmt:message key="templateBuilder.label"/></td>
        <td><html:text property="templateLabel" styleId="templateLabel" size="20"
             value="${editable ? constraint.description : ''}" disabled="${editable ?'false':'true'}"/></td>
      </tr>
      
      <tr>
        <td><input type="radio" name="switchable" id="switchable" value="locked" <c:if test="${switchable=='locked'}">checked</c:if> 
        <c:if test="${!constraint.editableInTemplate}">disabled</c:if>/>
        </td>
        <td>Required - the user must supply a value</td>
      </tr>
      <tr>
       
        <td><input type="radio" name="switchable" id="switchable" value="on" <c:if test="${switchable=='on'}">checked</c:if> 
        <c:if test="${!constraint.editableInTemplate}">disabled</c:if>/>
        </td>
        <td>Optional: ON - optional and ON by default</td>
      </tr>
      <tr>
        <td><input type="radio" name="switchable" id="switchable" value="off" <c:if test="${switchable=='off'}">checked</c:if> 
        <c:if test="${!constraint.editableInTemplate}">disabled</c:if>
        />
        </td>
         <td>Optional: OFF - optional and OFF by default</td>
      </tr>
      <tr>
        <td>&nbsp;</td>
        <td>&nbsp;
        <c:if test="${constraint.editableInTemplate}">

        </c:if>
        </td>
      </tr>
      
    </table>
    </div>
  <html:submit property="template" style="float:right; margin-bottom:10px;">
            Update
  </html:submit>
</div>

<!-- /constraintSettings.jsp -->
