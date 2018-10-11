<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- pathwaysDisplayer.jsp -->
<div id="pathways-displayer" class="collection-table">
<h3>Pathways</h3>

<c:choose>
  <c:when test="${!empty noPathwayResults }">
    <p>${noPathwayResults}</p>
  </c:when>
  <c:otherwise>
    <table>
      <thead>
        <tr>
            <th>Pathway</th>
            <th>Number of Genes</th>
        </tr>
      </thead>
      <tbody>
      <c:forEach items="${pathways}" var="entry" varStatus="status">
        <c:set var="pathway" value="${entry.key}" />
        <c:set var="total" value="${entry.value}" />
        <tr <c:if test="${status.count > 10}">style="display:none"</c:if>>
          <td><html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pathway.id}"><c:out value="${pathway.name}"/></html:link></td>
          <td><html:link action="/collectionDetails?id=${pathway.id}&amp;field=genes"><c:out value="${total}"/></html:link></td>
        </tr>
      </c:forEach>
      </tbody>
    </table>

    <div class="toggle" style="display:none;">
      <a class="less" style="float:right; display:none; margin-left:20px;"><span>Collapse</span></a>
      <a class="more" style="float:right;"><span>Show more rows</span></a>
    </div>

  <div class="show-in-table" style="display:none;">
    <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=pathways&amp;trail=${param.trail}">
        Show all in a table &raquo;
      </html:link>
  </div>

    <script type="text/javascript">
    (function() {
      var t = jQuery('#pathways-displayer');
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
<!-- /pathwaysDisplayer.jsp -->
