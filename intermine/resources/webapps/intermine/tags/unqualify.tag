<%@ tag body-content="empty" %>
<%@ attribute name="className" required="true" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

${fn:split(className, ".")[fn:length(fn:split(className, ".")) - 1]}
