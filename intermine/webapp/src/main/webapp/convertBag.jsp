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
  <c:forEach items="${customConverters}" var="converter">
    <h3 class="goog">${converter.title}</h3>

    <input type="text" name="extraFieldValue" style="display:none;" />
    <input type="text" name="convertToThing" value="Convert" style="display:none;" />

    <script type="text/javascript" charset="utf-8">
        Object.prototype.hasOwnProperty = function(property) {
            return typeof(this[property]) !== 'undefined'
        };

        function convertBagCallBag(datei) {
          var _i, _len, _ref, _target, _ref1, _ref2;
          _json = jQuery.parseJSON(datei);
          _target = jQuery("ul#customConverter");
          _ref1 = "name";
          _ref2 = "count";
          for (_i = 0, _len = _json.length; _i < _len; _i++) {
            _entry = _json[_i];
            if (_entry.hasOwnProperty(_ref1) && _entry.hasOwnProperty(_ref2)) {
              var _text, _count;
              _name = _entry[_ref1];
              _count = _entry[_ref2];
              _target.append(
                jQuery("<li/>", {
                  style: 'display:inline-block',
                  html: function() {
                    return jQuery("<a/>", {
                      href: "#",
                      "data-value": _name,
                      text: function() {
                        return "" + _name + " (" + _count + ")";
                      },
                      click: function(e) {
                        var _value, form;
                        _value = jQuery(this).attr("data-value");
                        jQuery("input[name='extraFieldValue']").attr("value", _value);
                        form = jQuery(e.target).closest('form');
                        form.submit();
                      }
                    });
                  }
                })
              );
            }
          }
        }

        getCustomConverterCounts('${bag.name}', '${converter.className}', convertBagCallBag);
    </script>
    <ul id="customConverter"></ul>
  </c:forEach>
  </div>

</c:if>
<!-- /custom converters -->

</div>

<!-- /convertBag.jsp -->
