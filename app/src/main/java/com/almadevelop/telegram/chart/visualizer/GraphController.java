package com.almadevelop.telegram.chart.visualizer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Xfermode;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.almadevelop.telegram.chart.Utils;
import com.almadevelop.telegram.chart.graph.Graph;
import com.almadevelop.telegram.chart.graph.GraphLine;
import com.almadevelop.telegram.chart.graph.GraphObject;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

class GraphController implements View.OnTouchListener {
    private static final String HOLDER_LINE_ALPHA_PREFIX = "line_alpha_";
    private static final String HOLDER_TOP = "top";
    private static final String HOLDER_BOTTOM = "bottom";

    private static final int TRANSITION_HIDE = 0;
    private static final int TRANSITION_SHOW = 1;

    private final RectF lineBoundsTemp = new RectF();

    @NonNull
    private final Graph graph;

    @Nullable
    private final Callback callback;

    private final float touchDrawPadding;
    private final float lineWidth;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint notSelectedPaint = new Paint();

    private final SelectedWindow selectedWindow;
    private final Touch touch;

    private final Map<String, GraphLinePath> lines;
    private final Map<String, Integer> linesInTransition;
    private final SortedMap<Long, Integer> extremums;

    private final Path[] tempShowedPath;

    private final Matrix lineMatrix = new Matrix();

    private View view;

    private final RectF graphBounds = new RectF();
    //bounds of lines what should be
    private final RectF expectedLinesBounds = new RectF();
    //actual lines bounds
    private final RectF currentLinesBounds = new RectF();

    private final AxisMath xMath = new AxisMath();
    private final AxisMath yMath = new AxisMath();

    private ValueAnimator linesAnimator;
    private Animator.AnimatorListener linesAnimatorListener;

