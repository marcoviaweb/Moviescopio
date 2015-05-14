package net.marcoviaweb.moviescopio.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import net.marcoviaweb.moviescopio.data.MovieContract.MovieEntry;

public class MovieDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;

    static final String DATABASE_NAME = "movie.db";

    public MovieDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // Create a table to hold movies.
        final String SQL_CREATE_MOVIE_TABLE = "CREATE TABLE " + MovieEntry.TABLE_NAME + " (" +
                MovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MovieEntry.COLUMN_IDENTIFIER + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_POSTER_PATH + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_RELEASE_DATE + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_TITLE + " TEXT NOT NULL," +
                MovieEntry.COLUMN_VOTE_AVERAGE + " REAL NOT NULL, " +
                MovieEntry.COLUMN_BACKDROP_PATH + " TEXT NOT NULL, " +
                MovieEntry.COLUMN_POPULARITY + " REAL NOT NULL, " +
                MovieEntry.COLUMN_VOTE_COUNT + " REAL NOT NULL, " +
                " UNIQUE (" + MovieEntry.COLUMN_IDENTIFIER + ") ON CONFLICT REPLACE);";

        // Create a table to hold genre movies.
        final String SQL_CREATE_GENRE_MOVIE_TABLE = "CREATE TABLE " + MovieContract.GenreMovieEntry.TABLE_NAME + " (" +
                MovieContract.GenreMovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                MovieContract.GenreMovieEntry.COLUMN_MOVIE_KEY + " INTEGER NOT NULL, " +
                MovieContract.GenreMovieEntry.COLUMN_GENRE_KEY + " INTEGER NOT NULL, " +
                /*
                // Set up the movie column as a foreign key to movie table.
                " FOREIGN KEY (" + MovieContract.GenreMovieEntry.COLUMN_MOVIE_KEY + ") REFERENCES " +
                MovieEntry.TABLE_NAME + " (" + MovieEntry._ID + "), " +
                */
                " UNIQUE (" + MovieContract.GenreMovieEntry.COLUMN_MOVIE_KEY + ", " + MovieContract.GenreMovieEntry.COLUMN_GENRE_KEY + ") ON CONFLICT REPLACE);";


        sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_GENRE_MOVIE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieContract.GenreMovieEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + MovieEntry.TABLE_NAME);

        onCreate(sqLiteDatabase);
    }
}
