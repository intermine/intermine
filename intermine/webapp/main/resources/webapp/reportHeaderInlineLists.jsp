<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<tiles:importAttribute name="object" ignore="false" />

<script type="text/javascript">
  function showMoreInlineList(listDiv) {
    jQuery(listDiv + ' ul li').each(function(index) {
      if (!jQuery(this).is(":visible")) {
        jQuery(this).show();
      }
    });
    jQuery(listDiv + ' ul li.show-more').hide();
  }

  <%-- give some leeway of 20 chars as "Show more" takes up some space too --%>
  function applyShowMoreInlineList(listDiv, listLength) {
    <%-- traverse all elements for this list --%>
    jQuery(listDiv + ' ul li').each(function(index) {
      <%-- substract elements length --%>
      listLength -= jQuery(this).text().length;
      <%-- hide the further elements if we broke through the limit --%>
      if (listLength <= 0) {
        jQuery(this).hide();
      }
    });
    <%-- show link to show more? --%>
    if (listLength <= 0) {
      jQuery('<li/>', {
          'class': 'show-more',
          'html': jQuery('<a/>', {
              'href': '#',
              'title': 'Show more items',
              'text': 'Show more',
              'click': function(e) {
                showMoreInlineList(listDiv);
                e.preventDefault();
              }
          })
      }).appendTo(listDiv + ' ul');
    }
  }
</script>

<c:forEach items="${object.headerInlineLists}" var="list" varStatus="outerStatus">
  <c:if test="${list.size > 0}">
    <div class="inline-list" id="header-inline-list-${outerStatus.count}">
      <a name="${list.prefix}" class="anchor"></a>
      <ul>
        <li><span class="name label">${list.prefix}:</span></li>
        <c:choose>
          <c:when test="${list.showLinksToObjects}">
            <c:forEach items="${list.items}" var="item" varStatus="status">
              <li>
                <a href="<c:out value="${WEB_PROPERTIES['path']}" />report.do?id=${item.id}"
                title="Show '${item.value}' detail">${item.value}</a><c:if test="${status.count < list.size}">, </c:if>
              </li>
            </c:forEach>
          </c:when>
          <c:otherwise>
            <c:forEach items="${list.items}" var="item" varStatus="status">
              <li>${item.value}<c:if test="${status.count < list.size}">, </c:if></li>
            </c:forEach>
          </c:otherwise>
        </c:choose>
      </ul>
    </div>

    <%-- give some leeway of 20 chars as "Show more" takes up some space too --%>
    <c:if test="${list.lineLength > 0 && list.length - 20 > list.lineLength}">
      <script type="text/javascript">
        applyShowMoreInlineList('#header-inline-list-${outerStatus.count}', ${list.lineLength});
      </script>
    </c:if>
  </c:if>
</c:forEach>