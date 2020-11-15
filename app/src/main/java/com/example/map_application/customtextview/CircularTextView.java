package com.example.map_application.customtextview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class CircularTextView extends TextView
{

    public CircularTextView(Context context) {
        super(context);
    }

    public CircularTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CircularTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int r = Math.max(getMeasuredWidth(),getMeasuredHeight());
        setMeasuredDimension(r, r);
    }
}