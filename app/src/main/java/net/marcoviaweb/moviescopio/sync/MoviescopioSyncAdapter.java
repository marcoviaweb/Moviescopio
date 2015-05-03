package net.marcoviaweb.moviescopio.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import net.marcoviaweb.moviescopio.MainActivity;
import net.marcoviaweb.moviescopio.R;
import net.marcoviaweb.moviescopio.Utility;
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

public class MoviescopioSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = MoviescopioSyncAdapter.class.getSimpleName();
    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final long PERIOD_IN_MILLIS = 1000 * 60 * 5; //5 minutos //1000 * 60 * 60 * 24;
    private static final int MOVIE_NOTIFICATION_ID = 3007;

    private static final String[] NOTIFY_MOVIE_PROJECTION = new String[] {
            MovieContract.MovieEntry.COLUMN_IDENTIFIER,
            MovieContract.MovieEntry.COLUMN_TITLE,
            MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE,
    };

    // these indices must match the projection
    private static final int INDEX_IDENTIFIER = 0;
    private static final int INDEX_TITLE = 1;
    private static final int INDEX_VOTE_AVERAGE = 2;

    public MoviescopioSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "Starting sync");
        String genreQuery = Utility.getPreferredGenre(getContext());

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
                getContext().getContentResolver().bulkInsert(MovieContract.MovieEntry.CONTENT_URI, cvMovieArray);
                //MoviesByGenre
                ContentValues[] cvMovieByGenreArray = new ContentValues[cVMovieByGenreVector.size()];
                cVMovieByGenreVector.toArray(cvMovieByGenreArray);
                getContext().getContentResolver().bulkInsert(MovieContract.GenreMovieEntry.CONTENT_URI, cvMovieByGenreArray);

                notifyMovie();
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

    private void notifyMovie() {
        Log.d(LOG_TAG, "++++++++++++Se llamo a notifyMovie ...!!!");
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {
            Log.d(LOG_TAG, "++++++++++++Es true...!!!");
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            if (System.currentTimeMillis() - lastSync >= PERIOD_IN_MILLIS) {
                Log.d(LOG_TAG, "++++++++++++Deberia realizar la sincronización ...!!!");
                // NotificationCompatBuilder is a very convenient way to build backward-compatible
                // notifications.  Just throw in some data.
                Resources resources = context.getResources();
                int iconId = R.drawable.ic_notification;
                Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                        R.drawable.art_notification);
                String title = context.getString(R.string.app_name);
                // Define the text of the forecast.
                String contentText = String.format(context.getString(R.string.format_notification),
                        "Moviescopio++",
                        "9.5");

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getContext())
                            .setColor(resources.getColor(R.color.moviescopio_theme_light))
                            .setSmallIcon(iconId)
                            .setLargeIcon(largeIcon)
                            .setContentTitle(title)
                            .setContentText(contentText);

                // Make something interesting happen when the user clicks on the notification.
                // In this case, opening the app is sufficient.
                Intent resultIntent = new Intent(context, MainActivity.class);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);

                NotificationManager mNotificationManager =
                        (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                // WEATHER_NOTIFICATION_ID allows you to update the notification later on.
                mNotificationManager.notify(MOVIE_NOTIFICATION_ID, mBuilder.build());

                //refreshing last sync
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(lastNotificationKey, System.currentTimeMillis());
                editor.commit();
            }
        }
    }

    long addMovie(String identifier, String posterPath, String releaseDate, String title, double voteAverage) {
        long movieId;

        // First, check if the location with this city name exists in the db
        Cursor movieCursor = getContext().getContentResolver().query(
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
            Uri insertedUri = getContext().getContentResolver().insert(
                    MovieContract.MovieEntry.CONTENT_URI, movieValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            movieId = ContentUris.parseId(insertedUri);
        }

        movieCursor.close();
        // Wait, that worked?  Yes!
        return movieId;
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        MoviescopioSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}