package com.zon.app;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
public class AodActivity extends MainActivity {
  android.hardware.SensorManager sm; android.hardware.Sensor ps;
  final android.hardware.SensorEventListener sl = new android.hardware.SensorEventListener() {
    @Override public void onSensorChanged(android.hardware.SensorEvent e) { Hub.js("window.zonPocket&&zonPocket(" + (e.values[0] < Math.min(5f, ps.getMaximumRange())) + ")"); }
    @Override public void onAccuracyChanged(android.hardware.Sensor s, int a) {}
  };
  @Override public void onResume() { super.onResume(); sm = (android.hardware.SensorManager) getSystemService(SENSOR_SERVICE); ps = sm.getDefaultSensor(android.hardware.Sensor.TYPE_PROXIMITY); if (ps != null) sm.registerListener(sl, ps, android.hardware.SensorManager.SENSOR_DELAY_NORMAL); }
  @Override public void onPause() { super.onPause(); if (sm != null) sm.unregisterListener(sl); }
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
