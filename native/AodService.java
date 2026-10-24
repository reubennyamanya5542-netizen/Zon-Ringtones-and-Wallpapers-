package com.zon.app;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
public class AodService extends Service {
  boolean reg = false;
  final BroadcastReceiver r = new BroadcastReceiver() {
    @Override public void onReceive(Context c, Intent i) {
      try { c.startActivity(new Intent(c, AodActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)); } catch (Exception e) {}
    }
  };
  @Override public int onStartCommand(Intent i, int f, int id) {
    NotificationManager nm = getSystemService(NotificationManager.class);
    nm.createNotificationChannel(new NotificationChannel("zon_aod", "Zon AOD", NotificationManager.IMPORTANCE_MIN));
    Notification n = new Notification.Builder(this, "zon_aod").setContentTitle("Zon AOD is on")
      .setContentText("Shows when the screen turns off").setSmallIcon(android.R.drawable.ic_lock_idle_lock).setOngoing(true).build();
    startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
    if (!reg) { registerReceiver(r, new IntentFilter(Intent.ACTION_SCREEN_OFF)); reg = true; }
    return START_STICKY;
  }
  @Override public void onDestroy() { if (reg) { unregisterReceiver(r); reg = false; } super.onDestroy(); }
  @Override public IBinder onBind(Intent i) { return null; }
}
