
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>
<%@ taglib uri="/WEB-INF/struts-tiles.tld" prefix="tiles" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib tagdir="/WEB-INF/tags" prefix="im"%>

<!-- dataSetIcon.jsp -->

<html:xhtml/>
<img src="${DATASETS[dataSet.name].iconImage}" width="18" height="18" class="dsSmallIconImage" align="top"/>

<span class="dataSetTitle">${dataSet.name}</span>

<!-- /dataSetIcon.jsp -->
