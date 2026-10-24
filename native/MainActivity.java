package com.zon.app;
import android.os.Bundle;
import android.webkit.WebView;
import com.getcapacitor.BridgeActivity;
public class MainActivity extends BridgeActivity {
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    WebView w = getBridge().getWebView();
    Hub.web = w;
    w.addJavascriptInterface(new ZonBridge(this), "ZonNative");
  }
}
