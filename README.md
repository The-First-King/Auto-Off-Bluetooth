# Auto Off Bluetooth

Auto Off Bluetooth is a lightweight utility for Android designed to preserve battery life and increase device security. The app monitors your Bluetooth connection status and automatically turns off the Bluetooth radio after 20 seconds if no devices are connected.

## How it works

The app listens for Bluetooth state changes in the stack and for Asynchronous Connection-Less disconnection events in the background. When a device disconnects, the app starts a timer. If the timer expires without a reconnection, the app automatically disables the Bluetooth adapter to save power. If a Bluetooth device reconnects, the app postpones the Auto Off task until the next disconnect.
   
## Screenshots

<div align="center">
  <img src="https://github.com/The-First-King/Auto-Off-Bluetooth/blob/master/metadata/en-US/images/phoneScreenshots/01.png?raw=true" alt="App UI" width="405" />
</div>

## Permissions

The app requires the following permissions to manage your Bluetooth hardware:

* `BLUETOOTH`: Allows the app to see the status of connections.
* `BLUETOOTH_ADMIN`: Allows the app to toggle the Bluetooth radio on/off.
* `BLUETOOTH_CONNECT`: To interact with paired devices (required for Android 12+).

## Installation & License

<a href="https://github.com/The-First-King/Auto-Off-Bluetooth/releases"><img src="images/GitHub.png" alt="Get it on GitHub" height="60"></a>
<a href="https://apt.izzysoft.de/packages/com.mine.autooffbluetooth"><img src="images/IzzyOnDroid.png" alt="Get it at IzzyOnDroid" height="60"></a>

---

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

---
