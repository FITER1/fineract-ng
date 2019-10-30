define(['underscore'], function () {
    var styles = {
        css: [
            'fontawesome.a912fc97.css',
            'styles.2a5e504b.css'
            ]
    };

    require(_.reduce(_.keys(styles), function (list, pluginName) {
        return list.concat(_.map(styles[pluginName], function (stylename) {
            return pluginName + '!styles/' + stylename;
        }));
    }, []));
});
