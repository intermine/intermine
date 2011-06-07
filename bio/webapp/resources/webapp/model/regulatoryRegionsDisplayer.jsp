<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- regulatoryRegionsDisplayer.jsp -->

<div class="collection-of-collections" id="regulatory-regions">
  <div class="header">
	  <h3>Regulatory Regions</h3>
	  <p class="desc">
	    <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
	    Something that is relevant and descriptive should go here, this ain't it
	  </p>
	  <div class="switchers">
	    <c:forEach items="${regionCounts}" var="entry" varStatus="status"><c:if test="${status.count > 1}">, </c:if>
	    <%-- TODO: potential fail if key has spaces --%>
	    <a href="#" id="${fn:toLowerCase(entry.key)}" class="switcher">${entry.key}</a>: ${entry.value}</c:forEach>
	  </div>
  </div>
  <c:if test="${!empty regionTables}">
    <c:forEach items="${regionTables}" var="entry">
      <div class="collection-table" id="${fn:toLowerCase(entry.key)}" style="display:none;">
        <h3 class="">${entry.key}</h3>
        <div class="clear"></div>

        <c:set var="inlineResultsTable" value="${entry.value}" />
        <tiles:insert page="/reportCollectionTable.jsp">
           <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
           <tiles:put name="object" beanName="reportObject.object" />
           <tiles:put name="fieldName" value="${entry.key}" />
        </tiles:insert>
        <p class="toggle">
          <a href="#" style="float:right;" class="collapser"><span>Hide</span></a>
        </p>
        <p class="in_table">
          <html:link action="/collectionDetails?id=${object.id}&amp;field=regulatoryRegions&amp;trail=${param.trail}">
            Show all in a table »
          </html:link>
        </p>
      <br/>
      </div>
      <div class="clear"></div>
    </c:forEach>
    <p class="in_table outer">
      <html:link action="/collectionDetails?id=${object.id}&amp;field=regulatoryRegions&amp;trail=${param.trail}">
        Show all in a table »
      </html:link>
    </p>
  </c:if>

  <script type="text/javascript">
    // apply different class to h3 so tables are not so separate
    jQuery("#regulatory-regions.collection-of-collections div.collection-table h3").each(function(i) {
        jQuery(this).toggleClass('someclass');
        jQuery(this).toggleClass('someclass');
    });

    // switcher between tables this displayer haz
    jQuery("#regulatory-regions.collection-of-collections a.switcher").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#regulatory-regions.collection-of-collections div.collection-table:visible").each(function(j) {
              jQuery(this).hide();
              // hide more toggler
              jQuery(this).parent().find('p.toggle a.toggler').remove();
            });

            // show the one we want
            jQuery("#regulatory-regions.collection-of-collections #" + jQuery(this).attr('id') + ".collection-table").show();

            // show only 10 rows
            var rows = jQuery("#regulatory-regions.collection-of-collections #" + jQuery(this).attr('id') + ".collection-table tbody tr");
            var count = 10;
            rows.each(function(index) {
                count--;
                if (count < 0) {
                    jQuery(this).hide();
                }
            });
            // add a show next link
            if (count < 0) {
                jQuery('<a/>', {
                    className: 'toggler',
                    href: '#',
                    title: 'Show more rows',
                    style: 'float:right; margin-right:20px;',
                    html: jQuery('<span/>', {
                        text: 'Show more rows'
                    }),
                    click: function(f) {
	                    // show another 10 rows
	                    count = 10;
	                    rows = jQuery("#regulatory-regions.custom-displayer #" + jQuery(this).attr('id') + ".collection-table tbody tr:hidden");
	                    rows.each(function(index) {
	                        count--;
	                        if (count > 0) {
	                            jQuery(this).show();
	                        }
	                    });

	                    // we have no more rows to show
	                    if (jQuery("#regulatory-regions.collection-of-collections #" + jQuery(this).attr('id') + ".collection-table tbody tr:hidden").length == 0) {
	                        // hide the link to more
	                        jQuery("#regulatory-regions.collection-of-collections #" + jQuery(this).attr('id') + ".collection-table p.toggle a.toggler").remove();
	                    }

	                    // no linking on my turf
	                    f.preventDefault();
                    }
                }).appendTo("#regulatory-regions.collection-of-collections #" + jQuery(this).attr('id') + ".collection-table p.toggle");
            }

            // switchers all off
            jQuery("#regulatory-regions.collection-of-collections a.switcher.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

            // we are active
            jQuery(this).toggleClass('active');

            // hide the global show all in a table
            jQuery(this).parent().parent().find('p.in_table.outer').hide();

            // no linking on my turf
            e.preventDefault();
        }
      );
    });

    // table hider
    jQuery("#regulatory-regions.collection-of-collections p.toggle a").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#regulatory-regions.collection-of-collections div.table:visible").each(function(j) {
              jQuery(this).hide();
              // hide more toggler
              jQuery(this).parent().find('p.toggle a.toggler').remove();
            });

            // switchers all off
            jQuery("#regulatory-regions.collection-of-collections a.switcher.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

            // show the global show all in a table
            jQuery(this).parent().parent().parent().find('p.in_table').show();

            // scroll to the top of the displayer (inlinetemplate.js)
            jQuery("#regulatory-regions.collection-of-collections").scrollTo('fast', 'swing', -30);

            // no linking on my turf
            e.preventDefault();
        }
      );
    });
  </script>

</div>

<!-- /regulatoryRegionsDisplayer.jsp -->
