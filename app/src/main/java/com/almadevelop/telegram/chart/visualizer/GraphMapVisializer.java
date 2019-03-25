package com.almadevelop.telegram.chart.visualizer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.almadevelop.telegram.chart.R;
import com.almadevelop.telegram.chart.graph.Graph;

public class GraphMapVisializer extends View {
    private GraphController graphManager;
    private GraphController.Callback controllerCallback;

    private boolean isAttached = false;

    public GraphMapVisializer(Context context) {
        this(context, null);
    }

    public GraphMapVisializer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphMapVisializer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GraphMapVisializer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int w = measureWidth(widthMeasureSpec);
        setMeasuredDimension(w, 300);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        graphManager.onGraphViewSizeChanged(w, h, getPaddingLeft(), getPaddingRight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (graphManager != null) {
            graphManager.drawLines(canvas);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        isAttached = true;
        super.onAttachedToWindow();

        if (graphManager != null) {
            graphManager.attachView(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        isAttached = false;
        super.onDetachedFromWindow();
        if (graphManager != null) {
            graphManager.detachView();
        }
    }

    public void setGraph(Graph graph) {
        //TODO get width from style
        final float lineWidth = getResources().getDimension(R.dimen.line_width);
        final float touchPadding = getResources().getDimension(R.dimen.touch_padding);

        final int unselectedColor = getResources().getColor(R.color.unselected);
        final int selectedColor = getResources().getColor(R.color.selected_frame);
        final int touchColor = getResources().getColor(R.color.graph_touch);

        graphManager = new GraphController(graph, getResources(), lineWidth, touchPadding, unselectedColor, selectedColor, touchColor, controllerCallback);

        if (isAttached) {
            graphManager.attachView(this);
        }
    }

    public void setGraphControllerCallback(GraphController.Callback controllerCallback) {
        this.controllerCallback = controllerCallback;
    }

    public void setLineEnabled(String lineLabel, boolean enabled) {
        if (graphManager != null) {
            if (enabled) {
                graphManager.showLine(lineLabel);
            } else {
                graphManager.hideLine(lineLabel);
            }
        }
    }

    private void init(@Nullable AttributeSet attrs) {
    }

    private int measureWidth(int widthMeasureSpec) {
        final int wMode = MeasureSpec.getMode(widthMeasureSpec);
        final int wSize = MeasureSpec.getSize(widthMeasureSpec);

        switch (wMode) {
            case MeasureSpec.AT_MOST:
            case MeasureSpec.EXACTLY:
                return wSize;
            case MeasureSpec.UNSPECIFIED:
            default:
                return ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }
}
