<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>

<tiles:useAttribute id="pathString" name="pathString"/>
<tiles:useAttribute id="description" name="description"/>

<c:set var="pathName" value="${fn:replace(pathString,'.','_')}"/>

<script type="text/javascript">
<!--//<![CDATA[
  function noenter() {
    return !(window.event && window.event.keyCode == 13);
  }
//]]>-->
</script>

<im:unqualify className="${pathString}" var="pathEnd"/>
<im:prefixSubstring str="${pathString}" outVar="pathPrefix" delimiter="."/>

<!-- viewElementDescription.jsp -->
<html:xhtml/>
<span id="form_${pathName}" style="display:none;">
  <input type="text" id="newName_${pathName}" name="newName_${pathname}"
         value="${description}" size="10" onkeypress="return noenter();" />
  <input type="button" name="change_description" value="Change"
         onclick="changeViewPathDescription('${pathName}')"/>
</span>
<span id="name_${pathName}">
  <span id="name_${pathName}_inner">
    <c:choose>
      <c:when test="${empty description}">
        <fmt:message key="view.noViewPathDescription"/>
      </c:when>
      <c:otherwise>
        <span class="viewPathDescription"><c:out value="${description}"/></span>
        &gt; ${pathEnd}
      </c:otherwise>
    </c:choose>
  </span>
  <a href="javascript:editName('${pathName}');" title="change description">
    <img border="0" src="images/edit.gif" width="13" height="13" alt="change description"/>
  </a>
</span>
<!-- /viewElementDescription.jsp -->
