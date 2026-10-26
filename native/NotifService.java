package com.zon.app;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.*;
import android.service.notification.*;
import android.util.Base64;
import org.json.*;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;

public class NotifService extends NotificationListenerService {
  static MediaController mc;
  static NotifService inst;
  static String lastTrack, lastNotifs;
  static void sync(Context c) {
    if (inst != null) { inst.push(); inst.send(); }
    else if (android.os.Build.VERSION.SDK_INT >= 24) NotificationListenerService.requestRebind(new ComponentName(c, NotifService.class));
  }
  @Override public void onListenerDisconnected() { inst = null; }
  MediaController cur;
  List<MediaController> ctrls = new ArrayList<>();
  long lostAt = 0;
  final MediaController.Callback cb = new MediaController.Callback() {
    @Override public void onMetadataChanged(MediaMetadata m) { choose(); }
    @Override public void onPlaybackStateChanged(PlaybackState p) { choose(); }
  };
  @Override public void onListenerConnected() {
    inst = this;
    push();
    try {
      MediaSessionManager m = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
      ComponentName cn = new ComponentName(this, NotifService.class);
      m.addOnActiveSessionsChangedListener(this::pick, cn);
      pick(m.getActiveSessions(cn));
    } catch (Exception e) {}
  }
  @Override public void onNotificationPosted(StatusBarNotification s) { push(); }
  @Override public void onNotificationRemoved(StatusBarNotification s) { push(); }

  void push() {
    try {
      StatusBarNotification[] all = getActiveNotifications();
      Arrays.sort(all, (x, y) -> Long.compare(y.getPostTime(), x.getPostTime()));
      PackageManager pm = getPackageManager(); JSONArray a = new JSONArray();
      for (StatusBarNotification s : all) {
        Notification n = s.getNotification();
        if (s.getPackageName().equals(getPackageName()) || (n.flags & (Notification.FLAG_ONGOING_EVENT | Notification.FLAG_GROUP_SUMMARY)) != 0 || Notification.CATEGORY_TRANSPORT.equals(n.category)) continue;
        CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TITLE), x = n.extras.getCharSequence(Notification.EXTRA_TEXT);
        if (t == null && x == null) continue;
        String app = label(pm, s);
        JSONObject o = new JSONObject();
        o.put("app", app); o.put("text", (t != null ? t : "") + (t != null && x != null ? ": " : "") + (x != null ? x : ""));
        o.put("time", new SimpleDateFormat("HH:mm").format(new Date(s.getPostTime()))); o.put("pkg", s.getPackageName());
        if (n.color != 0) o.put("color", String.format("#%06X", 0xFFFFFF & n.color));
        a.put(o);
      }
      lastNotifs = a.toString();
      Hub.js("window.zonSetNotifs&&zonSetNotifs(" + a + ")");
    } catch (Exception e) {}
  }
  void pick(List<MediaController> l) {
    for (MediaController m : ctrls) { try { m.unregisterCallback(cb); } catch (Exception e) { } }
    ctrls = l == null ? new ArrayList<MediaController>() : new ArrayList<>(l);
    for (MediaController m : ctrls) { try { m.registerCallback(cb); } catch (Exception e) { } }
    choose();
  }
  /** Prefer the player that is playing; otherwise any player that has a song loaded. */
  void choose() {
    MediaController best = null;
    for (MediaController m : ctrls) { PlaybackState p = m.getPlaybackState(); if (p != null && p.getState() == PlaybackState.STATE_PLAYING && m.getMetadata() != null) { best = m; break; } }
    if (best == null) for (MediaController m : ctrls) if (m.getMetadata() != null) { best = m; break; }
    cur = best; mc = best; send();
  }
  static String pretty(String pkg) {
    String[] p = pkg.split("\\."); int i = (p.length > 1 && p[0].matches("com|org|net|io|app|me|co")) ? 1 : 0;
    StringBuilder b = new StringBuilder();
    for (int k = i; k < p.length; k++) { if (p[k].isEmpty()) continue; if (b.length() > 0) b.append(' '); b.append(Character.toUpperCase(p[k].charAt(0))).append(p[k].substring(1)); }
    return b.length() > 0 ? b.toString() : pkg;
  }
  /** The real app name (Opera, WhatsApp...), never a web-style package id. */
  String label(PackageManager pm, StatusBarNotification s) {
    String pkg = s.getPackageName();
    try { return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); } catch (Exception e) { }
    try {
      android.content.pm.ApplicationInfo ai = s.getNotification().extras.getParcelable("android.appInfo");
      if (ai != null) { CharSequence l = pm.getApplicationLabel(ai); if (l != null && !l.toString().equals(pkg)) return l.toString(); }
    } catch (Exception e) { }
    return pretty(pkg);
  }
  void send() {
    try {
      MediaMetadata m = cur == null ? null : cur.getMetadata();
      if (m == null) {
        if (lastTrack != null && !lastTrack.isEmpty() && System.currentTimeMillis() - lostAt < 15 * 60000L) {
          if (lostAt == 0) lostAt = System.currentTimeMillis();
          JSONObject o = new JSONObject(lastTrack); o.put("playing", false); lastTrack = o.toString();
          Hub.js("window.zonSetTrack&&zonSetTrack(" + lastTrack + ")"); return;
        }
        lastTrack = ""; Hub.js("window.zonSetTrack&&zonSetTrack(null)"); return;
      }
      lostAt = System.currentTimeMillis();
      PlaybackState p = cur.getPlaybackState(); JSONObject o = new JSONObject();
      String title = m.getString(MediaMetadata.METADATA_KEY_TITLE);
      if (title == null) title = m.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
      if (title == null) title = "Unknown";
      o.put("title", title);
      String ar = m.getString(MediaMetadata.METADATA_KEY_ARTIST); if (ar == null) ar = m.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST); o.put("artist", ar == null ? "" : ar);
      o.put("playing", p != null && p.getState() == PlaybackState.STATE_PLAYING);
      o.put("pos", p == null ? 0 : p.getPosition() / 1000); o.put("dur", m.getLong(MediaMetadata.METADATA_KEY_DURATION) / 1000);
      Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART); if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
      if (b != null) { Bitmap sm = Bitmap.createScaledBitmap(b, 200, 200, true); ByteArrayOutputStream bo = new ByteArrayOutputStream(); sm.compress(Bitmap.CompressFormat.JPEG, 80, bo); o.put("art", "data:image/jpeg;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)); }
      lastTrack = o.toString();
      Hub.js("window.zonSetTrack&&zonSetTrack(" + lastTrack + ")");
    } catch (Exception e) {}
  }
}
