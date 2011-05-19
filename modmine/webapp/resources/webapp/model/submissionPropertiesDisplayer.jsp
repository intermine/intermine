<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>

<!-- submissionPropertiesDisplayer.jsp -->

<div class="custom-displayer" id="submission-properties">
<%--
  <h3>Properties</h3>
  <p class="desc theme-5-background">
    <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
    Properties of this ${reportObject.type}
  </p>
  <p class="switchers theme-5-background">
    <c:forEach items="${propertyCounts}" var="entry" varStatus="status"><c:if test="${status.count > 1}">, </c:if>
    <!-- TODO: potential fail if key has spaces -->
        <c:choose>
            <c:when test='${entry.value == "0"}'>
                ${entry.key}: ${entry.value}
            </c:when>
            <c:otherwise>
                <a href="#" id="${fn:toLowerCase(entry.key)}" class="switcher">${entry.key}</a>: ${entry.value}
            </c:otherwise>
        </c:choose>
    </c:forEach>
  </p>

  <c:if test="${!empty propertyTables}">
    <c:forEach items="${propertyTables}" var="entry">
      <div class="table" id="${fn:toLowerCase(entry.key)}" style="display:none;">
        <h3 class="theme-1-border theme-5-background">${entry.key}</h3>
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
            <html:link styleClass="theme-1-color" action="/collectionDetails?id=${object.id}&amp;field=properties&amp;trail=${param.trail}">
                Show all in a table »
            </html:link>
        </p>
      <br/>
      </div>
      <div class="clear"></div>
    </c:forEach>
    <p class="in_table outer">
      Show all
      <html:link styleClass="theme-1-color" action="/collectionDetails?id=${object.id}&amp;field=properties&amp;trail=${param.trail}">
        properties
      </html:link>
      in a table »
    </p>
  </c:if>

  <script type="text/javascript">
    // apply different class to h3 so tables are not so separate
    jQuery("#regulatory-regions.custom-displayer div.table h3").each(function(i) {
        jQuery(this).toggleClass('theme-2-border');
        jQuery(this).toggleClass('theme-3-border');
    });

    // switcher between tables this displayer haz
    jQuery("#submission-properties.custom-displayer a.switcher").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#submission-properties.custom-displayer div.table:visible").each(function(j) {
              jQuery(this).hide();
              // hide more toggler
              jQuery(this).parent().find('p.toggle a.toggler').remove();
            });

            // show the one we want
            jQuery("#submission-properties.custom-displayer #" + jQuery(this).attr('id') + ".table").show();

            // show only 10 rows
            var rows = jQuery("#submission-properties.custom-displayer #" + jQuery(this).attr('id') + ".table tbody tr");
            var count = 10;
            rows.each(function(index) {
                count--;
                if (count < 0) {
                    jQuery(this).hide();
                }
            });
            // add a show next link
            if (count < 0) {
                var a = "<a href='#' style='float:right;margin-right:20px;' class='toggler'><span>Show more rows</span></a>";
                jQuery("#submission-properties.custom-displayer #" + jQuery(this).attr('id') + ".table p.toggle").append(a);
                jQuery("#submission-properties.custom-displayer #" + jQuery(this).attr('id') + ".table p.toggle a.toggler").bind(
                    "click",
                    function(f) {
                        // show another 10 rows
                        count = 10;
                        rows = jQuery("#submission-properties.custom-displayer #" + jQuery(this).attr('id') + ".table tbody tr:hidden");
                        rows.each(function(index) {
                            count--;
                            if (count > 0) {
                                jQuery(this).show();
                            }
                        });

                        // we have no more rows to show
                        if (jQuery("#submission-properties.custom-displayer #" + jQuery(this).attr('id') + ".table tbody tr:hidden").length == 0) {
                            // hide the link to more
                            jQuery("#submission-properties.custom-displayer #" + jQuery(this).attr('id') + ".table p.toggle a.toggler").remove();
                        }

                        // no linking on my turf
                        f.preventDefault();
                    });
            }

            // switchers all off
            jQuery("#submission-properties.custom-displayer a.switcher.active").each(function(j) {
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
    jQuery("#submission-properties.custom-displayer p.toggle a").each(function(i) {
      jQuery(this).bind(
        "click",
        function(e) {
            // hide anyone (!) that is shown
            jQuery("#submission-properties.custom-displayer div.table:visible").each(function(j) {
              jQuery(this).hide();
              // hide more toggler
              jQuery(this).parent().find('p.toggle a.toggler').remove();
            });

            // switchers all off
            jQuery("#submission-properties.custom-displayer a.switcher.active").each(function(j) {
              jQuery(this).toggleClass('active');
            });

            // show the global show all in a table
            jQuery(this).parent().parent().parent().find('p.in_table').show();entry

            // scroll to the top of the displayer (inlinetemplate.js)
            jQuery("#submission-properties.custom-displayer").scrollTo('fast', 'swing', -30);

            // no linking on my turf
            e.preventDefault();
        }
      );
    });
  </script>
--%>

<table id="submissionProperties" style="">
  <tr>
    <td style="width:15%;">Organism:</td>
    <td>
      <c:forEach var="organism" items="${organismMap}" varStatus="status">
        <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${organism.key}"><strong>${organism.value}</strong></html:link>
      </c:forEach>
    </td>
  </tr>
  <tr>
    <td valign="top">Cell Line:</td>
    <td>
      <c:choose>
        <c:when test="${not empty cellLineMap}">
          <c:forEach var="celline" items="${cellLineMap}" varStatus="status">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${celline.key}"><strong>${celline.value}</strong></html:link>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Antibody/Target:</td>
    <td id="antibodyContent">
      <c:choose>
        <c:when test="${not empty antibodyInfoList}">
          <c:forEach var="antibody" items="${antibodyInfoList}" varStatus="status">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${antibody.id}"><strong>${antibody.name}</strong></html:link>
            /
            <c:choose>
                <c:when test="${not empty antibody.target}">
                  <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${antibody.target.id}"><strong>${antibody.targetName}</strong></html:link>
                </c:when>
                <c:otherwise>
                  <i>target not available</i>
                </c:otherwise>
            </c:choose>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Developmental Stage:</td>
    <td>
      <c:choose>
        <c:when test="${not empty developmentalStageMap}">
          <c:forEach var="developmentalstage" items="${developmentalStageMap}" varStatus="status">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${developmentalstage.key}"><strong>${developmentalstage.value}</strong></html:link>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Strain:</td>
    <td>
      <c:choose>
        <c:when test="${not empty strainMap}">
          <c:forEach var="strain" items="${strainMap}" varStatus="status">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${strain.key}"><strong>${strain.value}</strong></html:link>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Tissue:</td>
    <td>
      <c:choose>
        <c:when test="${not empty tissueMap}">
          <c:forEach var="tissue" items="${tissueMap}" varStatus="status">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${tissue.key}"><strong>${tissue.value}</strong></html:link>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <tr>
    <td valign="top">Array:</td>
    <td>
      <c:choose>
        <c:when test="${not empty arrayMap}">
          <c:forEach var="array" items="${arrayMap}" varStatus="status">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${array.key}"><strong>${array.value}</strong></html:link>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <i>not available</i>
        </c:otherwise>
      </c:choose>
    </td>
  </tr>
  <c:if test="${not empty submissionPropertyMap}">
    <c:forEach var="submissionproperties" items="${submissionPropertyMap}">
      <tr>
        <td valign="top">${submissionproperties.key}:</td>
        <td id="${submissionproperties.key}Content_${fn:length(submissionproperties.value)}">
          <c:forEach var="submissionproperty" items="${submissionproperties.value}" varStatus="status">
            <html:link href="/${WEB_PROPERTIES['webapp.path']}/report.do?id=${submissionproperty.key}"><strong>${submissionproperty.value}</strong></html:link>
            <c:if test="${!status.last}">,  </c:if>
          </c:forEach>
        </td>
      </tr>
    </c:forEach>
  </c:if>
</table>

</div>

<script type="text/javascript" src="model/jquery_expander/jquery.expander.js"></script>
<script type="text/javascript">

        //TODO: concise code
        for (i=0;i<jQuery('td[id*="Content"]').length;i++)
        {
          if (jQuery('td[id*="Content"]').eq(i).attr("id") != "submissionDescriptionContent") {
            if (jQuery('td[id*="Content"]').eq(i).attr("id").indexOf("primerContent") != -1) {
                var id = jQuery('td[id*="Content"]').eq(i).attr("id");
                var count = id.substr(id.indexOf("_")+1);
                if (count > 15) {
                  jQuery('td[id*="Content"]').eq(i).expander({
                    slicePoint: 200,
                    expandText: 'read all ' + count + ' records'
                  });
                } else {
                  jQuery('td[id*="Content"]').eq(i).expander({
                    slicePoint: 200
                  });
                }
            } else {
                  jQuery('td[id*="Content"]').eq(i).expander({
                    slicePoint: 200
                  });
            }
          }
        }

</script>

<!-- /submissionPropertiesDisplayer.jsp -->