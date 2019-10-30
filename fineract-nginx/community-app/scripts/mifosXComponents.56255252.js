define(['Q', 'underscore', 'mifosX'], function (Q) {
    var components = {
        models: [
            'models.eee211fa'
        ],
        services: [
            'ResourceFactoryProvider',
            'HttpServiceProvider',
            'AuthenticationService',
            'SessionManager',
            'Paginator',
            'UIConfigService',
            'NotificationResponseHeaderProvider'
        ],
        controllers: [
            'controllers.eeb13efe'
        ],
        filters: [
            'filters.01aa453b'
        ],
        directives: [
            'directives.0071dae2'
        ]
    };

    return function() {
        var defer = Q.defer();
        require(_.reduce(_.keys(components), function (list, group) {
            return list.concat(_.map(components[group], function (name) {
                return group + "/" + name;
            }));
        }, [
            'routes-initialTasks-webstorage-configuration.7acd6a2b'
        ]), function(){
            defer.resolve();
        });
        return defer.promise;
    }
});
