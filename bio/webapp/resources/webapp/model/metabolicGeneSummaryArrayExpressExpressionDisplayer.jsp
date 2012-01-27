<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!-- metabolicGeneSummaryArrayExpressExpressionDisplayer.jsp -->

<div id="arrayexpress-expression-displayer">

  <style>
    #arrayexpress-expression-displayer div.data { font-size:12px; font-weight:normal; }
    #arrayexpress-expression-displayer div.data ul li { line-height:14px; }
    #arrayexpress-expression-displayer div.data span.regulation { display:block; float:left;
      width:10px; height:10px; border:1px solid #000; margin-right:5px; }
    #arrayexpress-expression-displayer div.data span.regulation.up { background:#59BB14; }
    #arrayexpress-expression-displayer div.data span.regulation.down { background:#0000FF; }
    #arrayexpress-expression-displayer div.label span.title { display:block; margin-right:10px; }
  </style>

  <c:forEach items="${field.value['data']}" var="entry" varStatus="rowCounter">
    <c:if test="${rowCounter.count == 2}">
      <div class="label">
        <span class="title">&lt;- Tissues Expression</span>
        <span class="title">Diseases Expression -&gt;</span>
        <span class="description">${field.value['description']}</span>
      </div>
    </c:if>

    <div class="data">
      <c:choose>
        <c:when test="${not empty entry}">
          <ul>
          <c:forEach var="thingie" items="${entry}">
            <li title="${fn:toLowerCase(thingie.value)}regulated">
              <span class="regulation ${fn:toLowerCase(thingie.value)}"></span>${thingie.key}
            </li>
          </c:forEach>
          </ul>
        </c:when>
        <c:otherwise>
          No high differential expression (p < 1e-20).
        </c:otherwise>
      </c:choose>
    </div>
  </c:forEach>
</div>

<!-- /metabolicGeneSummaryArrayExpressExpressionDisplayer.jsp -->