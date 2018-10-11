<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!-- metabolicGeneSummaryArrayExpressExpressionDisplayer.jsp -->

<div id="arrayexpress-expression-displayer">

  <style>
    #arrayexpress-expression-displayer div.regulation { font-size:20px; font-weight:bold; }
    #arrayexpress-expression-displayer div.regulation .up { color:#59BB14; }
    #arrayexpress-expression-displayer div.regulation .down { color:#0000FF; }
    #arrayexpress-expression-displayer span.title.main { text-decoration:underline; font-size:15px; display:inline-block; font-weight:bold; text-align:center; margin:0 6px; }
    #arrayexpress-expression-displayer div.box { display:inline-block; }
    #arrayexpress-expression-displayer div.box .title { background:transparent; }
  </style>

  <c:forEach items="${field.value['data']}" var="entry" varStatus="rowCounter">
    <c:choose>
      <c:when test="${rowCounter.count == 2}">
        <span class="title main">Genes<br/>Expression</span>
        <div class="box">
          <span class="title">Disease</span>
          <div class="regulation">
            <span class="up">&uArr;</span>${entry.up}
            <span class="down">&dArr;</span>${entry.down}
          </div>
        </div>
      </c:when>
      <c:otherwise>
        <div class="box">
          <span class="title">Tissue</span>
          <div class="regulation">
            <span class="up">&uArr;</span>${entry.up}
            <span class="down">&dArr;</span>${entry.down}
          </div>
        </div>
      </c:otherwise>
    </c:choose>
  </c:forEach>
</div>

<!-- /metabolicGeneSummaryArrayExpressExpressionDisplayer.jsp -->
