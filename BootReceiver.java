package com.zon.app;
import android.content.*;
import android.provider.Settings;
public class BootReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context c, Intent i) {
    if (c.getSharedPreferences("zon", 0).getBoolean("bg_on", false) && Settings.canDrawOverlays(c)) {
      try { c.startForegroundService(new Intent(c, AodService.class)); } catch (Exception e) {}
    }
  }
}
