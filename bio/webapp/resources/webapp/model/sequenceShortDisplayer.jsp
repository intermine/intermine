<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html:xhtml/>
<html:link action="sequenceExporter?object=${object.id}" target="_new">
  <html:img styleClass="fasta" src="model/images/fasta.gif" title="FASTA" />
</html:link>
