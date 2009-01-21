<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- pathways.jsp -->

<html:xhtml/>
    <html:link href="http://www.genome.jp/dbget-bin/show_pathway?${fn:replace(interMineObject.identifier,'dme','map')}">${interMineObject.name}</html:link>

<!-- /pathways.jsp -->
