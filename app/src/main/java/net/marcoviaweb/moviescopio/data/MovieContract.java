package net.marcoviaweb.moviescopio.data;

import android.provider.BaseColumns;


/**
 * Defines table and column names for the Movie database.
 */
public class MovieContract {

    /* Inner class that defines the table contents of the movie table */
    public static final class MovieEntry implements BaseColumns {

        public static final String TABLE_NAME = "movie";

        // Columns
        public static final String COLUMN_IDENTIFIER = "identifier";
        public static final String COLUMN_POSTER_PATH = "poster_path";
        public static final String COLUMN_RELEASE_DATE = "release_date";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_VOTE_AVERAGE = "vote_average";
    }
}
