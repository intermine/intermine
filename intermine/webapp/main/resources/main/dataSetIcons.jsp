
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dataSetIcons -->

<div class="dataSetIcons">
  <table border="0" width="100">
    <tr valign="top">
      <td class="dsIconsElement" width="50%">
        <c:forEach var="entry" items="${DATASETS}" varStatus="status">
          <c:set var="set" value="${entry.value}"/>
          
          <table border="0">
            <tr>
              <td>
               <html:link action="/dataSet?name=${set.name}">
                 <img src="${set.iconImage}" class="dsIconImage"/>
               </html:link>
             </td>
             <td>
                       
             <div class="dsIconLabel">
               <html:link action="/dataSet?name=${set.name}">
                 ${set.name}
               </html:link>
             </div>
             <div class="dsIconDetail">
               ${set.subTitle}
               <c:if test="${status.count % 3 != 0}">
                 <im:hspacer width="82"/>
               </c:if>
             </div>
          
             </td>
           </tr>
          </table>

          <c:if test="${fn:length(DATASETS) != status.count}">
            <td/>
            <c:if test="${status.count % 3 == 0}">
              <tr/>
              <tr valign="top">
            </c:if>
            <td class="dsIconsElement">
          </c:if>
        </c:forEach>
      </td>
    </tr>
  </table>
</div>

<!-- /dataSetIcons -->
