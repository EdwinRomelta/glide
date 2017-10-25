package com.bumptech.glide.load.resource.gif2;

import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable;

/**
 * Created by edwin on 10/24/17.
 */

public class GifDrawable2 extends AnimationDrawable {

  public Bitmap getFirstFrame() {
    return ((BitmapDrawable) getFrame(0)).getBitmap();
  }
}
