<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div id="proteinAtlasDisplayer">
  <h3>Protein Atlas Tissue Expression</h3>
  <p>Reliability: <strong>${expressions.reliability}</strong></p>

  <style>
    #proteinAtlasDisplayer div.graph { float:left; }

  #proteinAtlasDisplayer li.organ { padding-left:10px; }
    #proteinAtlasDisplayer li.organ.alt { background:#F6FCFE; border-top:1px solid #E6F7FE; border-bottom:1px solid #E6F7FE; }
    #proteinAtlasDisplayer li.organ span { margin-right:20px; line-height:20px; }
    #proteinAtlasDisplayer li.organ span.count { float:left; width:20px; }

    #proteinAtlasDisplayer div.expressions { float:right; width:300px; }
    #proteinAtlasDisplayer div.expressions div.wrap { border:1px solid #000; display:inline-block; margin:2px 0; }
    #proteinAtlasDisplayer div.expression { float:left; display:inline-block; width:20px; height:15px; cursor:help; }
    #proteinAtlasDisplayer div.expression.strong { background:#CD3A32; }
    #proteinAtlasDisplayer div.expression.moderate { background:#FD9B34; }
    #proteinAtlasDisplayer div.expression.weak { background:#FFED8D; }
    #proteinAtlasDisplayer div.expression.negative { background:#FFF; }

    #proteinAtlasDisplayer div.expression span.tissue { display:none; }
    #proteinAtlasDisplayer div.expression span.tooltip { z-index:5; background:pink; border:2px solid red; whitespace:no-wrap; display:inline-block;
      position:absolute; font-size:20px; }
  </style>

<div class="graph">
  <ul class="organs">
  <c:forEach var="organ" items="${expressions.byOrgan}" varStatus="status">
    <li class="organ<c:if test='${status.count % 2 == 0}'> alt</c:if>" id="${organ.key}">
      <div class="expressions">
        <c:set var="expressionList" value="${organ.value}"/>
        <c:set var="cellTypesCount" value="${fn:length(expressionList)}"/>
        <span class="count">${cellTypesCount}</span>
        <div class="wrap">
          <c:forEach begin="${1}" end="${cellTypesCount}">
            <c:set var="expression" value="${expressionList.item}"/>
            <div class="expression ${fn:toLowerCase(expression.level)}"><span class="tissue">${expression.tissue}</span></div>
          </c:forEach>
        </div>
      </div>
      <span class="name">${expression.organ}</span>
      <div style="clear:both;"></div>
    </li>
  </c:forEach>
  </ul>
</div>
<div style="clear:both;"></div>

<script type="text/javascript">
<%-- hovering on an organ tissue level will merge the contained tissue expressions of the same level and show a tooltip --%>
(function() {
  jQuery("#proteinAtlasDisplayer div.expression").each(function() {
    var level = jQuery(this).attr('class').replace('expression', '').trim();
    var that = this;
    jQuery(this).hover(
          function() {
              jQuery('<span/>', {
                  className: 'tooltip',
                  html: function() {
                    var tooltip = new Array();
                    jQuery(that).parent().find("div.expression."+level).each(function() {
                        tooltip.push(jQuery(this).find('span.tissue').text());
                    });
                    return tooltip.join('<br/>');
                  }
              }).appendTo(that);
          },
          function() {
              jQuery(this).find('span.tooltip').remove();
          }
    );
  });
})();
</script>