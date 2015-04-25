package net.marcoviaweb.moviescopio;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ArrayAdapter;

import net.marcoviaweb.moviescopio.data.MovieContract;
import net.marcoviaweb.moviescopio.data.MovieContract.GenreMovieEntry;
import net.marcoviaweb.moviescopio.data.MovieContract.MovieEntry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    private ArrayAdapter<String> mForecastAdapter;
    private final Context mContext;

    public FetchWeatherTask(Context context, ArrayAdapter<String> forecastAdapter) {
        mContext = context;
        mForecastAdapter = forecastAdapter;
    }

    private boolean DEBUG = true;

    /* The date/time conversion code is going to be moved outside the asynctask later,
     * so for convenience we're breaking it out into its own method now.
     */
    private String getReadableDateString(long time){
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        Date date = new Date(time);
        SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
        return format.format(date).toString();
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    /*
    private String formatHighLows(double high, double low) {
        // Data is fetched in Celsius by default.
        // If user prefers to see in Fahrenheit, convert the values here.
        // We do this rather than fetching in Fahrenheit so that the user can
        // change this option without us having to re-fetch the data once
        // we start storing the values in a database.
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(mContext);
        String unitType = sharedPrefs.getString(
                mContext.getString(R.string.pref_units_key),
                mContext.getString(R.string.pref_units_metric));

        if (unitType.equals(mContext.getString(R.string.pref_units_imperial))) {
            high = (high * 1.8) + 32;
            low = (low * 1.8) + 32;
        } else if (!unitType.equals(mContext.getString(R.string.pref_units_metric))) {
            Log.d(LOG_TAG, "Unit type not found: " + unitType);
        }

        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }
    */

    /**
     * Helper method to handle insertion of a new location in the weather database.
     *
     * @param identifier The location string used to request updates from the server.
     * @param posterPath A human-readable city name, e.g "Mountain View"
     * @param releaseDate the latitude of the city
     * @param title the longitude of the city
     * @param voteAverage the longitude of the city
     * @return the row ID of the added location.
     */
    long addMovie(String identifier, String posterPath, String releaseDate, String title, double voteAverage) {
        long movieId;

        // First, check if the location with this city name exists in the db
        Cursor movieCursor = mContext.getContentResolver().query(
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
            Uri insertedUri = mContext.getContentResolver().insert(MovieContract.MovieEntry.CONTENT_URI, movieValues);

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            movieId = ContentUris.parseId(insertedUri);
        }

        movieCursor.close();
        // Wait, that worked?  Yes!
        return movieId;
    }

    /*
        Students: This code will allow the FetchWeatherTask to continue to return the strings that
        the UX expects so that we can continue to test the application even once we begin using
        the database.
     */
    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv) {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for ( int i = 0; i < cvv.size(); i++ ) {
            ContentValues movieValues = cvv.elementAt(i);

            resultStrs[i] = movieValues.getAsString(MovieEntry.COLUMN_RELEASE_DATE) +
                    " = " + movieValues.getAsString(MovieEntry.COLUMN_POSTER_PATH) +
                    " = " + movieValues.getAsString(MovieEntry.COLUMN_TITLE) +
                    " = " + movieValues.getAsString(MovieEntry.COLUMN_VOTE_AVERAGE);
        }
        return resultStrs;
    }

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     *
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */

    private String[] getMovieDataFromJson(String movieJsonStr, String genreSetting) throws JSONException {

        // Now we have a String representing the complete forecast in JSON Format.
        // Fortunately parsing is easy:  constructor takes the JSON string and converts it
        // into an Object hierarchy for us.

        // These are the names of the JSON objects that need to be extracted.

        // Movie information.  Each movie info is an element of the "results" array.
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
                movieValues.put(MovieEntry.COLUMN_IDENTIFIER, identifier);
                movieValues.put(MovieEntry.COLUMN_POSTER_PATH, posterPath);
                movieValues.put(MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
                movieValues.put(MovieEntry.COLUMN_TITLE, title);
                movieValues.put(MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);

                //Genre movie content values
                ContentValues movieByGenreValues = new ContentValues();
                movieByGenreValues.put(GenreMovieEntry.COLUMN_MOVIE_KEY, identifier);
                movieByGenreValues.put(GenreMovieEntry.COLUMN_GENRE_KEY, genreSetting);

                cVMovieVector.add(movieValues);
                cVMovieByGenreVector.add(movieByGenreValues);
            }

            int inserted = 0;
            // add to database
            if ( cVMovieVector.size() > 0 ) {
                //Movies
                ContentValues[] cvMovieArray = new ContentValues[cVMovieVector.size()];
                cVMovieVector.toArray(cvMovieArray);
                inserted = mContext.getContentResolver().bulkInsert(MovieEntry.CONTENT_URI, cvMovieArray);
                //MoviesByGenre
                ContentValues[] cvMovieByGenreArray = new ContentValues[cVMovieByGenreVector.size()];
                cVMovieByGenreVector.toArray(cvMovieByGenreArray);
                inserted = mContext.getContentResolver().bulkInsert(GenreMovieEntry.CONTENT_URI, cvMovieByGenreArray);
            }


            // Sort order:  Ascending, by date.
            String sortOrder = MovieEntry.COLUMN_RELEASE_DATE + " DESC";
            Uri movieByGenreUri = GenreMovieEntry.buildMovieByGenre(genreSetting);

            // Students: Uncomment the next lines to display what what you stored in the bulkInsert

            Cursor cur = mContext.getContentResolver().query(movieByGenreUri,
                    null, null, null, sortOrder);

            cVMovieVector = new Vector<ContentValues>(cur.getCount());
            if ( cur.moveToFirst() ) {
                do {
                    ContentValues cv = new ContentValues();
                    DatabaseUtils.cursorRowToContentValues(cur, cv);
                    cVMovieVector.add(cv);
                } while (cur.moveToNext());
            }


            Log.d(LOG_TAG, "FetchWeatherTask Movie Complete. " + cVMovieVector.size() + " Inserted");
            Log.d(LOG_TAG, "FetchWeatherTask Genre movie Complete. " + cVMovieByGenreVector.size() + " Inserted");

            String[] resultStrs = convertContentValuesToUXFormat(cVMovieVector);
            return resultStrs;

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        return null;
    }



    @Override
    protected String[] doInBackground(String... params) {
         /*

        // If there's no zip code, there's nothing to look up.  Verify size of params.
        if (params.length == 0) {
            return null;
        }
        String locationQuery = params[0];

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final String QUERY_PARAM = "q";
            final String FORMAT_PARAM = "mode";
            final String UNITS_PARAM = "units";
            final String DAYS_PARAM = "cnt";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
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
                // Nothing to do.
                return null;
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
                return null;
            }
            forecastJsonStr = buffer.toString();
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
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

        try {
            return getMovieDataFromJson(forecastJsonStr, locationQuery);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
        */
        // This will only happen if there was an error getting or parsing the forecast.
        return null;

    }


    @Override
    protected void onPostExecute(String[] result) {
        if (result != null && mForecastAdapter != null) {
            mForecastAdapter.clear();
            for(String dayForecastStr : result) {
                mForecastAdapter.add(dayForecastStr);
            }
            // New data is back from the server.  Hooray!
        }
    }
}