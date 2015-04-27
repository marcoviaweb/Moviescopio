package net.marcoviaweb.moviescopio;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.marcoviaweb.moviescopio.data.MovieContract;
import net.marcoviaweb.moviescopio.data.MovieContract.MovieEntry;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";

    private static final String MOVIE_SHARE_HASHTAG = " #moviescopio";

    private ShareActionProvider mShareActionProvider;
    private String mMovieStr;
    private Uri mUri;

    private static final int DETAIL_LOADER = 0;

    private static final String[] MOVIE_COLUMNS = {
            MovieEntry.TABLE_NAME + "." + MovieEntry._ID,
            MovieEntry.COLUMN_IDENTIFIER,
            MovieEntry.COLUMN_POSTER_PATH,
            MovieEntry.COLUMN_RELEASE_DATE,
            MovieEntry.COLUMN_TITLE,
            MovieEntry.COLUMN_VOTE_AVERAGE,
    };

    private final int COL_MOVIE_ID = 0;
    private final int COL_MOVIE_IDENTIFIER = 1;
    private final int COL_MOVIE_POSTER_PATH = 2;
    private final int COL_RELEASE_DATE = 3;
    private final int COL_TITLE = 4;
    private final int COL_VOTE_AVERAGE = 5;

    private TextView mvoteAverageView;
    private TextView mtitleView;
    private TextView mdateReleaseView;
    private ImageView mposterView;
    private TextView mposterPathView;

    public DetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mUri = arguments.getParcelable(DetailFragment.DETAIL_URI);
        }

        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);

        mvoteAverageView = (TextView) rootView.findViewById(R.id.detail_voteaverage_textview);
        mtitleView = (TextView) rootView.findViewById(R.id.detail_title_textview);
        mdateReleaseView = (TextView) rootView.findViewById(R.id.detail_daterelease_textview);
        mposterView = (ImageView) rootView.findViewById(R.id.detail_poster_imageview);
        mposterPathView = (TextView) rootView.findViewById(R.id.detail_posterPath_textview);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.detailfragment, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mMovieStr != null) {
            mShareActionProvider.setShareIntent(createShareMovieIntent());
        }
    }

    private Intent createShareMovieIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, mMovieStr + MOVIE_SHARE_HASHTAG);
        return shareIntent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(DETAIL_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onGenreChanged(String newGenre) {
        Uri uri = mUri;
        if (null != uri) {
            String movieId = MovieContract.MovieEntry.getMovieIdFromUri(uri);
            Uri updatedUri = MovieContract.MovieEntry.buildMovieId(movieId);
            mUri = updatedUri;
            getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if ( null != mUri ) {
            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(
                    getActivity(),
                    mUri,
                    MOVIE_COLUMNS,
                    null,
                    null,
                    null
            );
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null && data.moveToFirst()) {
            String releaseDate = data.getString(COL_RELEASE_DATE);
            mdateReleaseView.setText(releaseDate);

            String title = data.getString(COL_TITLE);
            mtitleView.setText(title);

            String voteAverage = data.getString(COL_VOTE_AVERAGE);
            mvoteAverageView.setText(voteAverage);

            String posterPath = data.getString(COL_MOVIE_POSTER_PATH);
            mposterPathView.setText(posterPath);

            //TODO falta setear la imagen
            //ImageView mposterView;

            mMovieStr = String.format("%s - %s - %s", releaseDate, title, voteAverage);

            // If onCreateOptionsMenu has already happened, we need to update the share intent now.
            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareMovieIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }
}