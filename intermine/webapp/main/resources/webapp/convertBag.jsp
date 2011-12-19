<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>

<!-- convertBag.jsp -->
<tiles:importAttribute />

<div id="convert-and-orthologues">

<!-- convert e.g. Transcript, Protein etc. -->
<c:if test="${!empty conversionTypes}">

   <h3 class="goog"><img src="images/icons/convert.png" title="Convert objects in this bag to different type"/>&nbsp;Convert to a different type</h3>

   <c:forEach items="${conversionTypes}" var="type">
     <script type="text/javascript" charset="utf-8">
       getConvertCountForBag('${bag.name}','${type}','${idname}');
     </script>
     <c:set var="nameForURL"/>
     <str:encodeUrl var="nameForURL">${bag.name}</str:encodeUrl>
     <html:link action="/modifyBagDetailsAction.do?convert=${type}&bagName=${nameForURL}">${type}</html:link>&nbsp;&nbsp;<span id="${type}_convertcount_${idname}">&nbsp;</span><br>
   </c:forEach>

</c:if>

<!-- custom converters -->
<c:if test="${orientation=='h'}">

  <div class="orthologues">
  <c:forEach items="${customConverters}" var="entry">
     <c:set var="converter" value="${entry.key}" />
     <c:set var="results" value="${entry.value}" />

    <h3 class="goog">converter.title</h3>
    <p>
    <c:forEach items="${results}" var="resultsEntry">
      <a href="#">${resultsEntry.key}</a> (${resultsEntry.value})
    </c:forEach>
    <html:select property="extraFieldValue" styleId="extraConstraintSelect" style="display:none;">
        <c:forEach items="${results}" var="resultsEntry">
         <html:option value="${resultsEntry.key}">${resultsEntry.key}</html:option>
       </c:forEach>
    </html:select>
    &nbsp;
    <html:submit property="convertToThing" style="display:none;">Convert</html:submit>
    </p>
  </c:forEach>
  </div>

</c:if>
<!-- /custom converters -->

<script type="text/javascript">
(function() {
    jQuery('#convert-and-orthologues div.orthologues a').click(function() {
      var t 	  = jQuery(this).text(),
        ortho = jQuery(this).closest('div.orthologues');
      ortho.find('select option').each(function() {
        if (jQuery(this).text() == t) {
          jQuery(this).attr('selected', 'selected');
          ortho.find('input[name="convertToThing"]').click();
        }
      });
    });
})();
</script>

</div>

<!-- /convertBag.jsp -->
