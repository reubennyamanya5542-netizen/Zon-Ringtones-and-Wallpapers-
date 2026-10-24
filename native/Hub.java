package com.zon.app;
import android.webkit.WebView;
public class Hub {
  static WebView web;
  static void js(String code) { final WebView w = web; if (w != null) w.post(() -> { try { w.evaluateJavascript(code, null); } catch (Exception e) {} }); }
}
