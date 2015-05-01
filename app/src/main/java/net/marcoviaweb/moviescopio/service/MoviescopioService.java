package net.marcoviaweb.moviescopio.service;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.widget.ArrayAdapter;

import net.marcoviaweb.moviescopio.data.MovieContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

public class MoviescopioService extends IntentService {
    private ArrayAdapter<String> mMovieAdapter;
    public static final String GENRE_QUERY_EXTRA = "gqe";
    private final String LOG_TAG = MoviescopioService.class.getSimpleName();

    public MoviescopioService() {
        super("MoviescopioService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String genreQuery = intent.getStringExtra(GENRE_QUERY_EXTRA);

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String apy_key = "54b406237a31ab795b7850ff7ddc2494";
        String sort_by = "popularity.desc";
        int page = 1;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
            final String QUERY_API_KEY = "api_key";
            final String QUERY_ORDEN = "sort_by";
            final String QUERY_PAGINA = "page";
            final String QUERY_GENERO = "with_genres";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_API_KEY, apy_key)
                    .appendQueryParameter(QUERY_ORDEN, sort_by)
                    .appendQueryParameter(QUERY_PAGINA, Integer.toString(page))
                    .appendQueryParameter(QUERY_GENERO, genreQuery)
                    .build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return;
            }
            forecastJsonStr = buffer.toString();
            getMovieDataFromJson(forecastJsonStr, genreQuery);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return;
    }

    private void getMovieDataFromJson(String movieJsonStr, String genreSetting) throws JSONException {

        final String OWM_LIST = "results";
        final String OWM_IDENTIFIER = "id";
        final String OWM_DATE_RELEASE = "release_date";
        final String OWM_POSTER_PATH = "poster_path";
        final String OWM_TITLE = "title";
        final String OWM_VOTE_AVERAGE = "vote_average";

        try {
            JSONObject forecastJson = new JSONObject(movieJsonStr);
            JSONArray movieArray = forecastJson.getJSONArray(OWM_LIST);

            Vector<ContentValues> cVMovieVector = new Vector<ContentValues>(movieArray.length());
            Vector<ContentValues> cVMovieByGenreVector = new Vector<ContentValues>(movieArray.length());

            for(int i = 0; i < movieArray.length(); i++) {
                // These are the values that will be collected.
                String identifier;
                String releaseDate;
                String posterPath;
                String title;
                String voteAverage;

                // Get the JSON object representing the movie
                JSONObject movie = movieArray.getJSONObject(i);
                identifier = movie.getString(OWM_IDENTIFIER);
                releaseDate = movie.getString(OWM_DATE_RELEASE);
                posterPath = movie.getString(OWM_POSTER_PATH);
                title = movie.getString(OWM_TITLE);
                voteAverage = movie.getString(OWM_VOTE_AVERAGE);

                //Movie content values
                ContentValues movieValues = new ContentValues();
                movieValues.put(MovieContract.MovieEntry.COLUMN_IDENTIFIER, identifier);
                movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, posterPath);
                movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
                movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
                movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);

                //Genre movie content values
                ContentValues movieByGenreValues = new ContentValues();
                movieByGenreValues.put(MovieContract.GenreMovieEntry.COLUMN_MOVIE_KEY, identifier);
                movieByGenreValues.put(MovieContract.GenreMovieEntry.COLUMN_GENRE_KEY, genreSetting);

                cVMovieVector.add(movieValues);
                cVMovieByGenreVector.add(movieByGenreValues);
            }

            int inserted = 0;
            // add to database
            if ( cVMovieVector.size() > 0 ) {
                //Movies
                ContentValues[] cvMovieArray = new ContentValues[cVMovieVector.size()];
                cVMovieVector.toArray(cvMovieArray);
                this.getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, cvMovieArray);
                //MoviesByGenre
                ContentValues[] cvMovieByGenreArray = new ContentValues[cVMovieByGenreVector.size()];
                cVMovieByGenreVector.toArray(cvMovieByGenreArray);
                this.getContentResolver().bulkInsert(MovieContract.GenreMovieEntry.CONTENT_URI, cvMovieByGenreArray);
            }

            Log.d(LOG_TAG, "Moviescopio Service Complete. " + cVMovieVector.size() + " Inserted");
            /*
            Uri movieByGenreUri = MovieContract.GenreMovieEntry.buildMovieByGenre(genreSetting);
            Cursor cur = mContext.getContentResolver().query(movieByGenreUri, null, null, null, null);
            //Log.d("** cantidad cursor **: ", Integer.toString(cur.getCount()) );

            cVMovieByGenreVector = new Vector<ContentValues>(cur.getCount());
            if ( cur.moveToFirst() ) {
                do {
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cur, cv);
                    //Log.d(LOG_TAG, cv.getAsString(MovieEntry.COLUMN_IDENTIFIER));
                    cVMovieByGenreVector.add(cv);
                } while (cur.moveToNext());
            }

            Log.d(LOG_TAG, "FetchWeatherTask Genre movie Complete. " + cVMovieByGenreVector.size() + " Inserted");
            */
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    long addMovie(String identifier, String posterPath, String releaseDate, String title, double voteAverage) {
        long movieId;

        // First, check if the location with this city name exists in the db
        Cursor movieCursor = this.getContentResolver().query(
                MovieContract.MovieEntry.CONTENT_URI,
                new String[]{MovieContract.MovieEntry._ID},
                MovieContract.MovieEntry.COLUMN_IDENTIFIER + " = ?",
                new String[]{identifier},
                null);

        if (movieCursor.moveToFirst()) {
            int movieIdIndex = movieCursor.getColumnIndex(MovieContract.MovieEntry._ID);
            movieId = movieCursor.getLong(movieIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues movieValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            movieValues.put(MovieContract.MovieEntry.COLUMN_IDENTIFIER, identifier);
            movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, posterPath);
            movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
            movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
            movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);

            // Finally, insert location data into the database.
            Uri insertedUri = this.getContentResolver().insert(
                    MovieContract.MovieEntry.CONTENT_URI, movieValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            movieId = ContentUris.parseId(insertedUri);
        }

        movieCursor.close();
        // Wait, that worked?  Yes!
        return movieId;
    }

    public static class AlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Intent sendIntent = new Intent(context, MoviescopioService.class);
            sendIntent.putExtra(MoviescopioService.GENRE_QUERY_EXTRA, intent.getStringExtra(MoviescopioService.GENRE_QUERY_EXTRA));
            context.startService(sendIntent);

        }
    }
}
