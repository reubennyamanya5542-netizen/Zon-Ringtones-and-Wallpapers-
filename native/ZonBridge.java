package com.zon.app;
import android.app.Activity;
import android.app.WallpaperManager;
import android.content.*;
import android.hardware.camera2.CameraManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.widget.Toast;
import java.io.*;
import java.net.URL;

public class ZonBridge {
  final Activity a; final Context c;
  ZonBridge(Activity act) { a = act; c = act.getApplicationContext(); }
  void ui(Runnable r) { new Handler(Looper.getMainLooper()).post(r); }
  void toast(String t) { ui(() -> Toast.makeText(c, t, Toast.LENGTH_LONG).show()); }
  void start(Intent i) { try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); } catch (Exception e) { toast("Not available"); } }

  @JavascriptInterface public void flashlight(boolean on) {
    try { CameraManager m = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE); m.setTorchMode(m.getCameraIdList()[0], on); }
    catch (Exception e) { toast("Flashlight is not available"); }
  }
  @JavascriptInterface public void openCamera() { start(new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)); }
  @JavascriptInterface public void openDialer() { start(new Intent(Intent.ACTION_DIAL)); }
  @JavascriptInterface public void openMessages() { start(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)); }
  @JavascriptInterface public void openApp(String pkg) {
    Intent i = c.getPackageManager().getLaunchIntentForPackage(pkg);
    if (i != null) start(i); else toast("App not found");
  }
  @JavascriptInterface public void playPause() {
    android.media.session.MediaController m = NotifService.mc;
    if (m == null) return;
    android.media.session.PlaybackState p = m.getPlaybackState();
    if (p != null && p.getState() == android.media.session.PlaybackState.STATE_PLAYING) m.getTransportControls().pause(); else m.getTransportControls().play();
  }
  @JavascriptInterface public void setAutoRotate(boolean on) {
    ui(() -> a.setRequestedOrientation(on ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
  }
  @JavascriptInterface public void setBackground(boolean on) {
    if (!on) { c.stopService(new Intent(c, AodService.class)); return; }
    if (!Settings.canDrawOverlays(c)) {
      ui(() -> start(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + c.getPackageName()))));
      toast("Allow 'Display over other apps', then open Zon again"); return;
    }
    c.startForegroundService(new Intent(c, AodService.class));
    PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
    if (!pm.isIgnoringBatteryOptimizations(c.getPackageName()))
      ui(() -> start(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + c.getPackageName()))));
  }
  @JavascriptInterface public void finishAod() { ui(() -> { if (a instanceof AodActivity) a.finishAndRemoveTask(); }); }
  @JavascriptInterface public void exitApp() { ui(() -> a.finishAffinity()); }

  String mime(String n) { String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(n.substring(n.lastIndexOf('.') + 1).toLowerCase()); return m != null ? m : "application/octet-stream"; }
  Uri save(String url, Uri table, String rel, ContentValues extra) throws Exception {
    String name = url.substring(url.lastIndexOf('/') + 1).split("\\?")[0];
    ContentValues v = new ContentValues(extra);
    v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
    v.put(MediaStore.MediaColumns.MIME_TYPE, mime(name));
    v.put(MediaStore.MediaColumns.RELATIVE_PATH, rel);
    Uri u = c.getContentResolver().insert(table, v);
    try (InputStream in = new URL(url).openStream(); OutputStream o = c.getContentResolver().openOutputStream(u)) {
      byte[] b = new byte[8192]; int n; while ((n = in.read(b)) > 0) o.write(b, 0, n);
    }
    return u;
  }
  @JavascriptInterface public void setAudio(String id, String as, String s, String e, String url) {
    new Thread(() -> { try {
      if (!Settings.System.canWrite(c)) {
        ui(() -> start(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.getPackageName()))));
        toast("Allow 'Modify system settings', then try again"); return;
      }
      String low = as.toLowerCase(); ContentValues x = new ContentValues(); int type; String dir;
      if (low.contains("notif")) { type = RingtoneManager.TYPE_NOTIFICATION; dir = "Notifications"; x.put(MediaStore.Audio.Media.IS_NOTIFICATION, 1); }
      else if (low.contains("alarm")) { type = RingtoneManager.TYPE_ALARM; dir = "Alarms"; x.put(MediaStore.Audio.Media.IS_ALARM, 1); }
      else { type = RingtoneManager.TYPE_RINGTONE; dir = "Ringtones"; x.put(MediaStore.Audio.Media.IS_RINGTONE, 1); }
      Uri u = save(url, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, dir + "/Zon", x);
      RingtoneManager.setActualDefaultRingtoneUri(c, type, u);
      toast("Done");
    } catch (Exception ex) { toast("Failed: " + ex.getMessage()); } }).start();
  }
  @JavascriptInterface public void setWallpaper(String id, String where, String url) {
    new Thread(() -> { try {
      String low = where.toLowerCase(); int sys = WallpaperManager.FLAG_SYSTEM, lock = WallpaperManager.FLAG_LOCK;
      int f = low.contains("both") ? (sys | lock) : low.contains("lock") ? lock : low.contains("home") ? sys : (sys | lock);
      try (InputStream in = new URL(url).openStream()) { WallpaperManager.getInstance(c).setStream(in, null, true, f); }
    } catch (Exception ex) { toast("Failed: " + ex.getMessage()); } }).start();
  }
  @JavascriptInterface public void exportItem(String id, String url) {
    new Thread(() -> { try {
      boolean audio = url.matches("(?i).*\\.(mp3|m4a|ogg|wav|aac|opus)(\\?.*)?$");
      save(url, audio ? MediaStore.Audio.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, (audio ? "Music" : "Pictures") + "/Zon", new ContentValues());
      toast("Saved to " + (audio ? "Music/Zon" : "Pictures/Zon"));
    } catch (Exception ex) { toast("Failed: " + ex.getMessage()); } }).start();
  }
}
