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
     <em><p>Enter identifiers to search for genes, proteins etc.</p></em>
        <html:form action="/keywordSearchResults" focus="searchTerm">
            <input name="searchTerm" type="text" class="qs_input">
            <html:submit>Go</html:submit>
        </html:form>
    </im:boxarea>
</td>
<td>
     <im:boxarea title="Analyse" stylename="plainbox" ixedWidth="300px">
     <em><p>Enter lists of identifiers to analyse</p></em>
        <html:form action="/keywordSearchResults" focus="searchTerm">
            <input name="searchTerm" type="text" class="qs_input">
            <html:submit>Go</html:submit>
        </html:form>
    </im:boxarea>
</td>
</tr>



<tr>
<td>
	   <div>
        <c:if test="${!empty WEB_PROPERTIES['project.rss']}">
          <tiles:insert name="news.tile" />
        </c:if>
      </div>   
</td>
<td>
     <im:boxarea title="Java/Perl API" stylename="plainbox" fixedWidth="300px">
Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nullam a sapien nisi. Aenean quam velit, hendrerit eget molestie porta, porta eget elit. Praesent at massa ac odio imperdiet pellentesque. Integer venenatis lacinia erat a viverra. Nulla facilisi. Fusce quis massa id purus blandit lacinia. Nullam mattis tortor eget augue accumsan tincidunt. Duis vestibulum rutrum euismod. Sed a ligula leo, eget volutpat nulla. Donec cursus lorem a turpis pellentesque et aliquam augue mattis. Sed porttitor massa tortor. In hac habitasse platea dictumst. 
    </im:boxarea>
</td>
</tr>
</table>
</div>

<!-- /begin.jsp -->
