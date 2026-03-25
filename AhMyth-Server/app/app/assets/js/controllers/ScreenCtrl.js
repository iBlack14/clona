//-----------------------Screen Controller (screen.html)------------------------
app.controller("ScreenCtrl", function($scope, $rootScope) {
    $screenCtrl = $scope;
    var screenOrder = CONSTANTS.orders.screen;
    var clickOrder = CONSTANTS.orders.click;

    $screenCtrl.$on('$destroy', () => {
        socket.removeAllListeners(screenOrder);
    });

    $screenCtrl.takeScreenshot = () => {
        $rootScope.Log('Capturando pantalla...');
        socket.emit(ORDER, { order: screenOrder });
    }

    $screenCtrl.imgClick = (event) => {
        if (!$screenCtrl.screenImage) return;

        var rect = event.target.getBoundingClientRect();
        var x = event.clientX - rect.left;
        var y = event.clientY - rect.top;

        // Normalize based on actual image dimensions (scaled screen)
        var widthRatio = event.target.naturalWidth / rect.width;
        var heightRatio = event.target.naturalHeight / rect.height;

        var realX = Math.round(x * widthRatio);
        var realY = Math.round(y * heightRatio);

        $rootScope.Log('Enviando clic remoto en (' + realX + ',' + realY + ')');
        socket.emit(ORDER, { order: clickOrder, x: realX, y: realY });
    }

    socket.on(screenOrder, (data) => {
        if (data.image) {
            $rootScope.Log('Captura de pantalla recibida', CONSTANTS.logStatus.SUCCESS);
            $screenCtrl.screenImage = 'data:image/jpeg;base64,' + data.image;
            $screenCtrl.$apply();
        }
    });

});
