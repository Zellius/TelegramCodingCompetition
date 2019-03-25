package com.almadevelop.telegram.chart;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;

public final class Utils {
    private Utils() {
    }

    public static int compareLong(long x, long y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return Long.compare(x, y);
        } else {
            return (x < y) ? -1 : ((x == y) ? 0 : 1);
        }
    }

    @ColorInt
    public static int getColor(@NonNull Context context, @ColorRes int resId) {
        final Resources res = context.getResources();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return res.getColor(resId, null);
        } else {
            return res.getColor(resId);
        }
    }
}
