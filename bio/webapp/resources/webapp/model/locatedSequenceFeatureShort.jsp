<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="org.flymine.model.genomic.LocatedSequenceFeature" %>
<%@ page import="org.intermine.web.results.DisplayObject" %>
<%@ page import="org.intermine.model.InterMineObject" %>

<!-- locatedSequenceFeatureShort.jsp -->
<%
    Object o = request.getAttribute("object");
    if ((o instanceof DisplayObject &&
       ((LocatedSequenceFeature) ((DisplayObject) o).getObject())
           .proxGetSequence() != null) ||
        (o instanceof InterMineObject &&
        ((LocatedSequenceFeature) o)
           .proxGetSequence() != null)) {
%>

  <html:xhtml/>
  <html:link action="sequenceExporter?object=${object.id}" target="_new">
    <html:img styleClass="fasta" src="model/images/fasta.gif" title="FASTA"/>
  </html:link>

<% } %>

<!-- /locatedSequenceFeatureShort.jsp -->
