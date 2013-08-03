<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>

<!-- bagUploadConfirm.jsp -->
<html:xhtml/>

<html:form action="/bagUploadConfirm" focus="newBagName" method="post" enctype="multipart/form-data">
    <input type="button" onclick="history.back();" id="goBack" value='<fmt:message key="bagUploadConfirm.goBack"/>' />
    <html:hidden property="matchIDs" styleId="matchIDs" />
    <html:hidden property="bagType"/>
    <input id="newBagName" type="text" name="newBagName" value="${bagName}">

    <c:choose>
        <c:when test="${empty buildNewBag}">
            <input type="hidden" name="upgradeBagName" value="${bagName}"/>
        </c:when>
    </c:choose>
</html:form>

<div id="component-400"></div>

<script type="text/javascript" src="js/baguploadconfirm.js"></script>

<script type="text/javascript">
(function() {
    var extraFilter = ("${bagExtraFilter}") ? "${bagExtraFilter}" : "all ${bagUploadConfirmForm.extraFieldValue}s".toLowerCase();
    // Generate new bag name.
    if (jQuery('input#newBagName').val().length == 0) {
        var t = new Date();
        var m = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
        jQuery('input#newBagName').val("${bagUploadConfirmForm.bagType} list for " + extraFilter + " " + t.getDate() +
            " " + m[t.getMonth()] + " " + t.getFullYear() + " " + t.getHours() + "." + t.getMinutes());
    }

    // Handle additional matches.
    var data = ${payload}; // coming from Struts
    require('component-400/app').call(null, data, '#component-400', function(err, selected) {
        // An error callback.
        if (err) throw err;
        // A list of selected identifiers.
        if (!!selected.length) {
            // Inject into the hidden field.
            jQuery('form#bagUploadConfirmForm input[name="matchIDs"]').val(selected.join(' '));
            // See `imdwr.js`.
            validateBagName('bagUploadConfirmForm');
        }
    });
})();
</script>
<!-- /bagUploadConfirm.jsp -->