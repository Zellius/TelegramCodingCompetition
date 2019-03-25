package com.almadevelop.telegram.chart.graph;

import android.support.annotation.NonNull;

import java.util.List;

public interface GraphObject {
    @NonNull
    List<Long> getData();

    long getTopExtremum();

    long getLowExtremum();
}
