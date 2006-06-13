<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>



<tiles:useAttribute id="name" name="name"/>
<tiles:useAttribute id="type" name="type"/>

<!-- historyElementName.jsp -->
<html:xhtml/>

                <c:choose>
                  <c:when test="${param.action=='rename' && param.type==type && param.name==name}">
                  
                    <td align="left" colspan="2" nowrap>
                  
<script type="text/javascript">
<!--
window.onload = function() {
  document.getElementById("renameEntry").focus();
}
// -->
</script>
                    <input type="text" name="newName" value="${name}" size="10" id="renameEntry"/>
                    <input type="hidden" name="name" value="${name}"/>
                    <input type="hidden" name="type" value="${type}"/>
                    <input type="submit" name="rename" value="Rename"/>
                    
                    </td>
                    
                  </c:when>
                  <c:otherwise>
                    <td align="left" class="noRightBorder">
                    <html:link action="/modifyQueryChange?method=load&amp;name=${name}&type=${type}">
                      <c:out value="${name}"/>
                    </html:link>
                    </td>
                    <td align="right" valign="middle" width="1">
                    <html:link action="/history?action=rename&amp;name=${name}&type=${type}">
                      <img border="0" src="images/edit.gif" width="13" height="13" alt="rename"/>
                    </html:link>
                    </td>
                  </c:otherwise>
                </c:choose>
                
<!-- /historyElementName.jsp -->
