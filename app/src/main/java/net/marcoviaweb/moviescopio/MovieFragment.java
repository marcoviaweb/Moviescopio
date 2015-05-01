package net.marcoviaweb.moviescopio;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.marcoviaweb.moviescopio.data.MovieContract;
import net.marcoviaweb.moviescopio.service.MoviescopioService;
import net.marcoviaweb.moviescopio.sync.MoviescopioSyncAdapter;

public class MovieFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private MovieAdapter mMovieAdapter;

    private ListView mListView;
    private int mPosition = ListView.INVALID_POSITION;
    private boolean mUsePrincipalLayout;

    private static final String SELECTED_KEY = "selected_position";

    private static final int MOVIE_LOADER = 0;

    private static final String[] MOVIE_COLUMNS = {
            MovieContract.MovieEntry.TABLE_NAME + "." + MovieContract.MovieEntry._ID,
            MovieContract.MovieEntry.COLUMN_IDENTIFIER,
            MovieContract.MovieEntry.COLUMN_POSTER_PATH,
            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE,
    };

    static final int COL_MOVIE_ID = 0;
    static final int COL_MOVIE_IDENTIFIER = 1;
    static final int COL_MOVIE_POSTER_PATH = 2;
    static final int COL_RELEASE_DATE = 3;
    static final int COL_TITLE = 4;
    static final int COL_VOTE_AVERAGE = 5;

    public interface Callback {
        public void onItemSelected(Uri dateUri);
    }

    public MovieFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.moviefragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateMovie();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mMovieAdapter = new MovieAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mListView = (ListView) rootView.findViewById(R.id.listview_movies);
        mListView.setAdapter(mMovieAdapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredGenre(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(MovieContract.MovieEntry.buildMovieId(
                                    cursor.getString(COL_MOVIE_IDENTIFIER)
                            ));
                }
                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
            mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mMovieAdapter.setUsePrincipalLayout(mUsePrincipalLayout);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(MOVIE_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void onGenreChanged() {
        updateMovie();
        getLoaderManager().restartLoader(MOVIE_LOADER, null, this);
    }

    private void updateMovie() {
        Intent alarmIntent = new Intent(getActivity(), MoviescopioService.AlarmReceiver.class);
        /*
        alarmIntent.putExtra(MoviescopioService.GENRE_QUERY_EXTRA, Utility.getPreferredGenre(getActivity()));

        PendingIntent pi = PendingIntent.getBroadcast(getActivity(), 0,alarmIntent,PendingIntent.FLAG_ONE_SHOT);
        AlarmManager am = (AlarmManager)getActivity().getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 8000, pi);
        */
        MoviescopioSyncAdapter.syncImmediately(getActivity());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
            outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {

        String sortOrder = MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE + " DESC";

        String genreSetting = Utility.getPreferredGenre(getActivity());
        Uri movieByGenreUri = MovieContract.GenreMovieEntry.buildMovieByGenre(genreSetting);

        return new CursorLoader(getActivity(),
                movieByGenreUri,
                MOVIE_COLUMNS,
                null,
                null,
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mMovieAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
            mListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mMovieAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean usePrincipalLayout) {
        mUsePrincipalLayout = usePrincipalLayout;
        if (mMovieAdapter != null) {
            mMovieAdapter.setUsePrincipalLayout(mUsePrincipalLayout);
        }
    }
}
