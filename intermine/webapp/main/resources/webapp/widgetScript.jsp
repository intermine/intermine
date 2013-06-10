<%@ taglib uri="http://struts.apache.org/tags-tiles" prefix="tiles" %>
<script type="text/javascript">
jQuery(document).ready(function() {
    var opts = {
        matchCb: function(id, type) {
            window.open(window.service.replace('/service/', '/portal.do?class=' + type + "&externalids=" + id));
        },
        listCb: function(pq) {
            var service = new intermine.Service({'root': window.service, 'token': "<tiles:getAsString name="token"/>"});
            service.query(pq, function(query) {
                var dialogue = new intermine.query.actions.ListCreator(query);
                dialogue.render().$el.appendTo('#<tiles:getAsString name="widgetId"/>-widget');
                dialogue.openDialogue();

                var notify = {
                    'success': function(list, change) {
                        window.LIST_EVENTS['list-creation:success'](list, change);
                        window.scrollTo(0, 0);
                    },
                    'failure': function(list, change) {
                        window.LIST_EVENTS['list-creation:failure'](list, change);
                        window.scrollTo(0, 0);
                    }
                };

                query.on('list-creation:success', notify.success);
                query.on('list-creation:failure', notify.failure);
            });
        },
        resultsCb: function(pq) {
            (new intermine.Service({'root': service})).query(pq, function(query) {
                var target, uri, form, w;

                // Generate the target name.
                target = 'tmp' + +new Date();

                // Open the window.
                w = window.open('', target);

                // We will be posting here.
                uri = service.replace('/service/', '/run.do');
                // Create a form.
                form = jQuery('<form>', { 'method': 'POST', 'action': uri, 'target': target });
                // Append the field.
                form.append(jQuery('<input>', { 'type': 'hidden', 'value': query.toXML(), 'name': 'query' }));
                // Append the form, submit & remove.
                form.appendTo('body').submit().remove();

                // Give the window focus.
                w.focus();
            });
        }
    };
    window.widgets.<tiles:getAsString name="style"/>('<tiles:getAsString name="widgetId"/>', '<tiles:getAsString name="bagName"/>', '#<tiles:getAsString name="widgetId"/>-widget', opts);
});
</script>
