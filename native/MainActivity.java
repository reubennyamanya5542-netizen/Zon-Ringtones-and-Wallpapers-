package com.zon.app;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity {
  static void immersive(Activity a, boolean on) {
    Window w = a.getWindow();
    WindowManager.LayoutParams lp = w.getAttributes();
    if (on) {
      lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
      w.setAttributes(lp);
      w.setStatusBarColor(Color.BLACK); w.setNavigationBarColor(Color.BLACK);
      w.getDecorView().setBackgroundColor(Color.BLACK);
      if (Build.VERSION.SDK_INT >= 30) {
        w.setDecorFitsSystemWindows(false);
        WindowInsetsController c = w.getInsetsController();
        if (c != null) { c.hide(WindowInsets.Type.systemBars()); c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE); }
      } else {
        w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
      }
    } else {
      lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
      w.setAttributes(lp);
      if (Build.VERSION.SDK_INT >= 30) {
        w.setDecorFitsSystemWindows(true);
        WindowInsetsController c = w.getInsetsController();
        if (c != null) c.show(WindowInsets.Type.systemBars());
      } else w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
  }
  @Override public void onResume() { super.onResume(); Hub.web = getBridge().getWebView(); }
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    WebView w = getBridge().getWebView();
    Hub.web = w;
    w.addJavascriptInterface(new ZonBridge(this), "ZonNative");
  }
}
