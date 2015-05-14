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

import com.squareup.picasso.Picasso;

import net.marcoviaweb.moviescopio.data.MovieContract;
import net.marcoviaweb.moviescopio.data.MovieContract.MovieEntry;

public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();
    static final String DETAIL_URI = "URI";
    static final String BASE_POSTER_PATH = "http://image.tmdb.org/t/p/w500";

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
            MovieEntry.COLUMN_BACKDROP_PATH,
            MovieEntry.COLUMN_POPULARITY,
            MovieEntry.COLUMN_VOTE_COUNT,
    };

    private static final int COL_MOVIE_ID = 0;
    private static final int COL_MOVIE_IDENTIFIER = 1;
    private static final int COL_MOVIE_POSTER_PATH = 2;
    private static final int COL_RELEASE_DATE = 3;
    private static final int COL_TITLE = 4;
    private static final int COL_VOTE_AVERAGE = 5;
    private static final int COL_BACKDROP_PATH = 6;
    private static final int COL_POPULARITY = 7;
    private static final int COL_VOTE_COUNT = 8;

    private TextView mtitleView;
    private TextView mdateReleaseView;
    private ImageView mposterView;
    private TextView mvoteCountView;
    private ImageView mvoteAverageIconView;

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

        mtitleView = (TextView) rootView.findViewById(R.id.detail_title_textview);
        mdateReleaseView = (TextView) rootView.findViewById(R.id.detail_daterelease_textview);
        mposterView = (ImageView) rootView.findViewById(R.id.detail_poster_imageview);
        mvoteCountView = (TextView) rootView.findViewById(R.id.detail_votecount_textview);
        mvoteAverageIconView = (ImageView) rootView.findViewById(R.id.detail_voteaverage_icon);

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
            String title = String.format(getActivity().getString(R.string.format_title), data.getString(COL_TITLE));
            mtitleView.setText(title);

            String releaseDate = String.format(getActivity().getString(R.string.format_release_date), data.getString(COL_RELEASE_DATE));
            mdateReleaseView.setText(releaseDate);

            String voteCount = String.format(getActivity().getString(R.string.format_vote_count), data.getString(COL_VOTE_COUNT));
            mvoteCountView.setText(voteCount);

            float voteAverage = Float.parseFloat(data.getString(COL_VOTE_AVERAGE));
            mvoteAverageIconView.setImageResource(Utility.getArtResourceForAverageMovie(voteAverage));

            String posterPath = data.getString(COL_MOVIE_POSTER_PATH);
            Picasso.with(getActivity())
                    .load(BASE_POSTER_PATH + posterPath)
                    .into(mposterView);

            mMovieStr = String.format("%s - %s - %s", releaseDate, title, voteAverage);

            if (mShareActionProvider != null) {
                mShareActionProvider.setShareIntent(createShareMovieIntent());
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }
}