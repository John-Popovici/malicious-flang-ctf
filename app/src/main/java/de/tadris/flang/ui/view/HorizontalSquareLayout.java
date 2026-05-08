package de.tadris.flang.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class HorizontalSquareLayout extends FrameLayout {

    public HorizontalSquareLayout(Context context) {
        super(context);
    }

    public HorizontalSquareLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalSquareLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int width, int height) {
        // note we are applying the width value as the height
        super.onMeasure(width, width);
    }

}
