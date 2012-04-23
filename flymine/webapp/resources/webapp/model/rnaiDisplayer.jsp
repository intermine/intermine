<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<!-- rnaiDisplayer.jsp -->
<div id="rnai-displayer" class="basic-table">
<h3>RNAi</h3>

<c:choose>
  <c:when test="${!empty noRNAiMessage }">
    <p>${noRNAiMessage}</p>
  </c:when>
  <c:otherwise>
    <c:forEach items="${results}" var="parentEntry">
      <c:set var="score" value="${parentEntry.key}" />
      <c:set var="tableKey" value="${fn:toLowerCase(fn:replace(score, ' ', '-'))}" />
      <div class="switcher score" id="${tableKey}">
      <table style="margin-bottom:6px;">
        <thead>
          <tr><th colspan="2">${score}</th></tr>
        </thead>

        <c:choose>
        <c:when test="${!empty parentEntry.value}">
        <tbody>
        <tr>
          <c:forEach items="${parentEntry.value}" var="entry">
            <tr>
              <td>
                <c:set var="screen" value="${entry.key}" />
                <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${screen.id}">
                <c:out value="${screen.field}"/>
                </html:link>
              </td>
              <td>PubMed:
                <c:set var="pubmed" value="${entry.value}" />
                <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${pubmed.id}">
                <c:out value="${pubmed.field}"/>
                </html:link>
              </td>
            </tr>
          </c:forEach>
        </tr>
        </tbody>
        </table>
        </c:when>
        <c:otherwise>
        </table>
        <p class="smallnote" style="margin:-6px 0 0 6px;"><i>No results in this category.</i></p>
        </c:otherwise>
        </c:choose>
      </div>
    </c:forEach>

    <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=rnaiResults&amp;trail=${param.trail}" styleClass="link">
      Show all in a table &raquo;
    </html:link>

    <script type="text/javascript">
       jQuery('#rnai-displayer div.score').each(function(i) {
         var t = jQuery(this);
         if ((t.attr('id') in {'weak-hit':'', 'not-a-hit':'', 'not-screened':''}) && (t.find('table tbody tr').length > 0)) {
           t.find('table,p.smallnote').hide();
           jQuery('<a/>', {
             'class': 'link show',
               'html': function() {
                 return 'Show <strong>' + t.find('table thead tr th').text() + '</strong> RNAi in ' + t.find('table tbody tr').length + ' screens';
               },
               'click': function() {
                 jQuery(this).parent().find('table').show().parent().find('a.show').remove();
                 //jQuery(this).remove();
               }
           })
           .appendTo(t);
         }
       });
    </script>
  </c:otherwise>
</c:choose>
</div>
<!-- /rnaiDisplayer.jsp -->
