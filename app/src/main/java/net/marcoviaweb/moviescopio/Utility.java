package net.marcoviaweb.moviescopio;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Utility {
    public static String getPreferredGenre(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(context.getString(R.string.pref_genre_key),
                context.getString(R.string.pref_genre_default));
    }

    public static int getArtResourceForAverageMovie(float average) {
        if (average < 1) {
            return R.drawable.av_00;
        } else if (average < 2) {
            return R.drawable.av_05;
        } else if (average < 3) {
            return R.drawable.av_10;
        } else if (average < 4) {
            return R.drawable.av_15;
        } else if (average < 5) {
            return R.drawable.av_20;
        } else if (average < 6) {
            return R.drawable.av_25;
        } else if (average < 7) {
            return R.drawable.av_30;
        } else if (average < 8) {
            return R.drawable.av_35;
        } else if (average < 9) {
            return R.drawable.av_40;
        } else if (average < 10) {
            return R.drawable.av_45;
        } else if (average == 10) {
            return R.drawable.av_50;
        }
        return -1;
    }
}