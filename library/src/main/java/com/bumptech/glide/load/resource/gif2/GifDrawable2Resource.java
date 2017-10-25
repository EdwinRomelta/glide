package com.bumptech.glide.load.resource.gif2;

import com.bumptech.glide.load.engine.Initializable;
import com.bumptech.glide.load.engine.Resource;

/**
 * A resource wrapping an {@link GifDrawable2}.
 */
public class GifDrawable2Resource implements Resource<GifDrawable2>, Initializable {

  private final GifDrawable2 gifDrawable;

  public GifDrawable2Resource(GifDrawable2 animationDrawable) {
    this.gifDrawable = animationDrawable;
  }

  @Override public Class<GifDrawable2> getResourceClass() {
    return GifDrawable2.class;
  }

  @Override public GifDrawable2 get() {
    return gifDrawable;
  }

  @Override public int getSize() {
    return 0;
  }

  @Override public void recycle() {
    gifDrawable.stop();
  }

  @Override public void initialize() {
    gifDrawable.getFirstFrame().prepareToDraw();
    gifDrawable.start();
  }
}
