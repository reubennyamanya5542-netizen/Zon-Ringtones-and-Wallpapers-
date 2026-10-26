p = 'android/app/src/main/AndroidManifest.xml'
s = open(p, encoding='utf-8').read()
perms = ''.join(f'    <uses-permission android:name="android.permission.{x}"/>\n' for x in
  ['INTERNET', 'RECEIVE_BOOT_COMPLETED', 'FLASHLIGHT', 'WRITE_SETTINGS', 'SET_WALLPAPER', 'BLUETOOTH_CONNECT', 'POST_NOTIFICATIONS', 'SYSTEM_ALERT_WINDOW', 'FOREGROUND_SERVICE', 'FOREGROUND_SERVICE_SPECIAL_USE', 'REQUEST_IGNORE_BATTERY_OPTIMIZATIONS', 'WAKE_LOCK'])
svc = '''    <service android:name=".NotifService" android:label="Zon" android:exported="true"
        android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
      <intent-filter><action android:name="android.service.notification.NotificationListenerService"/></intent-filter>
    </service>
    <service android:name=".AodService" android:exported="false" android:foregroundServiceType="specialUse">
      <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="Always-on display shown when the screen turns off"/>
    </service>
    <receiver android:name=".BootReceiver" android:exported="true"><intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/></intent-filter></receiver>
    <service android:name=".WallJob" android:permission="android.permission.BIND_JOB_SERVICE" android:exported="false"/>
    <activity android:name=".AodActivity" android:exported="false" android:launchMode="singleTask" android:excludeFromRecents="true"
        android:showWhenLocked="true" android:turnScreenOn="true" android:theme="@style/AppTheme.NoActionBar"
        android:configChanges="orientation|keyboardHidden|keyboard|screenSize|locale|smallestScreenSize|screenLayout|uiMode"/>
'''
s = s.replace('<application', perms + '    <application', 1).replace('</application>', svc + '    </application>', 1)
open(p, 'w', encoding='utf-8').write(s)
