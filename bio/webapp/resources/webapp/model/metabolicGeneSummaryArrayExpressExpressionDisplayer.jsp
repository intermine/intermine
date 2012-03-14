<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!-- metabolicGeneSummaryArrayExpressExpressionDisplayer.jsp -->

<div id="arrayexpress-expression-displayer">

  <style>
    #arrayexpress-expression-displayer div.data { font-size:16px; font-weight:normal; }
    #arrayexpress-expression-displayer div.data span.regulation { font-weight:bold; }
    #arrayexpress-expression-displayer div.data span.regulation.up { color:#59BB14; }
    #arrayexpress-expression-displayer div.data span.regulation.down { color:#0000FF; }
    #arrayexpress-expression-displayer div.label span.title { display:block; margin:0 10px 10px 0; }
  </style>

  <c:forEach items="${field.value['data']}" var="entry" varStatus="rowCounter">
    <c:if test="${rowCounter.count == 2}">
      <div class="label">
        <span class="title">&lt;- Tissue Expression</span>
        <span class="title">Disease Expression -&gt;</span>
        <span class="description">${field.value['description']}</span>
      </div>
    </c:if>

    <div class="data">
       <span class="regulation up">&uArr;${entry.up}</span>
       <span class="regulation down">&dArr;${entry.down}</span>
    </div>
  </c:forEach>
</div>

<!-- /metabolicGeneSummaryArrayExpressExpressionDisplayer.jsp -->
