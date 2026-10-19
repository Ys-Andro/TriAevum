package org.triaevum.android;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.ViewGroup;
import org.libsdl.app.SDLSurface;

/** SDL retains lifecycle/input ownership; Android presents the game in a strict 18:9 viewport. */
final class TriAevumSurface extends SDLSurface {
    private static final double TARGET_ASPECT = 2.0; // 18:9
    private final int maximumShortEdge;

    TriAevumSurface(Context context, int maximumShortEdge) {
        super(context);
        this.maximumShortEdge = maximumShortEdge;
        setBackgroundColor(Color.BLACK);
        // The parent remains fullscreen; this view is measured to the largest 18:9
        // rectangle that fits inside it, leaving black bars instead of stretching.
        setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        int parentHeight = MeasureSpec.getSize(heightMeasureSpec);
        int width = parentWidth;
        int height = parentHeight;

        if (parentWidth > 0 && parentHeight > 0) {
            if ((double) parentWidth / parentHeight > TARGET_ASPECT) {
                width = (int) Math.round(parentHeight * TARGET_ASPECT);
            } else {
                height = (int) Math.round(parentWidth / TARGET_ASPECT);
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        int[] extent = SurfaceExtentPolicy.fit(width, height, maximumShortEdge);
        getHolder().setFixedSize(extent[0], extent[1]);
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        int viewWidth = getWidth() > 0 ? getWidth() : width;
        int viewHeight = getHeight() > 0 ? getHeight() : height;
        int[] extent = SurfaceExtentPolicy.fit(viewWidth, viewHeight, maximumShortEdge);
        if (width != extent[0] || height != extent[1]) {
            holder.setFixedSize(extent[0], extent[1]);
            return; // Do not start Vulkan against the transient full-resolution buffer.
        }
        Log.i("TriAevum", "Render surface " + width + "x" + height
            + ", view " + viewWidth + "x" + viewHeight + ", target 18:9");
        super.surfaceChanged(holder, format, width, height);
        // SDL normalizes touch positions against the actual game viewport. The
        // independently sized SurfaceHolder buffer is never used for touch mapping.
        mWidth = viewWidth;
        mHeight = viewHeight;
    }
}
