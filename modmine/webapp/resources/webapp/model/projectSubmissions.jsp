<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im" %>
<%@ taglib uri="http://flymine.org/imutil" prefix="imutil" %>
<%@ taglib uri="http://jakarta.apache.org/taglibs/string-1.1" prefix="str" %>


<tiles:importAttribute/>


<html:xhtml/>

<div class="body">

<table cellpadding="0" cellspacing="0" border="0" class="dbsources">
  <tr>
    <th>Project</th>
    <th>Title</th>
    <th>Principal Investigator</th>
    <th>Providers</th>    
    <th>Submissions</th>    
  </tr>
<c:forEach items="${providers}" var="item">
 <tr><td> 
    <html:link  href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${item.key.id}">
 ${item.key.name}
    </html:link>
 
 <td> <html:link  href="${item.key.url}">
 ${item.key.title}
    </html:link>
 <td>
 ${item.key.namePI} ${item.key.surnamePI}
 <td>
 <c:forEach items="${item.value}" var="prov">

    <html:link  href="/${WEB_PROPERTIES['webapp.path']}/objectDetails.do?id=${prov.id}">
 ${prov.name}
    </html:link>
  <br>
</c:forEach>
<td>
 <im:querylink text="All experiment submissions " skipBuilder="true">
				<query name="" model="genomic"
					view="ModEncodeProject.providers.experimentSubmissions.title ModEncodeProject.providers.experimentSubmissions.design ModEncodeProject.providers.experimentSubmissions.factorName ModEncodeProject.providers.experimentSubmissions.factorType ModEncodeProject.providers.experimentSubmissions.description"
					sortOrder="ModEncodeProject.providers.experimentSubmissions.title">
				<node path="ModEncodeProject" type="ModEncodeProject">
				</node>
				<node path="ModEncodeProject.surnamePI" type="String">
				<constraint op="=" value="${item.key.surnamePI}" description="" identifier=""
					code="A">
				</constraint>
				</node>
				</query>
			</im:querylink>
			
			</c:forEach>

</tr>
</table>
</div>


            <im:querylink text="All experiment submissions " skipBuilder="true">
<query name="" model="genomic" view="ModEncodeProvider.experimentSubmissions.title ModEncodeProvider.experimentSubmissions.design ModEncodeProvider.experimentSubmissions.factorName ModEncodeProvider.experimentSubmissions.factorType ModEncodeProvider.experimentSubmissions.description" sortOrder="ModEncodeProvider.experimentSubmissions.title">
  <node path="ModEncodeProvider" type="ModEncodeProvider">
  </node>
  <node path="ModEncodeProvider.name" type="String">
    <constraint op="=" value="Michael Brent" description="" identifier="" code="A">
    </constraint>
  </node>
</query>

            </im:querylink>


