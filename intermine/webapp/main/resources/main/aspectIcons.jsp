
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- aspectIcons -->

<div class="body">
  <table border="0" width="100" cellspacing="0" cellpadding="0">
    <tr>
      <td width="33%" valign="top">
        <c:forEach var="entry" items="${ASPECTS}" varStatus="status">
          <c:set var="set" value="${entry.value}"/>
          
          <table border="0">
            <tr>
              <td>
               <html:link action="/aspect?name=${set.name}">
                 <img src="${set.iconImage}" class="dsIconImage"/>
               </html:link>
             </td>
             <td>
                       
             <div class="dsIconLabel">
               <html:link action="/aspect?name=${set.name}">
                 ${set.name}
               </html:link>
             </div>
             <div class="dsIconDetail">
               ${set.subTitle}
               <%--<c:if test="${status.count % 3 != 0}">
                 <im:hspacer width="82"/>
               </c:if>--%>
               <im:hspacer width="82"/>
             </div>
          
             </td>
           </tr>
          </table>

          <c:if test="${fn:length(aspectS) != status.count}">
            <td/>
            <c:if test="${status.count % 3 == 0}">
              <tr/>
              <tr valign="top">
            </c:if>
            <td valign="top" width="33%" >
          </c:if>
        </c:forEach>
      </td>
    </tr>
  </table>
</div>

<!-- /aspectIcons -->
