package com.zon.app;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Cuts a part out of any audio file Android can decode (mp3, m4a, ogg, wav...) and returns it as a WAV file. */
public class ClipMaker {
  static File clip(File dir, InputStream in, int startPct, int endPct) throws Exception {
    File src = File.createTempFile("zon_in", ".bin", dir);
    File pcm = File.createTempFile("zon_pcm", ".raw", dir);
    File wav = File.createTempFile("zon_clip", ".wav", dir);
    MediaExtractor ex = new MediaExtractor();
    MediaCodec cd = null;
    try {
      try (OutputStream o = new FileOutputStream(src)) {
        byte[] b = new byte[16384]; int n;
        while ((n = in.read(b)) > 0) o.write(b, 0, n);
      } finally { in.close(); }

      ex.setDataSource(src.getAbsolutePath());
      int track = -1; MediaFormat f = null;
      for (int i = 0; i < ex.getTrackCount(); i++) {
        MediaFormat tf = ex.getTrackFormat(i);
        String m = tf.getString(MediaFormat.KEY_MIME);
        if (m != null && m.startsWith("audio/")) { track = i; f = tf; break; }
      }
      if (track < 0) throw new Exception("No audio found");
      long dur = f.containsKey(MediaFormat.KEY_DURATION) ? f.getLong(MediaFormat.KEY_DURATION) : 0;
      if (dur <= 0) throw new Exception("Unknown length");
      long st = dur * startPct / 100, en = dur * endPct / 100;
      int rate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE), ch = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT);

      ex.selectTrack(track);
      ex.seekTo(st, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
      cd = MediaCodec.createDecoderByType(f.getString(MediaFormat.KEY_MIME));
      cd.configure(f, null, null, 0);
      cd.start();

      MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
      boolean inDone = false, outDone = false;
      long bytes = 0, deadline = System.currentTimeMillis() + 120000;
      try (OutputStream o = new BufferedOutputStream(new FileOutputStream(pcm))) {
        while (!outDone) {
          if (System.currentTimeMillis() > deadline) throw new Exception("Took too long");
          if (!inDone) {
            int ii = cd.dequeueInputBuffer(10000);
            if (ii >= 0) {
              ByteBuffer ib = cd.getInputBuffer(ii);
              int n = ex.readSampleData(ib, 0);
              long t = ex.getSampleTime();
              if (n < 0 || t > en) { cd.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inDone = true; }
              else { cd.queueInputBuffer(ii, 0, n, t, 0); ex.advance(); }
            }
          }
          int oi = cd.dequeueOutputBuffer(bi, 10000);
          if (oi >= 0) {
            if (bi.size > 0 && bi.presentationTimeUs >= st && bi.presentationTimeUs <= en) {
              ByteBuffer ob = cd.getOutputBuffer(oi);
              byte[] b = new byte[bi.size];
              ob.position(bi.offset); ob.get(b);
              o.write(b); bytes += b.length;
            }
            cd.releaseOutputBuffer(oi, false);
            if ((bi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outDone = true;
          } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat of = cd.getOutputFormat();
            rate = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            ch = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
          }
        }
      }
      if (bytes <= 0) throw new Exception("Clip is empty");

      try (OutputStream o = new BufferedOutputStream(new FileOutputStream(wav)); InputStream p = new FileInputStream(pcm)) {
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes()).putInt((int) (36 + bytes)).put("WAVE".getBytes()).put("fmt ".getBytes())
         .putInt(16).putShort((short) 1).putShort((short) ch).putInt(rate).putInt(rate * ch * 2)
         .putShort((short) (ch * 2)).putShort((short) 16).put("data".getBytes()).putInt((int) bytes);
        o.write(h.array());
        byte[] b = new byte[16384]; int n;
        while ((n = p.read(b)) > 0) o.write(b, 0, n);
      }
      return wav;
    } catch (Exception e) {
      wav.delete();
      throw e;
    } finally {
      try { if (cd != null) { cd.stop(); cd.release(); } } catch (Exception ignored) { }
      try { ex.release(); } catch (Exception ignored) { }
      src.delete(); pcm.delete();
    }
  }
}
