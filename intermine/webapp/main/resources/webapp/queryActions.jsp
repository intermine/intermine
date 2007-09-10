<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<!-- queryActions.jsp -->
    <div class="heading">
      <fmt:message key="view.actions"/>
    </div>
    <div class="body">
        <p>
          <tiles:insert page="saveQuery.jsp"/>
        </p>
    </div>
<!-- /queryActions.jsp -->