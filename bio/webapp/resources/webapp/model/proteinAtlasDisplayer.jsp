<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

  <style>
    #proteinAtlasDisplayer table { float:left; border-spacing:0; border-collapse:collapse; }
    #proteinAtlasDisplayer table td, #proteinAtlasDisplayer table th { padding:2px 10px 2px 4px; }
    #proteinAtlasDisplayer table th { font-size:11px; }
    #proteinAtlasDisplayer table th.sortable { padding-left:8px; cursor:pointer; }
    #proteinAtlasDisplayer div.table table th.sortable { background:url("images/icons/sort-up.gif") no-repeat left; }
    #proteinAtlasDisplayer div.table.byLevel table th.sortable[title="byLevel"],
    #proteinAtlasDisplayer div.table.byOrgan table th.sortable[title="byOrgan"],
    #proteinAtlasDisplayer div.table.byCells table th.sortable[title="byCells"] { background:url("images/icons/sort.gif") no-repeat left; }
    #proteinAtlasDisplayer tr.alt td { background:#F6FCFE; border-top:1px solid #E6F7FE; border-bottom:1px solid #E6F7FE; }

    #proteinAtlasDisplayer table div.expressions { border:1px solid #000; display:inline-block; }
    #proteinAtlasDisplayer table div.expression { display:inline-block; width:20px; height:15px; cursor:pointer; float:left; }
    #proteinAtlasDisplayer table div.expression span.tissue { display:none; }
    #proteinAtlasDisplayer table div.expression span.tooltip { z-index:5; background:#FFF; border:1px solid #CCC; whitespace:no-wrap; display:inline-block;
      position:absolute; font-size:11px; padding:1px 2px; -moz-box-shadow:1px 1px 2px #DDD; -webkit-box-shadow:1px 1px 2px #DDD; box-shadow:1px 1px 2px #DDD;
      line-height:15px; color:#000; }

    #proteinAtlasDisplayer .strong { background:#CD3A32; }
    #proteinAtlasDisplayer .moderate { background:#FD9B34; }
    #proteinAtlasDisplayer .weak { background:#FFED8D; }
    #proteinAtlasDisplayer .negative { background:#FFF; }

    #proteinAtlasDisplayer table span.level span.value { display:none; }

    #proteinAtlasDisplayer div.sidebar { float:right; width:40%; }
    #proteinAtlasDisplayer div.sidebar p strong.uncertain { background:#FFD92C; border:1px solid #DDD; }
    #proteinAtlasDisplayer div.sidebar p strong.supportive { background:#A9CC30; border:1px solid #DDD; }
    #proteinAtlasDisplayer div.sidebar p.small { font-size:11px; }

    #proteinAtlasDisplayer div.legend { margin-top:10px; }
    #proteinAtlasDisplayer div.legend ul { margin:4px 0 0 0; }
    #proteinAtlasDisplayer div.legend ul li span, #proteinAtlasDisplayer table span.level { display:inline-block; width:20px; height:15px;
      border:1px solid #000; }

    #proteinAtlasDisplayer div.graph ul.header li { display:inline; font-size:11px; }

    #proteinAtlasDisplayer div.inactive { display:none; }
  </style>

<div id="proteinAtlasDisplayer">

<h3>Protein Atlas Tissue Expression</h3>

<div class="sidebar">

    <p>Reliability: <strong class="${fn:toLowerCase(expressions.reliability)}">${expressions.reliability}</strong></p>
    <p class="small">A validation score for immunohistochemistry is assigned for all antibodies and reflects the results of immunostaining.</p>

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

  <div class="table byOrgan active">
    <c:set var="tableRows" value="${expressions.byOrgan}" />
    <tiles:insert page="proteinAtlasDisplayerTable.jsp">
      <tiles:put name="rows" beanName="tableRows" />
    </tiles:insert>
  </div>

  <div class="table byCells inactive">
    <c:set var="tableRows" value="${expressions.byCells}" />
    <tiles:insert page="proteinAtlasDisplayerTable.jsp">
      <tiles:put name="rows" beanName="tableRows" />
    </tiles:insert>
  </div>

  <div class="table byLevel inactive">
    <c:set var="tableRows" value="${expressions.byLevel}" />
    <tiles:insert page="proteinAtlasDisplayerTable.jsp">
      <tiles:put name="rows" beanName="tableRows" />
    </tiles:insert>
  </div>

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
                    },
                    click: function() {
                      jQuery(that).parent().click();
                    }
                }).appendTo(that);
            },
            function() {
                jQuery(this).find('span.tooltip').remove();
            }
      );
    });

    <%-- table "sorting" --%>
    jQuery("#proteinAtlasDisplayer table th.sortable").click(function() {
      var order = jQuery(this).attr('title');
      jQuery("#proteinAtlasDisplayer div.table.active").removeClass('active').addClass('inactive');
      jQuery("#proteinAtlasDisplayer div.table."+order).removeClass('inactive').addClass('active');
    });
  })();
  </script>

</div>