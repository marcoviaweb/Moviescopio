package net.marcoviaweb.moviescopio.data;

import android.net.Uri;
import android.test.AndroidTestCase;

/*
    Students: This is NOT a complete test for the WeatherContract --- just for the functions
    that we expect you to write.
 */
public class TestMovieContract extends AndroidTestCase {

    // intentionally includes a slash to make sure Uri is getting quoted correctly
    private static final String TEST_GENRE = "/878";

    /*
        Students: Uncomment this out to test your weather location function.
     */
    public void testBuildMovieByGenre() {
        Uri locationUri = MovieContract.GenreMovieEntry.buildMovieByGenre(TEST_GENRE);
        assertNotNull("Error: Null Uri returned.  You must fill-in buildMovieByGenren in " +
                        "MovieContract.", locationUri);
        //TODO se dejo :(
        assertEquals("Error: Weather location Uri doesn't match our expected result",
                locationUri.toString(),
                "content://net.marcoviaweb.moviescopio/genreMovie/%2F878");
    }
}
