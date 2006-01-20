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
           .proxGetSequence().getId() != null) ||
        (o instanceof InterMineObject &&
        ((LocatedSequenceFeature) o)
           .proxGetSequence().getId() != null)) {
%>

  <html:xhtml/>
  <html:link action="sequenceExporter?object=${object.id}">
    <html:img styleClass="fasta" src="model/fasta.gif"/>
  </html:link>

<% } %>

<!-- /locatedSequenceFeatureShort.jsp -->
