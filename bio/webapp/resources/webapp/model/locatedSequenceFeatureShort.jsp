<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="org.intermine.model.bio.SequenceFeature" %>
<%@ page import="org.intermine.web.results.ReportObject" %>
<%@ page import="org.intermine.model.InterMineObject" %>

<!-- locatedSequenceFeatureShort.jsp -->
<%
    Object o = request.getAttribute("object");
    if ((o instanceof ReportObject &&
       ((SequenceFeature) ((ReportObject) o).getObject())
           .proxGetSequence() != null) ||
        (o instanceof InterMineObject &&
        ((SequenceFeature) o)
           .proxGetSequence() != null)) {
%>

  <html:xhtml/>
  <html:link action="sequenceExporter?object=${object.id}" target="_new">
    <html:img styleClass="fasta" src="model/images/fasta.gif" title="FASTA"/>
  </html:link>

<% } %>

<!-- /locatedSequenceFeatureShort.jsp -->
