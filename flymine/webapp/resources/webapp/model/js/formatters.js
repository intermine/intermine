(function(intermine) {
    var formatChrLoc = function(feature, start, end) {
        return "chr" + feature + ": " + start + ".." + end;
    };

    intermine.scope('intermine.results.formatters', {
        Location: function(model, query, $cell) {
            var id, start, end, feature, displayText;
            id = model.get('id');
            if (model.has('start') && model.has('end') && model.has('_located_on')) {
                // Can use pre-cached values...
                start = model.get('start');
                end = model.get('end');
                feature = model.get('_located_on');
                return {value: formatChrLoc(feature, start, end), field: 'id'};
            } else {
                query.service.findById("Location", id, function(location) {
                    feature = location.locatedOn.primaryIdentifier;
                    start = location.start;
                    end = location.end;
                    displayText = formatChrLoc(feature, start, end);
                    model.set({
                        start: start,
                        end: end,
                        _located_on: feature
                    });
                    $cell.find('.im-cell-link').text(displayText);
                });
                return {value: id, field: 'id'};
            }
        }
    });
}).call(window, intermine);
