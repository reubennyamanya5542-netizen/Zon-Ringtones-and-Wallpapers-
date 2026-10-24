package com.zon.app;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
public class AodActivity extends MainActivity {
  @Override public void onCreate(Bundle b) {
    setShowWhenLocked(true);
    setTurnScreenOn(true);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    WindowManager.LayoutParams p = getWindow().getAttributes();
    p.screenBrightness = 0.01f;
    getWindow().setAttributes(p);
    super.onCreate(b);
    getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    getBridge().getWebView().loadUrl("https://localhost/index.html#aod");
  }
}
