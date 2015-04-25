package net.marcoviaweb.moviescopio;

import android.annotation.TargetApi;
import android.database.Cursor;
import android.test.AndroidTestCase;

import net.marcoviaweb.moviescopio.data.MovieContract;

public class TestFetchWeatherTask extends AndroidTestCase{
    static final String ADD_MOVIE_IDENTIFIER = "157336";
    static final String ADD_MOVIE_POSTER_PATH = "/fasljdfladsjf.jpg";
    static final String ADD_MOVIE_RELEASE_DATE = "02-02-2015";
    static final String ADD_MOVIE_TITLE = "YOYO";
    static final double ADD_MOVIE_VOTE_AVERAGE = 1.6;

    /*
        Students: uncomment testAddMovie after you have written the AddLocation function.
        This test will only run on API level 11 and higher because of a requirement in the
        content provider.
     */
    @TargetApi(11)
    public void testAddMovie() {
        // start from a clean state
        getContext().getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI,
                MovieContract.MovieEntry.COLUMN_IDENTIFIER + " = ?",
                new String[]{ADD_MOVIE_IDENTIFIER});

        FetchWeatherTask fwt = new FetchWeatherTask(getContext(), null);
        long movieId = fwt.addMovie(ADD_MOVIE_IDENTIFIER, ADD_MOVIE_POSTER_PATH, ADD_MOVIE_RELEASE_DATE, ADD_MOVIE_TITLE, ADD_MOVIE_VOTE_AVERAGE);

        // does addMovie return a valid record ID?
        assertFalse("Error: addMovie returned an invalid ID on insert",
                movieId == -1);

        // test all this twice
        for ( int i = 0; i < 2; i++ ) {

            // does the ID point to our location?
            Cursor movieCursor = getContext().getContentResolver().query(
                    MovieContract.MovieEntry.CONTENT_URI,
                    new String[]{
                            MovieContract.MovieEntry._ID,
                            MovieContract.MovieEntry.COLUMN_IDENTIFIER,
                            MovieContract.MovieEntry.COLUMN_POSTER_PATH,
                            MovieContract.MovieEntry.COLUMN_RELEASE_DATE,
                            MovieContract.MovieEntry.COLUMN_TITLE,
                            MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE
                    },
                    MovieContract.MovieEntry.COLUMN_IDENTIFIER + " = ?",
                    new String[]{ADD_MOVIE_IDENTIFIER},
                    null);

            // these match the indices of the projection
            if (movieCursor.moveToFirst()) {
                assertEquals("Error: the queried value of locationId does not match the returned value" +
                        "from addMovie", movieCursor.getLong(0), movieId);
                assertEquals("Error: the queried value of location setting is incorrect", movieCursor.getString(1), ADD_MOVIE_IDENTIFIER);
                assertEquals("Error: the queried value of location city is incorrect", movieCursor.getString(2), ADD_MOVIE_POSTER_PATH);
                assertEquals("Error: the queried value of latitude is incorrect", movieCursor.getString(3), ADD_MOVIE_RELEASE_DATE);
                assertEquals("Error: the queried value of longitude is incorrect", movieCursor.getString(4), ADD_MOVIE_TITLE);
                assertEquals("Error: the queried value of longitude is incorrect", movieCursor.getDouble(5), ADD_MOVIE_VOTE_AVERAGE);
            } else {
                fail("Error: the id you used to query returned an empty cursor");
            }

            // there should be no more records
            assertFalse("Error: there should be only one record returned from a location query",
                    movieCursor.moveToNext());

            // add the location again
            long newMovieId = fwt.addMovie(ADD_MOVIE_IDENTIFIER, ADD_MOVIE_POSTER_PATH, ADD_MOVIE_RELEASE_DATE, ADD_MOVIE_TITLE, ADD_MOVIE_VOTE_AVERAGE);

            assertEquals("Error: inserting a location again should return the same ID", movieId, newMovieId);
        }
        // reset our state back to normal
        getContext().getContentResolver().delete(MovieContract.MovieEntry.CONTENT_URI,
                MovieContract.MovieEntry.COLUMN_IDENTIFIER + " = ?",
                new String[]{ADD_MOVIE_IDENTIFIER});

        // clean up the test so that other tests can use the content provider
        getContext().getContentResolver().
                acquireContentProviderClient(MovieContract.MovieEntry.CONTENT_URI).
                getLocalContentProvider().shutdown();
    }
}
