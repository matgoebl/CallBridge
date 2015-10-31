# WebIntentBridge
Android Web Intent Bridge using NanoHTTPD

## Web API
- base url: http://android-ip:8020/intent?
- parameters for specific action (choose one):
  - callNo=12345
  - openUrl=http://www.google.de
  - launchActivity=com.android.deskclock/.DeskClock
- additional optional parameters:
  - wakeupDevice=on

Example:
`curl -s 'http://192.168.0.100:8020/intent?openUrl=https://www.google.de&wakeupDevice=on'`
