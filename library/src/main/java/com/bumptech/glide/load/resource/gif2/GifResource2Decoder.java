package com.bumptech.glide.load.resource.gif2;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.util.Log;
import com.bumptech.glide.gifdecoder.GifDecoder;
import com.bumptech.glide.gifdecoder.GifHeader;
import com.bumptech.glide.gifdecoder.GifHeaderParser;
import com.bumptech.glide.gifdecoder.StandardGifDecoder;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.gif.GifBitmapProvider;
import com.bumptech.glide.util.LogTime;
import com.bumptech.glide.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Queue;

/**
 * An {@link ResourceDecoder} that decodes
 * {@link GifDrawable2} from {@link InputStream} data.
 */
public class GifResource2Decoder implements ResourceDecoder<InputStream, GifDrawable2> {

  private static final String TAG = "GifResource2Decoder";

  private static final GifDecoderFactory GIF_DECODER_FACTORY = new GifDecoderFactory();
  private static final GifHeaderParserPool PARSER_POOL = new GifHeaderParserPool();
  private final List<ImageHeaderParser> parsers;
  private final GifHeaderParserPool parserPool;
  private final GifDecoderFactory gifDecoderFactory;
  private final GifBitmapProvider provider;

  public GifResource2Decoder(List<ImageHeaderParser> parsers, BitmapPool bitmapPool,
      ArrayPool arrayPool) {
    this.parsers = parsers;
    this.parserPool = PARSER_POOL;
    this.gifDecoderFactory = GIF_DECODER_FACTORY;
    this.provider = new GifBitmapProvider(bitmapPool, arrayPool);
  }

  @Override public boolean handles(InputStream source, Options options) throws IOException {
    return true;
  }

  @Nullable @Override
  public Resource<GifDrawable2> decode(InputStream source, int width, int height, Options options)
      throws IOException {
    byte[] data = inputStreamToBytes(source);
    if (data == null) {
      return null;
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(data);
    final GifHeaderParser parser = parserPool.obtain(byteBuffer);
    try {
      return decode(byteBuffer, width, height, parser);
    } finally {
      parserPool.release(parser);
    }
  }

  private GifDrawable2Resource decode(ByteBuffer byteBuffer, int width, int height,
      GifHeaderParser parser) {
    long startTime = LogTime.getLogTime();
    final GifHeader header = parser.parseHeader();
    if (header.getNumFrames() <= 0 || header.getStatus() != GifDecoder.STATUS_OK) {
      // If we couldn't decode the GIF, we will end up with a frame count of 0.
      return null;
    }

    int sampleSize = getSampleSize(header, width, height);
    GifDecoder gifDecoder = gifDecoderFactory.build(provider, header, byteBuffer, sampleSize);
    gifDecoder.advance();
    Bitmap firstFrame = gifDecoder.getNextFrame();
    if (firstFrame == null) {
      return null;
    }

    GifDrawable2 gifDrawable = new GifDrawable2();
    gifDrawable.addFrame(new BitmapDrawable(firstFrame), gifDecoder.getDelay(0));
    gifDecoder.advance();
    for (int i = 1; i < gifDecoder.getFrameCount(); ++i) {
      gifDrawable.addFrame(new BitmapDrawable(gifDecoder.getNextFrame()), gifDecoder.getDelay(i));
      gifDecoder.advance();
    }
    return new GifDrawable2Resource(gifDrawable);
  }

  private static int getSampleSize(GifHeader gifHeader, int targetWidth, int targetHeight) {
    int exactSampleSize =
        Math.min(gifHeader.getHeight() / targetHeight, gifHeader.getWidth() / targetWidth);
    int powerOfTwoSampleSize = exactSampleSize == 0 ? 0 : Integer.highestOneBit(exactSampleSize);
    // Although functionally equivalent to 0 for BitmapFactory, 1 is a safer default for our code
    // than 0.
    int sampleSize = Math.max(1, powerOfTwoSampleSize);
    if (Log.isLoggable(TAG, Log.VERBOSE) && sampleSize > 1) {
      Log.v(TAG, "Downsampling GIF"
          + ", sampleSize: "
          + sampleSize
          + ", target dimens: ["
          + targetWidth
          + "x"
          + targetHeight
          + "]"
          + ", actual dimens: ["
          + gifHeader.getWidth()
          + "x"
          + gifHeader.getHeight()
          + "]");
    }
    return sampleSize;
  }

  // Visible for testing.
  static class GifDecoderFactory {
    public GifDecoder build(GifDecoder.BitmapProvider provider, GifHeader header, ByteBuffer data,
        int sampleSize) {
      return new StandardGifDecoder(provider, header, data, sampleSize);
    }
  }

  // Visible for testing.
  static class GifHeaderParserPool {
    private final Queue<GifHeaderParser> pool = Util.createQueue(0);

    public synchronized GifHeaderParser obtain(ByteBuffer buffer) {
      GifHeaderParser result = pool.poll();
      if (result == null) {
        result = new GifHeaderParser();
      }
      return result.setData(buffer);
    }

    public synchronized void release(GifHeaderParser parser) {
      parser.clear();
      pool.offer(parser);
    }
  }

  private static byte[] inputStreamToBytes(InputStream is) {
    final int bufferSize = 16384;
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(bufferSize);
    try {
      int nRead;
      byte[] data = new byte[bufferSize];
      while ((nRead = is.read(data)) != -1) {
        buffer.write(data, 0, nRead);
      }
      buffer.flush();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
    return buffer.toByteArray();
  }
}
