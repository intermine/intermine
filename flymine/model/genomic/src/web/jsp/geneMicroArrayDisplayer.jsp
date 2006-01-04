<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

  
<c:forEach items="${experiments}" var="item">
  <p>
    <html:link  action="/chartRenderer?method=microarray&amp;gene=${object.identifier}&amp;experiment=${item.identifier}&amp;width=600&amp;height=120">
      <im:abbreviate value="${item.name}" length="65"/>
    </html:link><br/>
    <img src="<html:rewrite action="/chartRenderer?method=microarray&amp;gene=${object.identifier}&amp;experiment=${item.identifier}&amp;width=420&amp;height=60"/>" width="420" height="60"/>
  </p>
</c:forEach>

