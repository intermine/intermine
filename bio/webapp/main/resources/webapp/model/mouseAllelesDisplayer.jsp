<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- mouseAllelesDisplayer.jsp -->

<div class="collection-of-collections" id="mouse-alleles">
  <style>
  #mouse-alleles span.size-1 { font-size:10px; }
  #mouse-alleles span.size-2 { font-size:14px; }
  #mouse-alleles span.size-3 { font-size:22px; }
  #mouse-alleles div.header h3 { border:0; }
  #mouse-alleles ul li { display:inline; }
  #mouse-alleles ul li:not(:last-child) { margin-right:10px; }
  #mouse-alleles ul li:not(.top) { display:none; }
  #mouse-alleles ul li a span { display:none; }
  #mouse-alleles div.toggle { margin:0 auto; margin-bottom:20px; text-align:center; }
  #mouse-alleles div.inline-list { margin:0; }
  #mouse-alleles-collection { margin-top:20px; }
  </style>

  <div class="header">
    <h3>Mouse Allele Phenotypes</h3>
    <c:choose>
      <c:when test="${isThisAMouser}">
        <p><img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?"> Most frequent phenotypes associated with the mouse alleles (MGI) by gene</p>
      </c:when>
      <c:otherwise>
        <p><img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?"> From the mouse orthologue(s), most frequent phenotypes associated with the mouse alleles (MGI) of this gene</p>
      </c:otherwise>
    </c:choose>
  </div>

  <c:choose>
    <c:when test="${not empty counts}">
    <c:forEach var="homologue" items="${counts}">
      <c:if test="${not homologue.value['isMouser']}">
        <div class="header"><h3>${homologue.key} Mouse Homologue with ${homologue.value['alleleCount']} alleles</h3></div>
      </c:if>
      <div class="inline-list">
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
      </div>
      <div class="toggle"><a class="more" title="Show more terms">Show more terms</a></div>
    </c:forEach>
    </c:when>
    <c:otherwise>
      <c:choose>
        <c:when test="${alleleCount != 1}">
          <p style="font-style:italic;">No data found for ${alleleCount} alleles</p>
        </c:when>
        <c:otherwise>
          <p style="font-style:italic;">No data found for 1 allele</p>
        </c:otherwise>
      </c:choose>
    </c:otherwise>
  </c:choose>
  <script type="text/javascript">
    jQuery("#mouse-alleles div.toggle a").click(function() {
      jQuery('#mouse-alleles ul li').css('display', 'inline');
      jQuery("#mouse-alleles div.toggle").remove();
    });

    jQuery("#mouse-alleles ul li a").each(function() {
      var span = jQuery(this).find('span');
      var xml = encodeURIComponent(span.html());
      jQuery(this).attr('href', jQuery(this).attr('href') + "?skipBuilder=true&method=xml&trail=%7Cquery&query=" + xml);
    });
  </script>
</div>

<c:if test="${collection != null}">
  <div id="mouse-alleles-collection" class="collection-table">
    <div class="header">
      <h3>Mouse Alleles </h3>
      <p><img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?"> Most frequent phenotypes associated with the mouse alleles (MGI) by gene</p>
    </div>

    <c:set var="inlineResultsTable" value="${collection}" />
    <tiles:insert page="/reportCollectionTable.jsp">
    <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
    <tiles:put name="object" beanName="reportObject.object" />
    <tiles:put name="fieldName" value="alleles" />
    </tiles:insert>
    <div class="toggle">
        <a class="more" style="float:right;"><span>Show more rows</span></a>
    </div>
    <div class="show-in-table">
      <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=alleles&amp;trail=${param.trail}">
        Show all in a table &raquo;
      </html:link>
    </div>
  </div>

  <script type="text/javascript">
  (function() {
     var bodyRows = jQuery("#mouse-alleles-collection.collection-table table tbody tr");
     if (bodyRows.length > 10) {
       bodyRows.each(function(i) {
         if (i > 9) {
           jQuery(this).hide();
         }
       });
       jQuery("#mouse-alleles-collection.collection-table div.toggle").show();
       jQuery('#mouse-alleles-collection.collection-table div.toggle a.more').click(function(e) {
         jQuery("#mouse-alleles-collection.collection-table table tbody tr:hidden").each(function(i) {
           if (i < 10) {
             jQuery(this).show();
           }
         });
         jQuery("#mouse-alleles-collection.collection-table div.toggle a.less").show();
         if (jQuery("#mouse-alleles-collection.collection-table table tbody tr:hidden").length == 0) {
           jQuery('#mouse-alleles-collection.collection-table div.toggle a.more').hide();
         }
       });
      }

      jQuery('#mouse-alleles-collection input.toggle-table').click(function() {
        jQuery('#mouse-alleles-collection.collection-table').toggle();
        if (jQuery('#mouse-alleles-collection.collection-table:visible')) {
          jQuery("#mouse-alleles-collection.collection-table").scrollTo('fast', 'swing', -20);
          jQuery(this).hide();
        }
      });
  })();
  </script>
</c:if>

<!-- /mouseAllelesDisplayer.jsp -->