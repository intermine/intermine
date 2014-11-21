<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- This section is rendered with Ajax to improve responsiveness -->
<c:if test="${!empty linkManager.friendlyMines}">
<script type="text/javascript" charset="utf-8" src="js/other-mines-links.js"></script>
<h3 class="goog"><fmt:message key="othermines.title"/></h3>
<div id="friendlyMines">
  <c:forEach items="${mines}" var="mine">
    <div class="mine" id="partner_mine_${mine.name}">

      <span style="background:${mine.bgcolor};color:${mine.frontcolor};">
          <c:out value="${mine.name}"/>
      </span>

      <div class="loading-indicator"></div>
      <span class="apology" style="display:none">
          <fmt:message key="noResults.title"/>
      </div>

      <ul class="results"></ul>

      <script type="text/javascript" charset="utf-8">
        var mine = {name: '${mine.name}', url: '${mine.url}'};
        var req = {
          origin: '${localMine.name}',
          domain: '${object.organism.shortName}',
          identifiers: '${object.primaryIdentifier}'
        };
        OtherMines.getLinks('#partner_mine_${mine.name}', mine, req);
      </script>

    </div>
  </c:forEach>
</div>
</c:if>
