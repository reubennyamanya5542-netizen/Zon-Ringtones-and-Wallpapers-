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
    immersive(this, true);
    getBridge().getWebView().loadUrl("https://localhost/index.html#aod");
  }
}
