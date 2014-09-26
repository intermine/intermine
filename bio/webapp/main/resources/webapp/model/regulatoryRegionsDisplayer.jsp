<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="/WEB-INF/functions.tld" prefix="imf" %>

<!-- regulatoryRegionsDisplayer.jsp -->

<div class="collection-of-collections" id="regulatory-regions">
  <div class="header">
    <h3>Regulatory Regions</h3>
    <p>
      <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
      View regulatory regions associated with this gene by type.
    </p>
    <div class="switchers">
      <c:forEach items="${regionCounts}" var="entry" varStatus="status"><c:if test="${status.count > 1}">, </c:if>
      <%-- TODO: potential fail if key has spaces --%>
      <a href="#" id="${fn:toLowerCase(entry.key)}" class="switcher"><c:out value="${imf:formatPathStr(entry.key, INTERMINE_API, WEBCONFIG)}"/><c:if test="${entry.value != 1}">s</c:if></a>: ${entry.value}</c:forEach>
    </div>
  </div>

  <c:choose>
    <c:when test="${!empty regionTables}">
      <c:forEach items="${regionTables}" var="entry">
        <div class="collection-table" id="${fn:toLowerCase(entry.key)}" style="display:none;">
          <h3 class=""><c:out value="${imf:formatPathStr(entry.key, INTERMINE_API, WEBCONFIG)}"/>s</h3>
          <div class="clear"></div>

          <c:set var="inlineResultsTable" value="${entry.value}" />
          <tiles:insert page="/reportCollectionTable.jsp">
             <tiles:put name="inlineResultsTable" beanName="inlineResultsTable" />
             <tiles:put name="object" beanName="reportObject.object" />
             <tiles:put name="fieldName" value="${entry.key}" />
          </tiles:insert>
          <div class="toggle">
            <a style="float:right;" class="less"><span>Hide</span></a>
          </div>
          <div class="show-in-table">
            <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=regulatoryRegions&amp;trail=${param.trail}">
              Show all in a table &raquo;
            </html:link>
          </div>
        <br/>
        </div>
        <div class="clear"></div>
      </c:forEach>
      <div class="show-in-table outer">
        <html:link action="/collectionDetails?id=${reportObject.object.id}&amp;field=regulatoryRegions&amp;trail=${param.trail}">
          Show all in a table &raquo;
        </html:link>
      </div>
    </c:when>
    <c:otherwise>
    <p>No results found.</p>    
    </c:otherwise>
  </c:choose>

  <script type="text/javascript">
    // switcher between tables this displayer haz
    jQuery("#regulatory-regions.collection-of-collections a.switcher").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#regulatory-regions.collection-of-collections div.collection-table:visible").each(function(j) {
              jQuery(this).hide();
              // remove more toggler
              jQuery(this).parent().find('div.toggle a.more').remove();
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
            // add a show more link
            if (count < 0) {
                var that = this;
                jQuery('<a/>', {
                    'class': 'more',
                    'title': 'Show more rows',
                    'style': 'float:right; margin-right:20px;',
                    'html': jQuery('<span/>', {
                        'text': 'Show more rows'
                    }),
                    click: function(f) {
                      // show another 10 rows
                      var limit = 10;
                      jQuery("#regulatory-regions.collection-of-collections #" + jQuery(that).attr('id') + ".collection-table table tbody tr:hidden").each(function(i, val) {
                            if (i <= limit) {
                              jQuery(this).show();
                            }
                      });

                      // we have no more rows to show
                      if (jQuery("#regulatory-regions.collection-of-collections #" + jQuery(that).attr('id') + ".collection-table table tbody tr:hidden").length == 0) {
                          // hide the link to more
                          jQuery("#regulatory-regions.collection-of-collections #" + jQuery(that).attr('id') + ".collection-table div.toggle a.more").remove();
                      }

                      // no linking on my turf
                      f.preventDefault();
                    }
                }).appendTo("#regulatory-regions.collection-of-collections #" + jQuery(this).attr('id') + ".collection-table div.toggle");
            }

            // switchers all off
            jQuery("#regulatory-regions.collection-of-collections a.switcher.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

            // we are active
            jQuery(this).toggleClass('active');

            // hide the global show all in a table
            jQuery(this).parent().parent().parent().find('div.show-in-table.outer').hide();

            // no linking on my turf
            e.preventDefault();
        }
      );
    });

    // table hider
    jQuery("#regulatory-regions.collection-of-collections div.toggle a.less").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#regulatory-regions.collection-of-collections div.collection-table:visible").each(function(j) {
              jQuery(this).hide();
              // hide more toggler
              jQuery(this).parent().find('div.toggle a.more').remove();
            });

            // switchers all off
            jQuery("#regulatory-regions.collection-of-collections a.switcher.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

            // show the global show all in a table
            jQuery(this).parent().parent().parent().find('div.show-in-table').show();

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
