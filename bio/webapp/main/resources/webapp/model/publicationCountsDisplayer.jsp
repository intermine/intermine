<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- publicationCountsDisplayer.jsp -->
<div id="publication-counts-displayer" class="collection-table">


<div class="header">
<h3>Publications</h3>

<c:choose>
  <c:when test="${!empty noResults }">
    <p>${noResults}</p>
    </div>
  </c:when>
  <c:otherwise>
    <p>Total number of publications:  ${totalNumberOfPubs}</p>
    </div>

    <table>
      <thead>
        <tr>
            <th>Author</th>
            <th>Date</th>
            <th>Journal</th>
            <th>Title</th>
            <th>Number of ${type}s mentioned</th>
        </tr>
      </thead>
      <tbody>
      <c:forEach items="${results}" var="entry" varStatus="status">
        <c:set var="pub" value="${entry.key}" />
        <c:set var="total" value="${entry.value}" />
        <tr <c:if test="${status.count > 10}">style="display:none"</c:if>>
          <td><c:out value="${pub.firstAuthor}"/></td>
          <td><c:out value="${pub.year}"/></td>
          <td><c:out value="${pub.journal}"/></td>
          <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pub.id}"><c:out value="${pub.title}"/></html:link></td>
          <td><c:out value="${total}"/></td>
        </tr>
      </c:forEach>
      </tbody>
    </table>

    <div class="toggle" style="display:none;">
      <a class="less" style="float:right; display:none; margin-left:20px;"><span>Collapse</span></a>
      <a class="more" style="float:right;"><span>Show more rows</span></a>
    </div>

  <div class="show-in-table" style="display:none;">
    <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=publications&amp;trail=${param.trail}">
        Show all in a table &raquo;
      </html:link>
  </div>

    <script type="text/javascript">
    (function() {
      var t = jQuery('#publication-counts-displayer');
        var rows = t.find("table tbody tr");
        if (rows.length > 10) {
          t.find("div.toggle").show();

          t.find('div.toggle a.more').click(function(e) {
          t.find("div.show-in-table").show();
            t.find("table tbody tr:hidden").each(function(i) {
              if (i < 10) {
                jQuery(this).show();
              }
            });
            t.find("div.toggle a.less").show();
            if (t.find("table tbody tr:hidden").length == 0) {
              t.find('div.toggle a.more').hide();
            }
          });

          t.find('div.toggle a.less').click(function(e) {
            var that = this;
            rows.each(function(i) {
              if (i > 9) {
                jQuery(this).hide();
                jQuery(that).hide();
              }
            });
            t.find('div.toggle a.more').show();
            t.scrollTo('fast', 'swing', -20);
            t.find("div.show-in-table").hide();
          });
        }
    })();
    </script>
  </c:otherwise>
</c:choose>
</div>
<!-- /publicationCountsDisplayer.jsp -->
