<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- reportDisplayers.jsp -->

<html:xhtml/>

<tiles:importAttribute name="reportObject"/>
<tiles:importAttribute name="placement"/>

<c:forEach items="${reportObject.reportDisplayers[placement]}" var="displayer">

   <a name="${displayer.displayerName}" class="anchor"></a>
   <c:choose>
     <c:when test="${displayer.showImmediately}">
      <%-- show right now --%>
      <tiles:insert name="reportDisplayer.tile">
        <tiles:put name="displayer" beanName="displayer" />
        <tiles:put name="reportObject" beanName="reportObject" />
      </tiles:insert>
      <br />
     </c:when>
     <c:otherwise>
      <%-- the AJAX way --%>
      <c:set var="displayerWrapper" value="${fn:toLowerCase(displayer.displayerName)}-wrapper"/>
      <div id="${displayerWrapper}" class="wrapper collection-table">
       <h3 class="loading">${displayer.nicerDisplayerName}</h3>
      </div>

      <script type="text/javascript">
       jQuery.ajax({
           url: 'modifyDetails.do',
           dataType: 'html',
           data: 'method=ajaxShowDisplayer&name=${displayer.displayerName}&id=${reportObject.id}',
           success: function(html) {
             jQuery('#${displayerWrapper}').hide().html(html).fadeIn().removeClass('collection-table');
           },
           error: function(jXHR, textStatus) {
             throw new Error('Failed to load Displayer "' + ${displayer.displayerName} + '", ' + textStatus);
           },
           complete: function(jXHR, textStatus) {
               //
           }
         });
       </script>
     </c:otherwise>
   </c:choose>
</c:forEach>

<!-- /reportDisplayers.jsp -->