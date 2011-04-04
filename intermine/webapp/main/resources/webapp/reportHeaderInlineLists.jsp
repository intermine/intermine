<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<tiles:importAttribute name="object" ignore="false" />

<c:forEach items="${object.headerInlineLists}" var="list" varStatus="outerStatus">
  <c:if test="${list.size > 0}">
    <div class="box grid_12 list" id="header-inline-list-${outerStatus.count}">
      <script type="text/javascript">
        var listLength = <c:out value="${list.lineLength}"/>;
      </script>
      <ul class="items">
        <li><span>${list.prefix}</span>:</li>
        <c:choose>
          <c:when test="${list.showLinksToObjects}">
            <c:forEach items="${list.items}" var="item" varStatus="status">
              <li>
                <a href="<c:out value="${WEB_PROPERTIES['path']}" />report.do?id=${item.id}" target="new"
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
    <div style="clear:both;">&nbsp;</div>
      <script type="text/javascript">
        function showMoreInlineList(numero) {
          jQuery('#header-inline-list-' + numero + ' ul.items li').each(function(index) {
            if (!jQuery(this).is(":visible")) {
              jQuery(this).show();
            }
          });
          jQuery('#header-inline-list-' + numero + ' ul.items li.show-more').hide();
        }

        // give some leeway of 20 chars as "Show more" takes up some space too
        if (listLength > 0 && ${list.length} - 20 > listLength) {
          // traverse all elements for this list
          jQuery('#header-inline-list-${outerStatus.count} ul.items li').each(function(index) {
            // substract elements length
            listLength -= jQuery(this).text().length;
            // hide the further elements if we broke through the limit
            if (listLength <= 0) {
              jQuery(this).hide();
            }
          });
          // show link to show more?
          if (listLength <= 0) {
            var linkElement = '<li class="show-more"><a href="#" onclick="showMoreInlineList(${outerStatus.count});return false;">Show more</a></li>'
            jQuery('#header-inline-list-${outerStatus.count} ul.items').append(linkElement);
          }
        }
      </script>
  </c:if>
</c:forEach>