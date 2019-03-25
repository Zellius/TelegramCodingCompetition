package com.almadevelop.telegram.chart.parser;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

final class Constants {
    public static final int COLUMN_TYPE_X = 0;
    public static final int COLUMN_TYPE_LINE = 1;

    private Constants() {
    }

    @Retention(SOURCE)
    @IntDef({COLUMN_TYPE_X, COLUMN_TYPE_LINE})
    public @interface ColumnType {
    }
}
