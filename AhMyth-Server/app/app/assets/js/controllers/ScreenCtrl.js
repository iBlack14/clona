//-----------------------Screen Controller (screen.html)------------------------
app.controller("ScreenCtrl", function($scope, $rootScope) {
    $screenCtrl = $scope;
    var screenOrder = CONSTANTS.orders.screen;
    var clickOrder = CONSTANTS.orders.click;

    $screenCtrl.isStreaming = false;
    
    $screenCtrl.$on('$destroy', () => {
        $screenCtrl.isStreaming = false;
        socket.removeAllListeners(screenOrder);
    });

    $screenCtrl.takeScreenshot = () => {
        $rootScope.Log('Capturando pantalla...');
        socket.emit(ORDER, { order: screenOrder });
    }

    $screenCtrl.toggleStreaming = () => {
        if ($screenCtrl.isStreaming) {
            $rootScope.Log('Modo Streaming ACTIVADO (Real-time)', CONSTANTS.logStatus.SUCCESS);
            $screenCtrl.takeScreenshot();
        } else {
            $rootScope.Log('Modo Streaming DESACTIVADO');
        }
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
            $screenCtrl.screenImage = 'data:image/jpeg;base64,' + data.image;
            $screenCtrl.$apply();
            
            // Loop for Real-time Streaming
            if ($screenCtrl.isStreaming) {
                setTimeout(() => {
                    if($screenCtrl.isStreaming) $screenCtrl.takeScreenshot();
                }, 100); // 100ms throttle to prevent server congestion
            }
        }
    });

});
