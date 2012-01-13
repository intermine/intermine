<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<!-- metabolicGeneSummaryArrayExpressExpressionDiseasesDisplayer.jsp -->

<div id="arrayexpress-expression-diseases-displayer">

  <style>
  #arrayexpress-expression-diseases-displayer div.data { font-size:12px; font-weight:normal; }
  #arrayexpress-expression-diseases-displayer div.data ul li { line-height:14px; }
  #arrayexpress-expression-diseases-displayer div.data span.regulation { display:block; float:left;
    width:10px; height:10px; border:1px solid #000; margin-right:5px; }
  #arrayexpress-expression-diseases-displayer div.data span.regulation.up { background:#59BB14; }
  #arrayexpress-expression-diseases-displayer div.data span.regulation.down { background:#0000FF; }
  </style>

  <div class="data">
    <c:choose>
      <c:when test="${not empty field.value['data']}">
        <ul>
        <c:forEach var="disease" items="${field.value['data']}">
          <li title="${fn:toLowerCase(disease.value)}regulated">
            <span class="regulation ${fn:toLowerCase(disease.value)}"></span>${disease.key}
          </li>
        </c:forEach>
        </ul>
      </c:when>
      <c:otherwise>
        No highly expressed diseases.
      </c:otherwise>
    </c:choose>
  </div>
  <div class="label">
    <span class="title">${field.key}</span>
    <span class="description">${field.value['description']}</span>
  </div>
</div>

<!-- /metabolicGeneSummaryArrayExpressExpressionDiseasesDisplayer.jsp -->