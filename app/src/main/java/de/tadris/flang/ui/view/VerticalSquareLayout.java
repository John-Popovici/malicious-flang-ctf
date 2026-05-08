package de.tadris.flang.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class VerticalSquareLayout extends FrameLayout {

    public VerticalSquareLayout(Context context) {
        super(context);
    }

    public VerticalSquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalSquareLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int width, int height) {
        // note we are applying the height value as the width
        super.onMeasure(height, height);
    }

}
