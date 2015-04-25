package net.marcoviaweb.moviescopio.data;

import android.content.UriMatcher;
import android.net.Uri;
import android.test.AndroidTestCase;

public class TestUriMatcher extends AndroidTestCase {

    private static final String MOVIE = "102";
    private static final String GENRE = "103";

    private static final Uri MOVIE_DIR = MovieContract.MovieEntry.buildMovieId(MOVIE);
    private static final Uri MOVIE_BY_GENRE_DIR = MovieContract.GenreMovieEntry.buildMovieByGenre(GENRE);

    public void testUriMatcher() {
        UriMatcher testMatcher = MovieProvider.buildUriMatcher();

        assertEquals("Error: The MOVIE URI was matched incorrectly.", testMatcher.match(MOVIE_DIR), MovieProvider.MOVIE_WITH_ID);
        assertEquals("Error: The MOVIE BY GENRE URI was matched incorrectly.", testMatcher.match(MOVIE_BY_GENRE_DIR), MovieProvider.MOVIE_WITH_GENRE);
    }
}
