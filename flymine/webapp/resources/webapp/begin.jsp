<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- begin.jsp -->
<html:xhtml/>

<div class="body">


<table>
<tr>
<td>
     <im:boxarea title="Search" stylename="plainbox" floatValue="left" fixedWidth="300px">
     <em><p>Search FlyMine. Enter <strong>identifiers</strong>, <strong>names</strong> or <strong>keywords</strong> for
                genes, pathways, authors, ontology terms, etc.  (e.g. <i>eve</i>, <i>embryo</i>,
                <i>zen</i>, <i>allele</i>)
     </p></em>
        <form action="<c:url value="/keywordSearchResults.do" />" name="search" method="get">
            <input type="text" id="keywordSearch" name="searchTerm" value="" style="width: 200px;" />
            <input type="submit" name="searchSubmit" value="Search" />
        </form>
    </im:boxarea>
    
    
</td>
<td>
     <im:boxarea title="Java/Perl API" stylename="plainbox" fixedWidth="300px">
We support programatic access to our data through Application Programming Interface too! Choose from options below:
<ul>
<li>Java
<li>Perl
</ul> 
    </im:boxarea>
</td>
<td>
     <im:boxarea title="Analyse" stylename="plainbox" fixedWidth="300px">
     <em><p>Enter a list of identifiers to be forwarded to the list analysis page.  Click here to view an example.</p></em>
        <html:form action="/buildBag" focus="pasteInput">

                <html:select styleId="typeSelector" property="type">
                        <html:option value="Gene">Gene</html:option>
                        <html:option value="Protein">Protein</html:option>
                </html:select>
            <html:textarea styleId="pasteInput" property="text" rows="2" cols="30" />
            <html:submit styleId="submitBag">Analyse</html:submit>
        </html:form>
    </im:boxarea>
</td>
</tr>

<tr>
<td colspan=3>
	   <div>
        <c:if test="${!empty WEB_PROPERTIES['project.rss']}">
          <tiles:insert name="news.tile" />
        </c:if>
      </div>   
</td>
</tr>
</table>
</div>
<script language="javascript">
<!--//<![CDATA[
    document.getElementById("takeATourLink").style.display="block";
//]]>-->
</script>
<!-- /begin.jsp -->
