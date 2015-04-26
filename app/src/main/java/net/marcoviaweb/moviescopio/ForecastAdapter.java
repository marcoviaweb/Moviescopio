package net.marcoviaweb.moviescopio;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * {@link ForecastAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class ForecastAdapter extends CursorAdapter {
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    /*
        Remember that these views are reused as needed.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_movies, parent, false);

        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // our view is pretty simple here --- just a text view
        // we'll keep the UI functional with a simple (and slow!) binding.

        String movieId = cursor.getString(MovieFragment.COL_MOVIE_IDENTIFIER);

        String voteAverage = cursor.getString(MovieFragment.COL_VOTE_AVERAGE);
        Log.d("____voteAverage: ", voteAverage);
        TextView voteAverageView = (TextView) view.findViewById(R.id.list_item_voteaverage_textview);
        voteAverageView.setText(voteAverage);

        String title = cursor.getString(MovieFragment.COL_TITLE);
        Log.d("_____title: ", title);
        TextView titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
        titleView.setText(title);

        String dateRelease = cursor.getString(MovieFragment.COL_RELEASE_DATE);
        Log.d("_____dateRelease: ", dateRelease);
        TextView dateReleaseView = (TextView) view.findViewById(R.id.list_item_daterelease_textview);
        dateReleaseView.setText(dateRelease);

    }
}