    GraphController(@NonNull Graph graph,
                    @NonNull Resources res,
                    float lineWidth,
                    float touchDrawPadding,
                    @ColorInt int notSelectedColor,
                    @ColorInt int selectedColor,
                    @ColorInt int touchColor,
                    @Nullable Callback callback) {
        this.graph = graph;
        this.callback = callback;
        this.lineWidth = lineWidth;
        this.touchDrawPadding = touchDrawPadding;

        this.linePaint.setStyle(Paint.Style.STROKE);
        this.linePaint.setStrokeWidth(lineWidth);
        this.linePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OVER));

        this.notSelectedPaint.setStyle(Paint.Style.FILL);
        this.notSelectedPaint.setColor(notSelectedColor);

        this.selectedWindow = new SelectedWindow(res, selectedColor);
        this.touch = new Touch(touchColor);

        this.lines = new HashMap<>(graph.linesCount());

        this.linesInTransition = new HashMap<>(graph.linesCount());

        this.extremums = new TreeMap<>((x, y) -> Utils.compareLong(y, x));

        this.tempShowedPath = new Path[graph.linesCount()];

        setExtremums();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final boolean isHandled = selectedWindow.onTouch(event);

        if (isHandled) {
            sendSelectedWindow();
        }

        return isHandled;
    }

    void attachView(@NonNull View view) {
        this.view = view;
        view.setOnTouchListener(this);

        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        if (view.getWidth() > 0.0f && view.getHeight() > 0.0f) {
            onGraphViewSizeChanged(view.getWidth(), view.getHeight(), view.getPaddingLeft(), view.getPaddingRight());
        }
    }

    void detachView() {
        this.view.setOnTouchListener(null);
        this.view = null;
    }

    void onGraphViewSizeChanged(float newWidth, float newHeight, float paddingLeft, float paddingRight) {
        final float oldW = graphBounds.width();
        final float oldH = graphBounds.height();

        graphBounds.set(paddingLeft, touchDrawPadding, newWidth - paddingRight, newHeight - touchDrawPadding);
        expectedLinesBounds.set(graphBounds);

        if (oldW == graphBounds.width() && oldH == graphBounds.height()) {
            return;
        }

        if (graphBounds.width() > 0.0f && graphBounds.height() > 0.0f) {
            selectedWindow.onGraphSizeChanged();

            //add offset to up and bottom to not draw lines behind selected window
            expectedLinesBounds.inset(0, Math.round(selectedWindow.tbBorderHeigth + lineWidth * 0.5f));
            currentLinesBounds.set(expectedLinesBounds);

            xMath.setSize(expectedLinesBounds.left, expectedLinesBounds.right);
            yMath.setSize(expectedLinesBounds.top, expectedLinesBounds.bottom);

            //step between x columns
            final float xPixelPerPoint = xMath.pixelPerPoint();

            for (GraphLine line : graph.getLines()) {

                final Path linePath = new Path();

                for (int i = 0; i < line.getData().size(); i++) {
                    final float xPos = xPixelPerPoint * i + expectedLinesBounds.left;
                    final float yPos = yMath.valueToPixel(line.getData().get(i));

                    if (linePath.isEmpty()) {
                        linePath.moveTo(xPos, yPos);
                    } else {
                        linePath.lineTo(xPos, yPos);
                    }
                }

                lines.put(line.getLabel(), new GraphLinePath(linePath,
                        line.getLabel(),
                        line.getTopExtremum(),
                        line.getLowExtremum(),
                        line.getColor()));
            }

            sendSelectedWindow();

            view.invalidate();
        }
    }

    /**
     * Hide graph's line with provided label
     *
     * @param lineLabel line's label to hide
     */
    void hideLine(@NonNull String lineLabel) {
        final GraphLinePath line = lines.get(lineLabel);

        if (line != null) {
            if (line.isInvisible()) {
                return;
            }

            changeLineVisibility(line, false);
        }
    }

    /**
     * Show graph's line with provided label
     *
     * @param lineLabel line's label to show
     */
    void showLine(@NonNull String lineLabel) {
        final GraphLinePath line = lines.get(lineLabel);

        if (line != null) {
            if (line.isVisible()) {
                return;
            }

            changeLineVisibility(line, true);
        }
    }

    void drawLines(Canvas canvas) {
        if (graphBounds.width() == 0.0f || graphBounds.height() == 0.0f) {
            return;
        }

        if (lines.size() > 0) {
            canvas.save();

            canvas.drawRect(graphBounds, notSelectedPaint);

            selectedWindow.draw(canvas);

            canvas.scale(1, -1, expectedLinesBounds.centerX(), expectedLinesBounds.centerY());

            for (GraphLine graphLine : graph.getLines()) {
                final GraphLinePath line = lines.get(graphLine.getLabel());

                linePaint.setColor(line.getColor());
                linePaint.setAlpha(Math.max(0, line.getColorAlpha()));

                canvas.drawPath(line.getPath(), linePaint);
            }

            canvas.restore();

            touch.draw(canvas);
        }
    }

    private static long[] getLineSubData(GraphObject graphObject, int start) {
        return getLineSubData(graphObject, start, graphObject.getData().size());
    }

    private static long[] getLineSubData(GraphObject graphObject, int start, int end) {
        final long[] subData = new long[end - start];

        for (int i = 0; i < subData.length; i++) {
            subData[i] = graphObject.getData().get(i + start);
        }

        return subData;
    }

    private void setGraphMathValues() {
        final long xTop = graph.getXAxis().getTopExtremum();
        final long xLow = graph.getXAxis().getLowExtremum();

        xMath.setValues(xTop, xLow, graph.size());
        yMath.setValues(currentTopExtremum(), currentLowExtremum(), graph.size());
    }

    /**
     * Return cached instance of showed via callback Path. Create it if needed
     *
     * @param pos position in the array
     * @return cached empty Path
     */
    private Path getShowedTempPath(int pos) {
        Path resultPath = tempShowedPath[pos];

        if (resultPath == null) {
            resultPath = new Path();
            tempShowedPath[pos] = resultPath;
        } else {
            resultPath.reset();
        }

        return resultPath;
    }

    /**
     * Calculate and animate line transition
     *
     * @param line line to show/hide
     * @param show is it show or hide transition
     */
    private void changeLineVisibility(@NonNull GraphLinePath line, boolean show) {
        final long topExtremum = currentTopExtremum();
        final long lowExtremum = currentLowExtremum();

        if (show) {
            addExtremum(line);
        } else {
            removeExtremum(line);
        }

        final long newTopExtremum = currentTopExtremum();
        final long newLowExtremum = currentLowExtremum();

        final boolean needToTransformLines = topExtremum != newTopExtremum || lowExtremum != newLowExtremum;

        if (linesAnimator == null) {
            linesAnimator = new ValueAnimator();
            linesAnimatorListener = new LinesAnimationListener();
            linesAnimator.setDuration(1000);
            linesAnimator.addListener(linesAnimatorListener);
        }

        if (linesAnimator.isStarted()) {
            linesAnimator.removeAllListeners();
            linesAnimator.removeAllUpdateListeners();

            linesAnimator.cancel();

            linesAnimator.addListener(linesAnimatorListener);
        }


        linesInTransition.put(line.getLabel(), show ? TRANSITION_SHOW : TRANSITION_HIDE);

        //+2 - animation of lines transfromation. Others - alpha animations
        final PropertyValuesHolder[] holders =
                new PropertyValuesHolder[linesInTransition.size() + (needToTransformLines ? 2 : 0)];

        fillLineInTransitionHolders(holders);

        if (needToTransformLines) {
            final float topChange = (lowExtremum - newLowExtremum) * yMath.currentPixelPerValue();
            holders[holders.length - 2] =
                    PropertyValuesHolder.ofFloat(HOLDER_TOP,
                            currentLinesBounds.top, currentLinesBounds.top + topChange);

            final float bottomChange = (topExtremum - newTopExtremum) * yMath.currentPixelPerValue();
            holders[holders.length - 1] =
                    PropertyValuesHolder.ofFloat(HOLDER_BOTTOM,
                            currentLinesBounds.bottom, currentLinesBounds.bottom + bottomChange);

            //extra listener which prepare current line to future transformations
            linesAnimator.addListener(new LinesAnimationPrepareListener(line, topChange, bottomChange));
        }

        linesAnimator.setValues(holders);
        linesAnimator.setCurrentPlayTime(0);
        linesAnimator.addUpdateListener(new LinesAnimationUpdateListener(needToTransformLines));
        linesAnimator.start();
    }

    private class LinesAnimationListener extends AnimatorListenerAdapter {
        @Override
        public void onAnimationStart(Animator animation) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            ((ValueAnimator) animation).removeAllUpdateListeners();

            //reset bounds
            currentLinesBounds.set(expectedLinesBounds);

            linesInTransition.clear();

            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    private class LinesAnimationPrepareListener extends AnimatorListenerAdapter {
        private final GraphLinePath line;
        private final float topBoundsChange;
        private final float bottomBoundsChange;

        public LinesAnimationPrepareListener(GraphLinePath line,
                                             float topBoundsChange,
                                             float bottomBoundsChange) {
            this.line = line;
            this.topBoundsChange = topBoundsChange;
            this.bottomBoundsChange = bottomBoundsChange;
        }

        @Override
        public void onAnimationStart(Animator animation) {
            //put line on the correct position before animation
            if (line.isInvisible()) {
                applyLineTransformation();
            }
        }

        @Override
        public void onAnimationEnd(Animator animation) {
            animation.removeListener(this);

            //apply transformation to the line after it was hidden
            if (line.isInvisible()) {
                applyLineTransformation();
            }
        }

        private void applyLineTransformation() {
            final RectF target = new RectF(currentLinesBounds);

            target.bottom += bottomBoundsChange;
            target.top += topBoundsChange;

            lineMatrix.setRectToRect(currentLinesBounds, target, Matrix.ScaleToFit.FILL);

            line.getPath().transform(lineMatrix);
        }
    }

    private class LinesAnimationUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private final RectF targetRect;

        LinesAnimationUpdateListener(boolean checkBounds) {
            targetRect = checkBounds ? new RectF(currentLinesBounds) : null;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            for (String lineLabel : linesInTransition.keySet()) {
                final int lineAlpha = (int) animation.getAnimatedValue(lineAlphaHolderName(lineLabel));
                lines.get(lineLabel).setColorAlpha(lineAlpha);
            }

            if (targetRect != null) {
                targetRect.bottom = (float) animation.getAnimatedValue(HOLDER_BOTTOM);
                targetRect.top = (float) animation.getAnimatedValue(HOLDER_TOP);

                lineMatrix.setRectToRect(currentLinesBounds, targetRect, Matrix.ScaleToFit.FILL);
                lineMatrix.mapRect(currentLinesBounds);

                for (GraphLinePath line : lines.values()) {
                    if (!linesInTransition.containsKey(line.getLabel())) {
                        line.getPath().transform(lineMatrix);
                    }
                }
            }

            view.invalidate();
        }
    }

    private void fillLineInTransitionHolders(PropertyValuesHolder[] holders) {
        if (linesInTransition.isEmpty()) {
            return;
        }

        int i = 0;
        for (Map.Entry<String, Integer> e : linesInTransition.entrySet()) {
            final int alpha;

            switch (e.getValue()) {
                case TRANSITION_HIDE:
                    alpha = 0;
                    break;
                case TRANSITION_SHOW:
                    alpha = 255;
                    break;
                default:
                    throw new IllegalStateException("Unsupported transition " + e.getValue());
            }

            holders[i++] = lineAlphaValueHolder(lines.get(e.getKey()), alpha);
        }
    }

    private static String lineAlphaHolderName(String lineLabel) {
        return HOLDER_LINE_ALPHA_PREFIX + lineLabel;
    }

    private static PropertyValuesHolder lineAlphaValueHolder(GraphLinePath line, int alpha) {
        return PropertyValuesHolder.ofInt(lineAlphaHolderName(line.getLabel()),
                line.getColorAlpha(), alpha);
    }

    /**
     * Get graph's current top extremum value
     *
     * @return the top extremum value
     */
    private long currentTopExtremum() {
        return extremums.firstKey();
    }

    /**
     * Get graph's current lowe extremum value
     *
     * @return the low extremum value
     */
    private long currentLowExtremum() {
        return extremums.lastKey();
    }

    /**
     * Set all Y extremums from graph lines
     */
    private void setExtremums() {
        extremums.clear();

        for (GraphLine line : graph.getLines()) {
            Integer topExtremumCount = extremums.get(line.getTopExtremum());
            Integer lowExtremumCount = extremums.get(line.getLowExtremum());

            extremums.put(line.getTopExtremum(), topExtremumCount == null ? 1 : ++topExtremumCount);
            extremums.put(line.getLowExtremum(), lowExtremumCount == null ? 1 : ++lowExtremumCount);
        }

        setGraphMathValues();
    }

    /**
     * Add line's extremums to the tree
     *
     * @param line line to use
     */
    private void addExtremum(GraphLinePath line) {
        Integer topCount = extremums.get(line.getTopExtremum());
        Integer lowCount = extremums.get(line.getLowExtremum());

        extremums.put(line.getTopExtremum(), topCount == null ? 1 : ++topCount);
        extremums.put(line.getLowExtremum(), lowCount == null ? 1 : ++lowCount);

        setGraphMathValues();
    }

    /**
     * Remove line's extremums from the tree
     *
     * @param line line to use
     */
    private void removeExtremum(GraphLinePath line) {
        Integer topCount = extremums.get(line.getTopExtremum());
        Integer lowCount = extremums.get(line.getLowExtremum());

        if (topCount == 1) {
            extremums.remove(line.getTopExtremum());
        } else {
            extremums.put(line.getTopExtremum(), --topCount);
        }

        if (lowCount == 1) {
            extremums.remove(line.getLowExtremum());
        } else {
            extremums.put(line.getLowExtremum(), --lowCount);
        }

        setGraphMathValues();
    }

    /**
     * Send selected windows' lines to the callback
     */
    private void sendSelectedWindow() {
        if (callback != null) {
            final GraphLinePath[] newPaths = new GraphLinePath[graph.linesCount()];
            final long[] xResultPathsValues;
            final long[][] yResultPathsValues = new long[graph.linesCount()][];

            //pixels
            final float startSelectionPos = selectedWindow.bounds.left;
            final float endSelectionPos = selectedWindow.bounds.right;

            final boolean isFromStartSelection = startSelectionPos == graphBounds.left;
            final boolean isToEndSelection = endSelectionPos == graphBounds.right;

            final boolean isWholeLineSelected = isFromStartSelection && isToEndSelection;

            final PathMeasure pathMeasure = new PathMeasure();

            long selectedTopExtremum = isWholeLineSelected ? currentTopExtremum() : Long.MIN_VALUE;
            long selectedLowExtremum = isWholeLineSelected ? currentLowExtremum() : Long.MAX_VALUE;

            float[] distancePos = null;

            //calculate start and end selected positions of asix values

            final int selectedValuesStart = xMath.pixelToRoundPointPosition(startSelectionPos);
            final int selectedValuesEnd = xMath.pixelToRoundPointPosition(endSelectionPos);

            //slice selected X axis values
            if (isWholeLineSelected) {
                xResultPathsValues = getLineSubData(graph.getXAxis(), 0);
            } else {
                xResultPathsValues = getLineSubData(graph.getXAxis(), selectedValuesStart, selectedValuesEnd);
            }

            for (int i = 0; i < graph.linesCount(); i++) {
                final GraphLine graphLine = graph.getLines().get(i);
                final GraphLinePath graphLinePath = lines.get(graphLine.getLabel());

                pathMeasure.setPath(graphLinePath.getPath(), false);

                final Path selectedPartPath = getShowedTempPath(i);

                if (isWholeLineSelected) {
                    //no need to calculate
                    selectedPartPath.set(graphLinePath.getPath());
                    yResultPathsValues[i] = getLineSubData(graphLine, 0);
                } else {
                    //current line length
                    final float currentPathLen = pathMeasure.getLength();
                    //current path distance ratio
                    final float ratio = currentPathLen / graphBounds.width();

                    if (distancePos == null) {
                        distancePos = new float[2];
                    }

                    //calculated path distances
                    float startDistance = isFromStartSelection ? 0 : Float.MIN_VALUE;
                    float endDistance = isToEndSelection ? currentPathLen : Float.MIN_VALUE;

                    for (float distance = 0; distance < currentPathLen; distance += ratio) {
                        if (startDistance == Float.MIN_VALUE) {
                            pathMeasure.getPosTan(distance, distancePos, null);

                            final float distanceXPos = distancePos[0];

                            if (distanceXPos >= startSelectionPos) {
                                startDistance = distance;
                            }
                        }
                        if (endDistance == Float.MIN_VALUE) {
                            pathMeasure.getPosTan(currentPathLen - distance, distancePos, null);

                            final float distanceXPos = distancePos[0];

                            if (distanceXPos <= endSelectionPos) {
                                endDistance = currentPathLen - distance;
                            }
                        }

                        if (startDistance != Float.MIN_VALUE && endDistance != Float.MIN_VALUE) {
                            break;
                        }
                    }

                    //extract the path segment using calculated distances
                    pathMeasure.getSegment(startDistance, endDistance, selectedPartPath, true);
                    //hack from getSegment documentation
                    selectedPartPath.rLineTo(0, 0);

                    selectedPartPath.computeBounds(lineBoundsTemp, false);

                    //switch top and bottom because y reversed
                    selectedTopExtremum = Math.max(selectedTopExtremum, yMath.pixelToValue(lineBoundsTemp.bottom));
                    selectedLowExtremum = Math.min(selectedLowExtremum, yMath.pixelToValue(lineBoundsTemp.top));

                    yResultPathsValues[i] = getLineSubData(graphLine, selectedValuesStart, selectedValuesEnd);
                }

                newPaths[i] = new GraphLinePath(selectedPartPath,
                        graphLine.getLabel(),
                        graphLine.getTopExtremum(),
                        graphLine.getLowExtremum(),
                        graphLine.getColor());
            }

            callback.onSelectionChanged(new SelectedGraphWindow(newPaths,
                    new RectF(startSelectionPos,
                            yMath.valueToPixel(selectedLowExtremum),
                            endSelectionPos,
                            yMath.valueToPixel(selectedTopExtremum)),
                    xResultPathsValues,
                    yResultPathsValues,
                    selectedTopExtremum,
                    selectedLowExtremum));
        }
    }

    interface Callback {
        /**
         * Called then selected window changed
         *
         * @param selectedWindow describes selected window
         */
        void onSelectionChanged(SelectedGraphWindow selectedWindow);
    }

    private class Touch implements ValueAnimator.AnimatorUpdateListener {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final ValueAnimator scaleAnimator = scaleAnimator();

        private float cx;
        private float currentScale;

        public Touch(@ColorInt int color) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            currentScale = (float) animation.getAnimatedValue();

            view.invalidate();
        }

        private void show(float cx) {
            this.cx = cx;

            scaleAnimator.start();
        }

        private void move(float cx) {
            this.cx = cx;

            view.invalidate();
        }

        private void hide() {
            scaleAnimator.reverse();
        }

        private void draw(Canvas canvas) {
            if (currentScale == 0.0f) {
                return;
            }

            final float radius = view.getHeight() * 0.5f;

            canvas.drawCircle(cx, radius, radius * currentScale, paint);
        }

        private ValueAnimator scaleAnimator() {
            final ValueAnimator animator = ValueAnimator.ofFloat(0.0f, 1.0f);
            animator.setDuration(200);
            animator.addUpdateListener(this);
            return animator;
        }
    }

    private class SelectedWindow {
        private static final float MIN_FACTOR = 0.2f;
        private static final float BORDER_W_FACTOR = 0.1f;
        private static final float BORDER_H_FACTOR = 0.02f;

        private static final int MOTION_MODE_NONE = 0;
        private static final int MOTION_MODE_MOVE = 1;
        private static final int MOTION_MODE_LEFT = 2;
        private static final int MOTION_MODE_RIGHT = 3;


        private final RectF bounds = new RectF();
        private final Paint paint = new Paint();

        private final float touchPading;

        @ColorInt
        private final int frameColor;
        private final Xfermode clearMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

        private float minSelectedWidth;

        private float lrBorderWidth;
        private float tbBorderHeigth;

        private RectF leftBorderTouchRect = new RectF();
        private RectF rightBorderTouchRect = new RectF();

        private float prevTouchX;

        private int motionMode = MOTION_MODE_NONE;

        SelectedWindow(Resources res, @ColorInt int frameColor) {
            this.frameColor = frameColor;

            touchPading = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8.0f, res.getDisplayMetrics());
        }

        private void onGraphSizeChanged() {
            bounds.top = graphBounds.top;
            bounds.bottom = graphBounds.bottom;

            //minimum possible selected width
            minSelectedWidth = graphBounds.width() * MIN_FACTOR;

            //width and heigth of left/right and top/bottom borders
            lrBorderWidth = minSelectedWidth * BORDER_W_FACTOR;
            tbBorderHeigth = Math.max(1.f, bounds.height() * BORDER_H_FACTOR);

            if (bounds.left == 0 && bounds.right == 0) {
                bounds.right = graphBounds.right;
                bounds.left = graphBounds.right - minSelectedWidth;
            }

            leftBorderTouchRect.set(bounds.left, bounds.top, bounds.left + lrBorderWidth, bounds.bottom);
            leftBorderTouchRect.inset(-touchPading, 0);

            rightBorderTouchRect.set(bounds.right - lrBorderWidth, bounds.top, bounds.right, bounds.bottom);
            rightBorderTouchRect.inset(-touchPading, 0);
        }

        private boolean move(float dx) {
            if (dx < 0.f) {
                if (bounds.left == graphBounds.left) {
                    return false;
                }

                dx += graphBounds.left - Math.min(graphBounds.left, bounds.left + dx);
            } else {
                if (bounds.right == graphBounds.right) {
                    return false;
                }

                dx += graphBounds.right - Math.max(graphBounds.right, bounds.right + dx);
            }

            bounds.offset(dx, 0f);
            leftBorderTouchRect.offset(dx, 0);
            rightBorderTouchRect.offset(dx, 0);

            return true;
        }

        private boolean scaleLeft(float dx) {
            if (dx < 0.f) {
                if (bounds.left == graphBounds.left) {
                    return false;
                }

                dx += graphBounds.left - Math.min(graphBounds.left, bounds.left + dx);
            } else {
                if (bounds.width() == minSelectedWidth) {
                    return false;
                }

                final float maxX = bounds.right - minSelectedWidth;
                dx += maxX - Math.max(maxX, bounds.left + dx);
            }

            bounds.left += dx;
            leftBorderTouchRect.offset(dx, 0);

            return true;
        }

        private boolean scaleRight(float dx) {
            if (dx > 0.f) {
                if (bounds.right == graphBounds.right) {
                    return false;
                }

                dx += graphBounds.right - Math.max(graphBounds.right, bounds.right + dx);
            } else {
                if (bounds.width() == minSelectedWidth) {
                    return false;
                }

                final float maxX = bounds.left + minSelectedWidth;
                dx += maxX - Math.min(maxX, bounds.right + dx);
            }

            bounds.right += dx;
            rightBorderTouchRect.offset(dx, 0);

            return true;
        }

        private boolean onTouch(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    final float x = event.getX();
                    final float y = event.getY();

                    final boolean handled;

                    if (leftBorderTouchRect.contains(x, y)) {
                        motionMode = MOTION_MODE_LEFT;
                        handled = true;
                    } else if (rightBorderTouchRect.contains(x, y)) {
                        motionMode = MOTION_MODE_RIGHT;
                        handled = true;
                    } else if (bounds.contains(x, y)) {
                        motionMode = MOTION_MODE_MOVE;
                        handled = true;
                    } else {
                        handled = false;
                    }

                    if (handled) {
                        prevTouchX = x;

                        touch.show(x);
                    }

                    return handled;
                }
                case MotionEvent.ACTION_UP:
                    prevTouchX = 0.f;
                    motionMode = MOTION_MODE_NONE;
                    touch.hide();
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    final float x = event.getX();

                    final float offset = x - prevTouchX;

                    if (Math.abs(offset) < 5.0f) {
                        return false;
                    }

                    switch (motionMode) {
                        case MOTION_MODE_MOVE:
                            if (!move(offset)) {
                                return false;
                            }
                            break;
                        case MOTION_MODE_LEFT:
                            if (!scaleLeft(offset)) {
                                return false;
                            }
                            break;
                        case MOTION_MODE_RIGHT:
                            if (!scaleRight(offset)) {
                                return false;
                            }
                            break;
                        default:
                            throw new IllegalArgumentException(String.format("Unsupported motion mode '{%1$d}'", motionMode));
                    }

                    touch.move(x);

                    prevTouchX = x;

                    return true;
                }
                default:
                    return false;
            }
        }

        private void draw(Canvas canvas) {
            paint.setColor(frameColor);
            paint.setXfermode(clearMode);
            canvas.drawRect(bounds, paint);

            paint.setXfermode(null);
            canvas.drawRect(bounds, paint);

            paint.setColor(Color.TRANSPARENT);
            paint.setXfermode(clearMode);
            bounds.inset(lrBorderWidth, tbBorderHeigth);
            canvas.drawRect(bounds, paint);
            bounds.inset(-lrBorderWidth, -tbBorderHeigth);
        }
    }
}
