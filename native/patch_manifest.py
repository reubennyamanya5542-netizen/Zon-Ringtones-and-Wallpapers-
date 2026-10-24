p = 'android/app/src/main/AndroidManifest.xml'
s = open(p, encoding='utf-8').read()
perms = ''.join(f'    <uses-permission android:name="android.permission.{x}"/>\n' for x in
  ['INTERNET', 'FLASHLIGHT', 'WRITE_SETTINGS', 'SET_WALLPAPER', 'BLUETOOTH_CONNECT', 'POST_NOTIFICATIONS'])
svc = '''    <service android:name=".NotifService" android:label="Zon" android:exported="true"
        android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
      <intent-filter><action android:name="android.service.notification.NotificationListenerService"/></intent-filter>
    </service>
'''
s = s.replace('<application', perms + '    <application', 1).replace('</application>', svc + '    </application>', 1)
open(p, 'w', encoding='utf-8').write(s)
