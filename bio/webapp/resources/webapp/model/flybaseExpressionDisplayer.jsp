<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>


<div id="flybase-expression">

<c:choose>
<c:when test="${empty(flybaseResults)}">
<h3 class="goog gray">Expression by Stage (modENCODE)</h3>
<p>No expression data available for this gene.</p>
</c:when>
<c:otherwise>
<h3 class="goog">Expression by Stage (modENCODE)</h3>

<div class="wrap">
<div class="inside">
<div class="chart" id="flybase-expression-chart">
  <div class="loading">Loading the chart...</div>
</div>
</c:otherwise>
</c:choose>
</div>