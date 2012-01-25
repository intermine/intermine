<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!-- metabolicGeneSummaryArrayExpressExpressionTissuesDisplayer.jsp -->

<div id="arrayexpress-expression-tissues-displayer">

  <style>
  #arrayexpress-expression-tissues-displayer div.data { font-size:12px; font-weight:normal; }
  #arrayexpress-expression-tissues-displayer div.data ul li { line-height:14px; }
  #arrayexpress-expression-tissues-displayer div.data span.regulation { display:block; float:left;
    width:10px; height:10px; border:1px solid #000; margin-right:5px; }
  #arrayexpress-expression-tissues-displayer div.data span.regulation.up { background:#59BB14; }
  #arrayexpress-expression-tissues-displayer div.data span.regulation.down { background:#0000FF; }
  </style>

  <div class="data">
    <c:choose>
      <c:when test="${not empty field.value['data']}">
        <ul>
        <c:forEach var="tissue" items="${field.value['data']}">
          <li title="${fn:toLowerCase(tissue.value)}regulated">
            <span class="regulation ${fn:toLowerCase(tissue.value)}"></span>${tissue.key}
          </li>
        </c:forEach>
        </ul>
      </c:when>
      <c:otherwise>
        No high differential expression (p < 1e-20).
      </c:otherwise>
    </c:choose>
  </div>
  <div class="label">
    <span class="title">${field.key}</span>
    <span class="description">${field.value['description']}</span>
  </div>
</div>

<!-- /metabolicGeneSummaryArrayExpressExpressionTissuesDisplayer.jsp -->