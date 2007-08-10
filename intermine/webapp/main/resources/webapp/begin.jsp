<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<div class="body">
	<div style="float:left;margin-left:20px;width:400px">
    	<span style="font-size:+2em;"><a href="http://www.flymine.org/what.shtml">What is FlyMine?</a></span>&nbsp;&nbsp;
    	<span style="font-size:+1.4em"><a href="http://www.flymine.org/tour/tour_1.html" target="_blank"  onclick="javascript:window.open('http://www.flymine.org/tour/tour_1.html','_manual','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Take a tour!</a></span>
		<p class="errors">This is release 8.0 of FlyMine.  See the <a href="http://www.flymine.org/release-notes.shtml">release notes</a> to find out what's new.</p>
    </div>
    <div style="margin-left:420px;width:600px;">
        <tiles:insert name="tipWrapper.tile"/>
    </div>
    <div style="clear:both;"></div>
	
	<div id="aspectsFront" class="actionArea">
     <h1>Data</h1>
     <em><p><fmt:message key="begin.data"/></p></em>

          <ul>
		       <c:forEach var="entry" items="${ASPECTS}" varStatus="status">
			       <c:set var="set" value="${entry.value}"/>
                   <li class="aspectOverview"                            
                   		onmouseover="javascript:$('aspectDescr_${status.index}').style.visibility = 'visible'"
                         onmouseout="javascript:$('aspectDescr_${status.index}').style.visibility = 'hidden'">                     
                     <html:link action="/aspect?name=${set.name}">
                       <img src="<html:rewrite page="/${set.iconImage}"/>" class="aspectIcon" />
                     </html:link>
                     <p>
                       <html:link action="/aspect?name=${set.name}">
                         ${set.name}
                       </html:link><br/>
                       
                       <div id="aspectDescr_${status.index}" class="aspectDescr">${set.subTitle}</div>
                     </p>
                  </li>
             </c:forEach>
          </ul>

		<div class="clear-both"></div>
     </div>

	<div id="leftColumn">
	
      <im:roundbox title="Templates" stylename="welcome" height="350">
        <em><p><fmt:message key="begin.templates"/></p></em>
        <br/>
        <p>Some example templates (<html:link action="/templates.do">${templateCount} total</html:link>):</p>
        <tiles:insert name="webSearchableList.tile">
          <!-- optional -->
          <tiles:put name="limit" value="3"/>
          <!-- bag or template? -->
          <tiles:put name="type" value="template"/>
          <!-- user or global -->
          <tiles:put name="wsListId" value="global_template"/>
          <tiles:put name="scope" value="global"/>
          <tiles:put name="tags" value="im:frontpage"/>
          <tiles:put name="showDescriptions" value="false"/>
          <tiles:put name="showSearchBox" value="true"/>
        </tiles:insert>
        
        <im:useTransparentImage src="images/go_to_template_page.png" id="gototemplates" link="/templates.do" width="156px" height="51px"  />
        <br/><br/><br/>
      </im:roundbox>

      <im:roundbox title="Lists" stylename="welcome" height="350">
        <p><em><fmt:message key="begin.bags"/></em></p>
        <br/>
        <p>Some example lists (<html:link action="/bag.do">${bagCount} total</html:link>):</p>
        <tiles:insert name="webSearchableList.tile">
          <tiles:put name="limit" value="3"/>
          <tiles:put name="wsListId" value="global_bag"/>
          <%-- bag or template? --%>
          <tiles:put name="type" value="bag"/>
          <%-- user or global --%>
          <tiles:put name="scope" value="global"/>
          <tiles:put name="tags" value="im:frontpage"/>
          <tiles:put name="showSearchBox" value="false"/>
          <%--<tiles:put name="height" value="100"/>--%>
        </tiles:insert>
        <im:useTransparentImage src="images/go_to_list_page.png" id="gotolists" link="/bag.do" width="156px" height="51px"  />
		<br/><br/><br/>
      </im:roundbox>

      <im:roundbox title="Query Builder" stylename="welcome" height="180">
        <p><em><fmt:message key="begin.querybuilder"/></em></p>        
        <im:useTransparentImage src="images/go_to_query_builder.png" id="gotoqb" link="/customQuery.do" width="156px" height="51px"  />    
		<br/><br/><br/>
      </im:roundbox>
	</div>
</div>

<!-- /begin.jsp -->
