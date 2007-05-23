<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>


<!-- objectDetailsAspectIcon.jsp -->

<html:xhtml/>

<%-- relies on 'aspect' being in the request scope --%>

<html:link action="/aspect?name=${aspect}">
  <img src="${ASPECTS[aspect].iconImage}" width="18" height="18" class="objectDetailsAspectIcon"/>
</html:link>
<!-- /objectDetailsAspectIcon.jsp -->
