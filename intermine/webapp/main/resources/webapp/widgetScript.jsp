<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<script type="text/javascript">
jQuery(document).ready(function() {
    var callbacks = {
        matchCb: function(id, type) {
            window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
        },
        listCb: function(pq) {
            var service = new intermine.Service({'root': window.service, 'token': "<tiles:getAsString name="token"/>"});
            service.query(pq, function(query) {
                var dialogue = new intermine.query.actions.ListCreator(query);
                dialogue.render().$el.appendTo('#<tiles:getAsString name="widgetId"/>-widget');
                dialogue.openDialogue();

                query.on('list-creation:success', window.LIST_EVENTS['list-creation:success']);
                query.on('list-creation:failure', window.LIST_EVENTS['list-creation:failure']);
            });
        },
        resultsCb: function(pq) {
            (new intermine.Service({'root': service})).query(pq, function(query) {						
                window.open(service.replace('/service/', "/run.do") + "?query=" + query.toXML());
                window.focus();
            });
        }
    };
    window.widgets.<tiles:getAsString name="style"/>('<tiles:getAsString name="widgetId"/>', '<tiles:getAsString name="bagName"/>', '#<tiles:getAsString name="widgetId"/>-widget', callbacks);
});
</script>
