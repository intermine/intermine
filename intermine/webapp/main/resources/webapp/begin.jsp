<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>
<link rel="stylesheet" type="text/css" href="css/begin.css"/>

<table id="frontpagelayout" cellspacing="0" cellpadding="0" >
   <tr><td>
   <!-- <p>
      FlyMine is an integrated database of genomic, expression and protein data for
      <i>Drosophila</i>, <i>Anopheles</i> and
      <i>C. elegans</i>.
      Integrating data makes it possible to run sophisticated data mining queries
      that span domains of biological knowledge.
    </p> -->
    
    <div class="body">
      <span style="font-size:+2em;"><a href="what.xml">What is FlyMine?</a></span>&nbsp;&nbsp;<span style="font-size:+1.4em"><a href="tour_1.html" target="_blank"  onclick="javascript:window.open('tour_1.html','_manual','toolbar=0,scrollbars=1,location=1,statusbar=1,menubar=0,resizable=1,width=800,height=600');return false">Take a tour!</a></span>
    
    <p style="color:#c00;"> 
      This is release 8.0 of FlyMine.  See the <a href="release-notes.xml">release notes</a> to find out what's new.
    </p>
    </div>
   </td>
   <td><p><tiles:insert name="tipWrapper.tile"/></p>
   </td>
   </tr>
   <tr>
      <td>
	<im:roundbox title="Templates" stylename="welcome" height="350">
<p><em><fmt:message key="begin.templates"/></em></p>
<p><u>Here are some templates you can try:</u></p>
	   <tiles:insert name="webSearchableList.tile">
      	    <!-- optional -->
            <tiles:put name="limit" value="5"/>
            <!-- bag or template? -->
            <tiles:put name="type" value="template"/>
            <!-- user or global -->
            <tiles:put name="scope" value="global"/>
            <tiles:put name="tags" value="im:public"/>
            <tiles:put name="showDescriptions" value="false"/>
            <tiles:put name="showSearchBox" value="false"/>
            <tiles:put name="height" value="100"/>
      </tiles:insert>
      <p><html:link action="/templates">View all ${templateCount} templates...</html:link></p>
      <div align="center"><html:link action="/templates.do"><img src="images/go_to_template_page.png" width="385" height="76" alt="Go To Template Page"></html:link></div>
	</im:roundbox>	
</td>
<td>
	<im:roundbox title="Lists" stylename="welcome" height="350">
	   <p><em><fmt:message key="begin.bags"/></em></p>
      <p><u>Have a look at some Lists we've made for you:</u></p>
      <tiles:insert name="webSearchableList.tile">
            <tiles:put name="limit" value="3"/>
             <%-- bag or template? --%>
            <tiles:put name="type" value="bag"/>
            <%-- user or global --%>
            <tiles:put name="scope" value="global"/>
            <tiles:put name="tags" value="im:frontpage"/>
            <tiles:put name="showSearchBox" value="false"/>
            <tiles:put name="height" value="100"/>
      </tiles:insert> 
	<p><html:link action="/bags">View all ${bagCount} bags...</html:link></p>
      <p><u>...or create your own List:</u></p>
      <div align="center"><html:link action="/bag.do"><img src="images/go_to_bag_page.png" align="center" width="317" height="80" alt="Go To Bag Page" border="0"></html:link></div>
	</im:roundbox>
</td>
</tr>
<tr>
   <td>
      <im:roundbox title="Data" stylename="welcome" height="180">
      <p><em><fmt:message key="begin.data"/></em></p>
   	<div align="center"><html:link action="/aspects.do"><img src="images/view_all_datasets.png" width="359" height="75" alt="View All Datasets"></html:link></div>
   	</im:roundbox>
   </td>
   <td>
	<im:roundbox title="Query Builder" stylename="welcome" height="180">
	<p><em><fmt:message key="begin.querybuilder"/></em></p>
    <div align="center"><html:link action="/customQuery.do"><img src="images/go_to_query_builder.png" width="305" height="75" alt="Go To Query Builder"></html:link></div>
   </im:roundbox>
   </td>
   </tr>
</table>

<!-- /begin.jsp -->