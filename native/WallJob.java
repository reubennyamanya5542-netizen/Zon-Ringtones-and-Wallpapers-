package com.zon.app;

import android.app.WallpaperManager;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;

// Changes the wallpaper on a schedule. Android runs this when the phone allows it, so it respects battery saving.
public class WallJob extends JobService {
  @Override public boolean onStartJob(final JobParameters p) {
    new Thread(() -> { try { run(); } catch (Throwable e) { /* try again next time */ } jobFinished(p, false); }).start();
    return true;
  }
  @Override public boolean onStopJob(JobParameters p) { return true; }

  private void run() throws Exception {
    SharedPreferences sp = getSharedPreferences("zonwall", MODE_PRIVATE);
    JSONArray a = new JSONArray(sp.getString("urls", "[]"));
    if (a.length() < 1) return;
    int i = (sp.getInt("i", -1) + 1) % a.length();
    String where = sp.getString("where", "both");
    HttpURLConnection h = (HttpURLConnection) new URL(a.getString(i)).openConnection();
    h.setConnectTimeout(15000); h.setReadTimeout(30000);
    byte[] data;
    try (InputStream in = h.getInputStream()) {
      ByteArrayOutputStream o = new ByteArrayOutputStream(); byte[] buf = new byte[16384]; int n;
      while ((n = in.read(buf)) > 0) o.write(buf, 0, n);
      data = o.toByteArray();
    } finally { h.disconnect(); }
    DisplayMetrics dm = getResources().getDisplayMetrics(); int tw = dm.widthPixels, th = dm.heightPixels;
    BitmapFactory.Options op = new BitmapFactory.Options(); op.inJustDecodeBounds = true;
    BitmapFactory.decodeByteArray(data, 0, data.length, op);
    int s = 1; while (op.outWidth / (s * 2) >= tw && op.outHeight / (s * 2) >= th) s *= 2;
    op = new BitmapFactory.Options(); op.inSampleSize = s;
    Bitmap src = BitmapFactory.decodeByteArray(data, 0, data.length, op);
    if (src == null) return;
    // centre-crop to the screen shape so nothing is stretched
    float want = (float) tw / th, have = (float) src.getWidth() / src.getHeight();
    int cw = src.getWidth(), ch = src.getHeight(), cx = 0, cy = 0;
    if (have > want) { cw = Math.round(ch * want); cx = (src.getWidth() - cw) / 2; } else { ch = Math.round(cw / want); cy = (src.getHeight() - ch) / 2; }
    Bitmap out = Bitmap.createBitmap(src, cx, cy, cw, ch);
    int f = "home".equals(where) ? WallpaperManager.FLAG_SYSTEM : "lock".equals(where) ? WallpaperManager.FLAG_LOCK : (WallpaperManager.FLAG_SYSTEM | WallpaperManager.FLAG_LOCK);
    WallpaperManager.getInstance(this).setBitmap(out, null, true, f);
    sp.edit().putInt("i", i).apply();
  }
}
