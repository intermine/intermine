<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/struts-html.tld" prefix="html" %>

<!-- locatedSequenceFeatureImage.jsp -->
<fmt:setBundle basename="model"/>

<html:link href="${WEB_PROPERTIES['gbrowse.prefix']}/${WEB_PROPERTIES['gbrowse.source']}?name=${cld.unqualifiedName}:FlyMineInternalID_${object.id};width=800">
  <div>
    <fmt:message key="locatedSequenceFeature.GBrowse.message"/>
  </div>
  <div>
    <html:img src="${WEB_PROPERTIES['gbrowse_image.prefix']}/${WEB_PROPERTIES['gbrowse.source']}?name=${cld.unqualifiedName}:FlyMineInternalID_${object.id};width=400"/>
  </div>
</html:link>
<!-- /locatedSequenceFeatureImage.jsp -->
