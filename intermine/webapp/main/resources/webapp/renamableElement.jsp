<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<tiles:useAttribute id="name" name="name"/>
<tiles:useAttribute id="type" name="type"/>
<tiles:useAttribute id="index" name="index"/>

<!-- renamableElement.jsp -->
<html:xhtml/>
<td align="left" colspan="" nowrap class="noRightBorder">
  <span id="form_${name}" style="display:none;">
    <input type="text" id="newName_${name}" name="newName_${name}" value="${name}" size="10" onkeypress="return noenter();" />
    <input type="button" name="rename" value="Rename" onclick="renameElement('${name}','${type}','${index}')"/>
  </span>
  <span id="name_${name}">
    <c:set var="nameForURL"/>
    <str:encodeUrl var="nameForURL">${name}</str:encodeUrl>
    <c:choose>
      <c:when test="${type == 'bag'}">
        <html:link action="/bagDetails?bagName=${nameForURL}">
          <c:out value="${name}"/>
        </html:link>
      </c:when>
      <c:otherwise>
        <c:out value="${name}"/>
      </c:otherwise>
    </c:choose>
  </span>
</td>

<td align="right" valign="middle" width="1">
  <a href="javascript:editName('${name}');">
    <img border="0" src="images/edit.gif" width="13" height="13" alt="rename"/>
  </a>
</td>
<!-- /renamableElement.jsp -->
