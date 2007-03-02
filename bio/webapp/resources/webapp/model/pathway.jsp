<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- pathway.jsp -->

<html:xhtml/>
    <html:link href="http://www.genome.jp/dbget-bin/show_pathway?${fn:replace(interMineObject.identifier,'dme','map')}">${interMineObject.name}</html:link>

<!-- /pathway.jsp -->