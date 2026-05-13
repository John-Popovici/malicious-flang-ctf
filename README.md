
> [!Caution]
> This is a copy of Jannis's FlangAndroid application.
> 
> This project intentionally contains both malicious code and flags in the format "FLAG{flag_here}".

---

# Malicious Flang Android

## The App
This repository contains a copy of Jannis's FlangAndroid application at commit `40a71fa489` in `app/` used under the GNUv3 license. It intentionally includes malicious code, flags to be found, and vulnerabilities for educational purposes. The application's original README.md with the added disclaimer can be found in app/README.md. The original source code is available at: https://codeberg.org/jannis/FlangAndroid

To launch the app, open the `app/` directory in Android Studio and run the app on an emulator or a physical device.

## The Server
The server code is located in the `server/` directory and serves as a server for requests. It should NOT be analyzed, and doing so would constitute a violation of the rules of this challenge. The server represents a remoteserver that the application would connect to.

To launch the server, run the following command in the `server/` directory:

```bash
npm install
npm rebuild # if you are on a different platform
node server.js
```
