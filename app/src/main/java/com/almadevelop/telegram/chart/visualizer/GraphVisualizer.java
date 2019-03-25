package com.almadevelop.telegram.chart.visualizer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.almadevelop.telegram.chart.R;
import com.almadevelop.telegram.chart.Utils;
import com.almadevelop.telegram.chart.graph.GraphLine;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GraphVisualizer extends View implements GraphController.Callback {
    private static final String HOLDER_SELECTED_MAPPED_TOP = "selected_window_top_m";
    private static final String HOLDER_SELECTED_MAPPED_BOTTOM = "selected_window_bottom_m";

    private static final String HOLDER_SELECTED_TOP = "selected_window_top";
    private static final String HOLDER_SELECTED_BOTTOM = "selected_window_bottom";

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix lineMatrix = new Matrix();
    private float labelBottomPadding;
    private float lineWidth;
    private float scaleLineWidth;
    private float selectedLineWidth;

    private final TextPaint labelPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    @ColorInt
    private int scaleLineColor;
    @ColorInt
    private int selectedLineColor;

    private final SimpleDateFormat xFormatter = new SimpleDateFormat("MMM d", Locale.ENGLISH);

    //bounds of the whole graph
    private final RectF graphBounds = new RectF();
    //bounds of the lines in the graph
    private final RectF expectedLinesBounds = new RectF();
    //bounds for any new selection received
    private final RectF newSelectionBounds = new RectF();

    private SelectedGraphWindow currentSelectedWindow;
    private ValueAnimator linesBoundsAnimator;

    private AxisScale xScale;
    private AxisScale yScale;

    private final AxisMath yMath = new AxisMath();
    private final AxisMath xMath = new AxisMath();

    private SelectedData selectedData;

    public GraphVisualizer(Context context) {
        this(context, null);
    }

    public GraphVisualizer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GraphVisualizer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public GraphVisualizer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int w = measureWidth(widthMeasureSpec);
        setMeasuredDimension(w, 600);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        final Paint.FontMetrics fm = labelPaint.getFontMetrics();
        final float lineHeight = fm.bottom - fm.top + fm.leading;
        //add space for x axis labels
        final float xLabelsSpace = lineHeight + labelBottomPadding;

        graphBounds.set(0, 0, w, h - xLabelsSpace);

        expectedLinesBounds.set(graphBounds);

        xMath.setSize(expectedLinesBounds.left, expectedLinesBounds.right);
        yMath.setSize(expectedLinesBounds.top, expectedLinesBounds.bottom);

        xScale.setAxisLength(graphBounds.width());
        yScale.setAxisLength(graphBounds.height());

        if (currentSelectedWindow != null) {
            expectedLinesBounds.top = graphBounds.top + yMath.valueToPixel(currentSelectedWindow.getyLowExtremum());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentSelectedWindow != null) {
            drawXScale(canvas);
            drawYScale(canvas);

            canvas.scale(1, -1, graphBounds.centerX(), graphBounds.centerY());

            if(selectedData != null){
                canvas.drawLine(selectedData.xPos, graphBounds.bottom, selectedData.xPos, graphBounds.top, selectedLinePaint());
            }

            for (int i = 0; i<currentSelectedWindow.getLines().length; i++) {
                final GraphLinePath line = currentSelectedWindow.getLines()[i];

                final Paint linePaint = linePaint(line.getColor());

                canvas.drawPath(line.getPath(), linePaint);

                if(selectedData != null){
                    final long[] yValues = currentSelectedWindow.getyValues()[i];

                    final float y = yMath.valueToPixel(yValues[selectedData.valuePos]);

                    canvas.drawCircle(selectedData.xPos, y, 10,linePaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final boolean res = super.onTouchEvent(event);

        if (currentSelectedWindow != null) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    final float xPos = event.getX();

                    if (xPos >= expectedLinesBounds.left && xPos <= expectedLinesBounds.right) {
                        final int pointPosition = xMath.pixelToRoundPointPosition(xPos);

                        final float realXPos = xMath.valueToPixel(currentSelectedWindow.getxValues()[pointPosition]);

                        if(selectedData != null && selectedData.xPos == realXPos){
                            return false;
                        }

                        final StringBuilder sb = new StringBuilder();

                        for(int i = 0; i<currentSelectedWindow.getLines().length; i++){
                            final GraphLinePath line = currentSelectedWindow.getLines()[i];
                            final long value = currentSelectedWindow.getyValues()[i][pointPosition];

                            if(sb.length() > 0){
                                sb.append('\n');
                            }

                            sb.append(line.getLabel()).append(": ").append(value);
                        }

                        Toast.makeText(getContext(), sb, Toast.LENGTH_SHORT).show();

                        selectedData = new SelectedData(realXPos, pointPosition);

                        invalidate();

                        return true;
                    }
                default:
                    return false;
            }
        } else {
            return res;
        }
    }

    @Override
    public void onSelectionChanged(SelectedGraphWindow selectedWindow) {
        removeSelection();

        yMath.setValues(selectedWindow.getyTopExtremum(), 0, selectedWindow.getxValues().length);
        xMath.setValues(selectedWindow.getxTopExtremum(), selectedWindow.getxLowExtremum(), selectedWindow.getxValues().length);

        xScale.setAxisMinMax(selectedWindow.getxTopExtremum(), selectedWindow.getxLowExtremum());
        yScale.setAxisMinMax(selectedWindow.getyTopExtremum(), 0);

        //lift up lines to the lowest graph value
        expectedLinesBounds.top = graphBounds.top + yMath.valueToPixel(selectedWindow.getyLowExtremum());

        if (this.currentSelectedWindow == null || currentSelectedWindow.hasSameExtremums(selectedWindow)) {
            if (linesBoundsAnimator == null || !linesBoundsAnimator.isStarted()) {
                changeSelectedWindowBottom(selectedWindow);
            } else {
                preserveSelectedWindowBottom(selectedWindow);
            }
        } else {
            preserveSelectedWindowBottom(selectedWindow);

            //apply matrix to the selection window bounds
            final RectF currentMappedSelectionBounds = new RectF(selectedWindow.getBounds());
            lineMatrix.mapRect(currentMappedSelectionBounds);

            if (linesBoundsAnimator == null) {
                linesBoundsAnimator = newAmimator();
            }

            if (linesBoundsAnimator.isStarted()) {
                linesBoundsAnimator.cancel();
            }

            //animate change from current showed lines to the expected lines positions
            final PropertyValuesHolder selectMappedTopHolder =
                    PropertyValuesHolder.ofFloat(HOLDER_SELECTED_MAPPED_TOP,
                            currentMappedSelectionBounds.top,
                            expectedLinesBounds.top);
            final PropertyValuesHolder selectMappedBottomHolder =
                    PropertyValuesHolder.ofFloat(HOLDER_SELECTED_MAPPED_BOTTOM,
                            currentMappedSelectionBounds.bottom,
                            expectedLinesBounds.bottom);

            //animate change from current selected window bounds to the current selected window bounds.
            //It needed for proper positioning a new selected window if animation does not finished yet
            final PropertyValuesHolder selectTopHolder =
                    PropertyValuesHolder.ofFloat(HOLDER_SELECTED_TOP,
                            newSelectionBounds.top,
                            selectedWindow.getBounds().top);
            final PropertyValuesHolder selectBottomHolder =
                    PropertyValuesHolder.ofFloat(HOLDER_SELECTED_BOTTOM,
                            newSelectionBounds.bottom,
                            selectedWindow.getBounds().bottom);

            linesBoundsAnimator.setValues(selectMappedTopHolder,
                    selectMappedBottomHolder,
                    selectTopHolder,
                    selectBottomHolder);
            linesBoundsAnimator.addUpdateListener(new BoundsUpdateListener(currentMappedSelectionBounds));
            linesBoundsAnimator.setCurrentPlayTime(0);
            linesBoundsAnimator.start();
        }

        this.currentSelectedWindow = selectedWindow;

        invalidate();
    }

    private Paint linePaint(@ColorInt int lineColor) {
        return preparePaint(lineColor, lineWidth);
    }

    private Paint scaleLinePaint() {
        return preparePaint(scaleLineColor, scaleLineWidth);
    }

    private Paint selectedLinePaint() {
        return preparePaint(selectedLineColor, selectedLineWidth);
    }

    private Paint preparePaint(@ColorInt int color, float width) {
        linePaint.setColor(color);
        linePaint.setStrokeWidth(width);

        return linePaint;
    }

    private void removeSelection() {
        selectedData = null;

        invalidate();
    }

    private void changeSelectedWindowBottom(SelectedGraphWindow selectedWindow) {
        lineMatrix.setRectToRect(selectedWindow.getBounds(), expectedLinesBounds, Matrix.ScaleToFit.FILL);

        for (GraphLinePath line : selectedWindow.getLines()) {
            line.getPath().transform(lineMatrix);
        }

        newSelectionBounds.set(selectedWindow.getBounds());
    }

    private void preserveSelectedWindowBottom(SelectedGraphWindow selectedWindow) {
        newSelectionBounds.right = selectedWindow.getBounds().right;
        newSelectionBounds.left = selectedWindow.getBounds().left;

        lineMatrix.setRectToRect(newSelectionBounds, expectedLinesBounds, Matrix.ScaleToFit.FILL);

        for (GraphLinePath line : selectedWindow.getLines()) {
            line.getPath().transform(lineMatrix);
        }
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

    private void init(@Nullable AttributeSet attrs) {
        scaleLineColor = Utils.getColor(getContext(), R.color.solitude);
        scaleLineWidth = getResources().getDimension(R.dimen.axis_width);

        lineWidth = getResources().getDimension(R.dimen.line_width);

        selectedLineColor = Utils.getColor(getContext(), R.color.pattens_blue);
        selectedLineWidth = getResources().getDimension(R.dimen.selected_width);

        labelBottomPadding = getResources().getDimension(R.dimen.axis_labels_padding);

        labelPaint.setColor(Utils.getColor(getContext(), R.color.grey_chateau));
        labelPaint.setTextSize(getResources().getDimension(R.dimen.axis_labels_size));

        linePaint.setStyle(Paint.Style.STROKE);


        xScale = AxisScale.calculatableSizeScale(0.2f);
        yScale = AxisScale.calculatableSizeScale(0.2f);

        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

//        if (attrs != null) {
//            final TypedArray ta = getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.GraphVisualizer, 0, R.style.SSS);
//
//            try {
//                graphHeight = ta.getDimensionPixelSize(R.styleable.GraphVisualizer_graph_height, 0);
//                miniGraphHeight = ta.getDimensionPixelSize(R.styleable.GraphVisualizer_mini_graph_height, 0);
//            } finally {
//                ta.recycle();
//            }
//        }
    }

    private void drawYScale(Canvas canvas) {
        final float startX = graphBounds.left;
        final float endX = graphBounds.right;

        labelPaint.setTextAlign(Paint.Align.LEFT);

        for (int i = 0; i < yScale.getCount(); i++) {
            final long value = yScale.getScaleByPosition(i);
            final float y = graphBounds.height() - yMath.valueToPixel(value);

            canvas.drawLine(startX, y, endX, y, scaleLinePaint());
            canvas.drawText(String.valueOf(value), startX, y - labelBottomPadding, labelPaint);
        }
    }

    private void drawXScale(Canvas canvas) {
        final Date date = new Date();

        final Paint.FontMetrics fm = labelPaint.getFontMetrics();

        final float y = getHeight() - fm.descent;

        date.setTime(currentSelectedWindow.getxLowExtremum());
        final String firstDate = xFormatter.format(date);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(firstDate, 0, y, labelPaint);

        date.setTime(currentSelectedWindow.getxTopExtremum());
        final String lastDate = xFormatter.format(date);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(lastDate, graphBounds.width(), y, labelPaint);

        final int count = xScale.getCount() - 2;

        if (count > 0) {
            labelPaint.setTextAlign(Paint.Align.LEFT);

            final float freeSpace = graphBounds.width() - labelPaint.measureText(firstDate) - labelPaint.measureText(lastDate);

            final float size = freeSpace / count;


        }

//        for (int i = 0; i < xScale.getCount(); i++) {
//            final float x = xScale.getSize() * i;
//
//            final long millis = xScale.getScaleByPosition(i);
//            date.setTime(millis);
//
//            float ff = labelPaint.measureText(xFormatter.format(date));
//
//            canvas.drawText(xFormatter.format(date), x, y, labelPaint);
//        }
    }

    private ValueAnimator newAmimator() {
        final ValueAnimator linesBoundsAnimator = new ValueAnimator();
        linesBoundsAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                ((ValueAnimator) animation).removeAllUpdateListeners();

                setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                ((ValueAnimator) animation).removeAllUpdateListeners();
            }
        });
        linesBoundsAnimator.setDuration(300);

        return linesBoundsAnimator;
    }

    private static class SelectedData {
        private final float xPos;
        private final int valuePos;

        SelectedData(float xPos, int valuePos) {
            this.xPos = xPos;
            this.valuePos = valuePos;
        }
    }

    private class BoundsUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        //current mapped lines positions
        private final RectF currentBounds;
        //expected lines position
        private final RectF targetBounds;

        BoundsUpdateListener(RectF currentLinesVBounds) {
            this.currentBounds = currentLinesVBounds;

            //set top and bottom values to th current values. It will be changed bt animator
            this.targetBounds = new RectF(expectedLinesBounds.left,
                    currentBounds.top,
                    expectedLinesBounds.right,
                    currentBounds.bottom);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            targetBounds.top = (float) animation.getAnimatedValue(HOLDER_SELECTED_MAPPED_TOP);
            targetBounds.bottom = (float) animation.getAnimatedValue(HOLDER_SELECTED_MAPPED_BOTTOM);

            newSelectionBounds.top = (float) animation.getAnimatedValue(HOLDER_SELECTED_TOP);
            newSelectionBounds.bottom = (float) animation.getAnimatedValue(HOLDER_SELECTED_BOTTOM);

            lineMatrix.setRectToRect(currentBounds, targetBounds, Matrix.ScaleToFit.FILL);
            lineMatrix.mapRect(currentBounds);

            for (GraphLinePath line : currentSelectedWindow.getLines()) {
                line.getPath().transform(lineMatrix);
            }

            invalidate();
        }
    }
}
