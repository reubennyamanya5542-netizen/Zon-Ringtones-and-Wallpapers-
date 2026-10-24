package com.zon.app;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
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
  static void sync(Context c) {
    if (inst != null) { inst.push(); inst.send(); }
    else if (Build.VERSION.SDK_INT >= 24) { try { requestRebind(new ComponentName(c, NotifService.class)); } catch (Exception e) {} }
  }
  @Override public void onListenerDisconnected() { if (inst == this) inst = null; }
  MediaController cur;
  final MediaController.Callback cb = new MediaController.Callback() {
    @Override public void onMetadataChanged(MediaMetadata m) { send(); }
    @Override public void onPlaybackStateChanged(PlaybackState p) { send(); }
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
        String app; try { app = pm.getApplicationLabel(pm.getApplicationInfo(s.getPackageName(), 0)).toString(); } catch (Exception e) { app = s.getPackageName(); }
        JSONObject o = new JSONObject();
        o.put("app", app); o.put("text", (t != null ? t : "") + (t != null && x != null ? ": " : "") + (x != null ? x : ""));
        o.put("time", new SimpleDateFormat("HH:mm").format(new Date(s.getPostTime()))); o.put("pkg", s.getPackageName());
        if (n.color != 0) o.put("color", String.format("#%06X", 0xFFFFFF & n.color));
        a.put(o);
      }
      Hub.js("window.zonSetNotifs&&zonSetNotifs(" + a + ")");
    } catch (Exception e) {}
  }
  void pick(List<MediaController> l) {
    if (cur != null) cur.unregisterCallback(cb);
    cur = (l == null || l.isEmpty()) ? null : l.get(0); mc = cur;
    if (cur != null) cur.registerCallback(cb);
    send();
  }
  void send() {
    try {
      MediaMetadata m = cur == null ? null : cur.getMetadata();
      if (m == null) { Hub.js("window.zonSetTrack&&zonSetTrack(null)"); return; }
      PlaybackState p = cur.getPlaybackState(); JSONObject o = new JSONObject();
      o.put("title", String.valueOf(m.getString(MediaMetadata.METADATA_KEY_TITLE)));
      String ar = m.getString(MediaMetadata.METADATA_KEY_ARTIST); o.put("artist", ar == null ? "" : ar);
      o.put("playing", p != null && p.getState() == PlaybackState.STATE_PLAYING);
      o.put("pos", p == null ? 0 : p.getPosition() / 1000); o.put("dur", m.getLong(MediaMetadata.METADATA_KEY_DURATION) / 1000);
      Bitmap b = m.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART); if (b == null) b = m.getBitmap(MediaMetadata.METADATA_KEY_ART);
      if (b != null) { Bitmap sm = Bitmap.createScaledBitmap(b, 200, 200, true); ByteArrayOutputStream bo = new ByteArrayOutputStream(); sm.compress(Bitmap.CompressFormat.JPEG, 80, bo); o.put("art", "data:image/jpeg;base64," + Base64.encodeToString(bo.toByteArray(), Base64.NO_WRAP)); }
      Hub.js("window.zonSetTrack&&zonSetTrack(" + o + ")");
    } catch (Exception e) {}
  }
}
