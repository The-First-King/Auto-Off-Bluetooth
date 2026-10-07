# Auto Off Bluetooth

Auto Off Bluetooth is a lightweight utility for Android designed to preserve battery life and increase device security. The app monitors your Bluetooth and Wi-Fi connection status and automatically turns off either service when no longer in use.

## How it works

The app listens for Bluetooth state changes in the stack and for Asynchronous Connection-Less disconnection events and Wi-Fi state changes in the background. When a device disconnects, the app starts a timer. If the timer reaches the specified duration without reconnecting, the app automatically turns off the respective service (Bluetooth or Wi-Fi) to save battery and reduce security risks.

The Bluetooth radio turns off automatically after 20 seconds if no devices are connected.

The Wi-Fi radio automatically turns off if it has not been connected to any SSID for the predefined timeout set in the settings, starting after it disconnects from the last connected SSID.

## Screenshots

<div align="center">
  <img src="https://raw.githubusercontent.com/The-First-King/Auto-Off-Bluetooth/refs/heads/master/metadata/en-US/images/phoneScreenshots/01.png" alt="App UI" width="405" />
</div>

## Permissions

The app requires the following permissions to manage your Bluetooth and Wi-Fi hardware on **Android 6.0 (Marshmallow)** or higher:

* `BLUETOOTH`: Allows the app to see the status of Bluetooth connections.
* `BLUETOOTH_ADMIN`: Allows the app to toggle the Bluetooth radio on/off.
* `BLUETOOTH_CONNECT`: To interact with paired devices (required for Android 12+).
* `BLUETOOTH_SCAN`: Required on Android 12+ to monitor Bluetooth state reliably.
* `CHANGE_WIFI_STATE`: Allows the app to toggle Wi-Fi on/off.
* `ACCESS_WIFI_STATE`: Allows the app to monitor Wi-Fi connection state.
* `ACCESS_FINE_LOCATION`: Required on Android 10+ to scan for available Wi-Fi networks and monitor connection status.
* **Disable Battery Optimization**: For the background timers to work accurately, it is recommended to exclude the app from battery "Optimization" via the in-app button.
* **Root Access**: Required on **Android 10 or later** for Wi-Fi toggle functionality.

## Installation & License

<a href="https://github.com/The-First-King/Auto-Off-Bluetooth/releases"><img src="images/GitHub.png" alt="Get it on GitHub" height="60"></a>
<a href="https://apt.izzysoft.de/packages/com.mine.autooffbluetooth"><img src="images/IzzyOnDroid.png" alt="Get it at IzzyOnDroid" height="60"></a>

---

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

---
