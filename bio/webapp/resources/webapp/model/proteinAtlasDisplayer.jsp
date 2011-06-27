<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<div id="proteinAtlasDisplayer">
  <h3>Protein Atlas Tissue Expression</h3>
  <p>Reliability: <strong>${expressions.reliability}</strong></p>

  <style>
    #proteinAtlasDisplayer table { float:left; border-spacing:0; border-collapse:collapse; }
    #proteinAtlasDisplayer table td, #proteinAtlasDisplayer table th { padding:2px 10px 2px 4px; }
    #proteinAtlasDisplayer table th { font-size:11px; }
    #proteinAtlasDisplayer table th.sortable { background:url("images/icons/sort.gif") no-repeat left; padding-left:8px; cursor:pointer; }
    #proteinAtlasDisplayer tr.alt td { background:#F6FCFE; border-top:1px solid #E6F7FE; border-bottom:1px solid #E6F7FE; }

    #proteinAtlasDisplayer table div.expressions { border:1px solid #000; display:inline-block; }
    #proteinAtlasDisplayer table div.expression { display:inline-block; width:20px; height:15px; cursor:help; float:left; }
    #proteinAtlasDisplayer table div.expression span.tissue { display:none; }
    #proteinAtlasDisplayer table div.expression span.tooltip { z-index:5; background:#FFF; border:1px solid #CCC; whitespace:no-wrap; display:inline-block;
      position:absolute; font-size:11px; padding:1px 2px; -moz-box-shadow:1px 1px 2px #DDD; -webkit-box-shadow:1px 1px 2px #DDD; box-shadow:1px 1px 2px #DDD;
      line-height:15px; }

    #proteinAtlasDisplayer .strong { background:#CD3A32; }
    #proteinAtlasDisplayer .moderate { background:#FD9B34; }
    #proteinAtlasDisplayer .weak { background:#FFED8D; }
    #proteinAtlasDisplayer .negative { background:#FFF; }

    #proteinAtlasDisplayer table span.level span.value { display:none; }

    #proteinAtlasDisplayer div.sidebar { float:right; }
    #proteinAtlasDisplayer div.legend ul li span, #proteinAtlasDisplayer table span.level { display:inline-block; width:20px; height:15px;
      border:1px solid #000; }

    #proteinAtlasDisplayer div.graph ul.header li { display:inline; font-size:11px; }
  </style>

<div class="sidebar">
  <div class="legend">
    <strong>Level of antibody staining</strong>
    <ul class="level">
      <li><span class="strong"></span> Strong</li>
      <li><span class="moderate"></span> Moderate</li>
      <li><span class="weak"></span> Weak</li>
      <li><span class="negative"></span> Negative</li>
    </ul>
  </div>
</div>

<h3>By Organ Name</h3>
<c:set var="tableRows" value="${expressions.byOrgan}" />
<tiles:insert page="proteinAtlasDisplayerTable.jsp">
  <tiles:put name="rows" beanName="tableRows" />
</tiles:insert>

<div style="clear:both;"></div>

<h3>By Cell Count</h3>
<c:set var="tableRows" value="${expressions.byCells}" />
<tiles:insert page="proteinAtlasDisplayerTable.jsp">
  <tiles:put name="rows" beanName="tableRows" />
</tiles:insert>

<div style="clear:both;"></div>

<h3>By Level</h3>
<c:set var="tableRows" value="${expressions.byLevel}" />
<tiles:insert page="proteinAtlasDisplayerTable.jsp">
  <tiles:put name="rows" beanName="tableRows" />
</tiles:insert>

<div style="clear:both;"></div>

<script type="text/javascript">
(function() {
  <%-- hovering on an organ tissue level will merge the contained tissue expressions of the same level and show a tooltip --%>
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

  <%-- table sorting --%>
  jQuery("#proteinAtlasDisplayer table th.sortable").click(function() {
    var order = jQuery(this).attr('title');
  });
})();
</script>

</div>