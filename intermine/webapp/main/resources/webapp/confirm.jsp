<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- confirm.jsp -->
<html:xhtml/>

<div align="center" class="body">
  ${message}<p>
  <table border=0>
    <tr>
      <td>
         <input type="button" value="<fmt:message key="confirm.cancel"/>" onclick="window.location.href='<html:rewrite action="${cancelAction}"/>'"> 
      </td>
      <td>
         <input type="button" value="<fmt:message key="confirm.ok"/>" onclick="window.location.href='<html:rewrite action="${confirmAction}"/>'"> 
      </td>
    </tr>
  </table>
</div>
<!-- /confirm.jsp -->
