<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="org.flymine.model.genomic.Protein" %>
<%@ page import="org.intermine.web.logic.results.DisplayObject" %>
<%@ page import="org.intermine.model.InterMineObject" %>

<!-- proteinShortDisplayer.jsp -->
<%
    Object o = request.getAttribute("object");
    if ((o instanceof DisplayObject &&
       ((Protein) ((DisplayObject) o).getObject()) .proxGetSequence() != null) ||
        (o instanceof InterMineObject &&
         ((Protein) o) .proxGetSequence() != null)) {
%>

  <html:xhtml/>
  <html:link action="sequenceExporter?object=${object.id}">
    <html:img styleClass="fasta" src="model/fasta.gif"/>
  </html:link>

<% } %>

<!-- /proteinShortDisplayer.jsp -->
