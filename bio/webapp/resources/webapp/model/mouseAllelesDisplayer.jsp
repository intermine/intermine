<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- mouseAllelesDisplayer.jsp -->

<div class="inline-list" id="mouse-alleles">
  <style>
  #mouse-alleles span.size-1 { font-size:10px; }
  #mouse-alleles span.size-2 { font-size:14px; }
  #mouse-alleles span.size-3 { font-size:22px; }
  #mouse-alleles div.header h3 { border:0; }
  #mouse-alleles ul li:not(:last-child) { margin-right:10px; }
  #mouse-alleles ul li:not(.top) { display:none; }
  #mouse-alleles ul li a span { display:none; }
  #mouse-alleles div.toggle { margin:0 auto; text-align:center; }
  </style>

  <h3>Mouse Alleles</h3>
  <c:if test="${not empty counts}">
  <c:forEach var="homologue" items="${counts}">
    <c:if test="${not homologue.value['isMouser']}">
      <div class="header"><h3>${homologue.key} Mouse Homologue</h3></div>
    </c:if>
    <ul>
    <c:forEach var="term" items="${homologue.value['terms']}">
      <li <c:if test="${term.value.top}">class="top"</c:if>>
      <span class="size-<c:choose>
        <c:when test="${term.value.count < 2}">1</c:when>
        <c:when test="${term.value.count < 5}">2</c:when>
        <c:otherwise>3</c:otherwise>
      </c:choose>"><html:link action="/loadQuery.do">
        <span>${term.value.url}</span> ${term.key}</html:link> (${term.value.count})</span>
      </li>
    </c:forEach>
    </ul>
    <div class="toggle"><a class="more" title="Show more terms">Show more terms</a></div>
  </c:forEach>
  </c:if>
  <script type="text/javascript">
    <%-- toggler --%>
    jQuery("#mouse-alleles div.toggle a").click(function() {
      jQuery('#mouse-alleles ul li').css('display', 'inline');
      jQuery("#mouse-alleles div.toggle").remove();
    });

    <%-- encode XML --%>
    jQuery("#mouse-alleles ul li a").each(function() {
      var span = jQuery(this).find('span');
      var xml = encodeURIComponent(span.html());
      jQuery(this).attr('href', jQuery(this).attr('href') + "?skipBuilder=true&method=xml&trail=%7Cquery&query=" + xml);
    });
  </script>
</div>

<!-- /mouseAllelesDisplayer.jsp -->