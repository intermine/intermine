<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!-- locatedSequenceFeatureShort.jsp -->
<c:if test="${(object.class.name == 'org.intermine.web.results.DisplayObject'
               && object.object.sequence != null) 
              ||
              (object.class.name != 'org.intermine.web.results.DisplayObject'
               && object.sequence != null)}">
  <html:xhtml/>
  <html:link action="sequenceExporter?object=${object.id}">
    <html:img styleClass="fasta" src="model/fasta.gif"/>
  </html:link>
</c:if>
<!-- /locatedSequenceFeatureShort.jsp -->
