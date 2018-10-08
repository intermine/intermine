<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- quickSearch.jsp -->
<c:set var="ids" value="${WEB_PROPERTIES['quickSearch.identifiers']}"/>

<script type="text/javascript">
function clearElement(e) {
     var value =document.getElementById('quickSearchInput').value;
     if(value == '${ids}') {
            e.value = "";
            jQuery('#quickSearchInput').css("color", "#000");
            jQuery('#quickSearchInput').css("fontStyle","normal");
     }
}

</script>
<form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get" style="display:inline;">
<fmt:message key="header.search"/>&nbsp;<input
style="width:150px;color:#666;font-style:italic;font-size:1em" type="text" id="quickSearchInput" name="searchTerm" value="${ids}" onFocus="clearElement(this);" />
<input type="submit" id="quickSearchButton" name="searchSubmit" value="GO" />
</form>
<!-- /quickSearch.jsp -->

 <script type="text/javascript">
    jQuery(function() {
        // For each of the search boxes.
        jQuery.each([ '#quicksearch', '#search-bochs' ], function(i, sel) {
            var div, input, submit, handler, edited, placeholder, _ref, _ele;

            // Do we exist on this page?
            div = jQuery(sel);
            input = div.find('input[type="text"]');
            submit = div.find('input[type="submit"]');
            _ref = [ div, input, submit ];
            while (_ele = _ref.pop()) {
                if (_ele.length === 0) {
                    return;
                }
            }

            // By default we are not focused.
            edited = false;

            // Find our placeholder.
            placeholder = input.val();

            handler = function(value) {
                // If no-one has edited the input field and the value is a placeholder.
                if (!edited && (value.length === 0 || value === placeholder)) {
                    return false;
                }
            };

            // Find the input box and attach Enter keypress handler.
            input.keypress(function(evt) {
                if (evt.which == 13) {
                    return handler(input.val());
                } else {
                    edited = true;
                }
            });
            // Find the submit button and attach a handler.
            submit.click(function() {
                return handler(input.val());
            });
        });
    });
</script>
