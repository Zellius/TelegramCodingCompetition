package com.almadevelop.telegram.chart;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.almadevelop.telegram.chart.graph.Graph;
import com.almadevelop.telegram.chart.graph.GraphLine;
import com.almadevelop.telegram.chart.parser.GraphRootParser;
import com.almadevelop.telegram.chart.visualizer.GraphMapVisializer;
import com.almadevelop.telegram.chart.visualizer.GraphVisualizer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends Activity implements CompoundButton.OnCheckedChangeListener {
    private final int[] attrs = new int[]{android.R.attr.listChoiceIndicatorMultiple};
    private Drawable supportButtonDrawable;

    private GraphVisualizer graphVisualizer;
    private GraphMapVisializer miniGraphVisializer;
    private ViewGroup checkBoxesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkBoxesView = findViewById(R.id.checkboxes);
        graphVisualizer = findViewById(R.id.graph);
        miniGraphVisializer = findViewById(R.id.miniVisualizer);
        miniGraphVisializer.setGraphControllerCallback(graphVisualizer);

        if (useSupport()) {
            final TypedArray a = getTheme().obtainStyledAttributes(attrs);
            supportButtonDrawable = a.getDrawable(0);
            a.recycle();
        }

        final List<Graph> graphs;

        try {
            graphs = GraphRootParser.parse(new InputStreamReader(getResources().openRawResource(R.raw.chart_data)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //graphs.get(4).getLines().remove(2);
        //graphs.get(4).getLines().remove(2);

        showGraph(graphs.get(4));
    }


    private void showGraph(Graph graph) {
        checkBoxesView.removeAllViews();

        final LayoutInflater inflater = LayoutInflater.from(this);

        for (GraphLine line : graph.getLines()) {
            final CheckBox checkBox = (CheckBox) inflater.inflate(R.layout.layout_line_checkbox, checkBoxesView, false);

            checkBox.setText(line.getName());
            checkBox.setChecked(true);
            checkBox.setTag(line.getLabel());

            if (supportButtonDrawable != null) {
                final Drawable button = supportButtonDrawable.getConstantState().newDrawable().mutate();
                button.setColorFilter(line.getColor(), PorterDuff.Mode.SRC_ATOP);
                checkBox.setButtonDrawable(button);
            } else {
                checkBox.setButtonTintList(ColorStateList.valueOf(line.getColor()));
            }

            checkBox.setOnCheckedChangeListener(this);

            checkBoxesView.addView(checkBox);
        }

        miniGraphVisializer.setGraph(graph);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final String lineLabel = (String) buttonView.getTag();
        miniGraphVisializer.setLineEnabled(lineLabel, isChecked);
    }

    private boolean useSupport() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP;
    }
}
