<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>


<!-- overlappingFeaturesDisplayer.jsp -->

<div class="custom-displayer" id="overlapping-features">
  <h3>Overlapping Features</h3>
  <p class="desc theme-5-background">
    <img class="tinyQuestionMark" src="images/icons/information-small-blue.png" alt="?">
    Genome features that overlap coordinates of this ${reportObject.type}
  </p>
  <p class="switchers theme-5-background">
    <c:forEach items="${featureCounts}" var="entry" varStatus="status"><c:if test="${status.count > 1}">, </c:if>
    <%-- TODO: potential fail if key has spaces --%>
    <a href="#" id="${fn:toLowerCase(entry.key)}" class="switcher">${entry.key}</a>: ${entry.value}</c:forEach>
  </p>

  <c:if test="${!empty featureTables}">
    <c:forEach items="${featureTables}" var="entry">
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
      <br/>
      </div>
      <div class="clear"></div>
    </c:forEach>
  </c:if>

  <script type="text/javascript">
    // apply different class to h3 so tables are not so separate
    jQuery("#overlapping-features.custom-displayer div.table h3").each(function(i){
        jQuery(this).toggleClass('theme-2-border');
        jQuery(this).toggleClass('theme-3-border');
    });

    // switcher between tables this displayer haz
    jQuery("#overlapping-features.custom-displayer a.switcher").each(function(i){
      jQuery(this).bind(
        "click",
        function(e){
            // hide anyone (!) that is shown
            jQuery("#overlapping-features.custom-displayer div.table:visible").each(function(j){
              jQuery(this).hide();
            });

            // show the one we want
            jQuery("#overlapping-features.custom-displayer #" + jQuery(this).attr('id') + ".table").show();

            // switchers all off
            jQuery("#overlapping-features.custom-displayer a.switcher.active").each(function(j){
              jQuery(this).toggleClass('active');
            });

            // we are active
            jQuery(this).toggleClass('active');

            // no linking on my turf
            e.preventDefault();
        }
      );
    });

    // table hider
    jQuery("#overlapping-features.custom-displayer p.toggle a").each(function(i){
      jQuery(this).bind(
        "click",
        function(e){
            // hide anyone (!) that is shown
            jQuery("#overlapping-features.custom-displayer div.table:visible").each(function(j){
              jQuery(this).hide();
            });

            // switchers all off
            jQuery("#overlapping-features.custom-displayer a.switcher.active").each(function(j){
              jQuery(this).toggleClass('active');
            });


            // no linking on my turf
            e.preventDefault();
        }
      );
    });
  </script>

</div>

<!-- /overlappingFeaturesDisplayer.jsp -->
