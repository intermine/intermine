<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- friendlyMineLinkDisplayer.jsp -->

<c:set var="interMineObject" value="${object}"/>

<!--  provides FriendlyMines global object. -->
<script type="text/javascript" charset="utf-8" src="model/js/friendly-mines.js"></script>

<h3 class="goog">View homologues in other Mines:</h3>

<tiles:importAttribute />

<div id="friendlyMines">
  <c:forEach items="${mines}" var="mine">
    <div class="mine" id="partner_mine_${mine.name}">

      <span style="background: ${mine.bgcolor}; color: ${mine.frontcolor};">
          <c:out value="${mine.name}"/>
      </span>

      <!-- loading indicator here -->
      <div class="loading-indicator"></div>

      <!-- results (eventually) here -->
      <ul class="results"></ul>

      <!-- go get some data! -->
      <script type="text/javascript" charset="utf-8">
        var mine = {name: '${mine.name}', url: '${mine.url}'};
        var req = {origin: '${localMine.name}', domain: '${organisms}', identifiers: '${identifiers}'};
        FriendlyMines.getLinks('#partner_mine_${mine.name}', mine, req);
      </script>
    </div>
  </c:forEach>
</div>
<!-- /friendlyMineLinkDisplayer.jsp -->

