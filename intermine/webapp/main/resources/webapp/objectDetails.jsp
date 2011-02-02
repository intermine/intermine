<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>

<!-- objectDetails.jsp -->
<html:xhtml/>

<link rel="stylesheet" type="text/css" href="css/960gs.css"/>

<div id="header_wrap">

<div id="object_header">
    <h1 class="title">${htmlPageTitle}<!-- <span>FBgn0004053 - zerknullt</span> --></h1>
    <p class="description">
      <c:forEach items="${object.fieldExprs}" var="expr">
        <c:choose>
          <c:when test="${object.fieldConfigMap[expr].showInSummary}">
            <c:if test="${!empty object.fieldValues[expr]}">
              ${expr}: <strong>${object.fieldValues[expr]}</strong>
            </c:if>
          </c:when>
        </c:choose>
      </c:forEach>
    </p>
</div>

<%--
<table id="menu" border="0" cellspacing="0">
    <tr>
        <td class="active">
            <div class="container">
                <span id="tab1">Summary</span><span class="right"></span><span class="left"></span>
            </div>
        </td>
        <td><div class="container"><span id="tab2">Function</span></div></td>
        <td><div class="container"><span id="tab3">Genome</span></div></td>
        <td><div class="container"><span id="tab4">Disease</span></div></td>
        <td><div class="container"><span id="tab5">Interactions</span></div></td>
        <td><div class="container"><span id="tab6">Reactions</span></div></td>
    </tr>
</table>
--%>

</div>

<div id="content" class="container_12">
    <p class="description grid_12"><img src="model/images/report_page/question-mark.png" alt="info">Summary represents a lorem ipsum dolor sit amet
        nunc eros felis, porta quis hendrerit sit amet, convallis in ipsum. Cras congue, nisi non volutpat scelerisque,
        augue nibh posuere dui, at tristique augue ante at velit. Vivamus arcu eros, tristique eu imperdiet a, mollis
        sed dui. Ut blandit, arcu non condimentum porta, quam lacus porttitor eros.</p>

    <div class="clear">&nbsp;</div>

    <%--
    <div class="box grid_8">
        <h3>Quick Links</h3>
        <div class="inner">
            <ul id="quick_links" class="quick_links">
                <!-- <li><a href="#"><span class="left">&nbsp;</span><strong>Graphs</strong><span class="right">&nbsp;</span></a></li> -->
            </ul>
            <script type="text/javascript">
              jQuery(document).ready(function() {
                jQuery("div.loadOnScroll").each(function(index) {
                    // fetch the text from the title
                    var text = jQuery(this).find('h3').text();
                    // split it on a space after removing unwanted whitespace
                    var textArray = text.replace(/^\s*|\s*$/g,'').split(" ");
                    // fetch the amount as the first value
                    var amount = textArray[0];
                    var textTitle = '';
                    // traverse the rest of the text for the title
                    for (i = 1; i < textArray.length; i++) {
                        textTitle += textArray[i];
                    }
                    // finally create an anchor
                    var anchor = '/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?<c:out value="${pageContext.request.queryString}" />'
                      + '#' + jQuery(this).find('h3').find('a').attr('name');

                    // create the list item
                    jQuery("#quick_links").append('<li><a href="' + anchor + '"><span class="left">&nbsp;</span>\
                          <strong>' + textTitle + '</strong>' + amount + '<span class="right">&nbsp;</span></a></li>')
                });
              });
            </script>
        </div>
    </div>
    --%>

    <c:set value="${fn:length(CATEGORIES)}" var="aspectCount"/>
    <c:set var="templateIdPrefix" value="objectDetailsTemplate${objectType}"/>
    <c:set var="miscId" value="objectDetailsMisc${objectType}"/>

    <%-- All other references and collections --%>
    <script type="text/javascript">
      <!--//<![CDATA[
        var modifyDetailsURL = '<html:rewrite action="/modifyDetails"/>';
        var detailsType = 'object';
      //]]>-->
      </script>
    <script type="text/javascript" src="js/inlinetemplate.js"></script>

    <div class="box grid_9">
      <tiles:insert page="/objectDetailsRefsCols.jsp">
        <tiles:put name="object" beanName="object"/>
        <tiles:put name="placement" value="im:aspect:Miscellaneous"/>
      </tiles:insert>
    </div>

    <div class="box grid_3" style="margin-top:30px;">
        <tiles:insert page="/objectDetailsDisplayers.jsp">
          <tiles:put name="placement" value=""/>
          <tiles:put name="displayObject" beanName="object"/>
          <tiles:put name="heading" value="true"/>
        </tiles:insert>
    </div>

</div>

<!-- /objectDetails.jsp -->