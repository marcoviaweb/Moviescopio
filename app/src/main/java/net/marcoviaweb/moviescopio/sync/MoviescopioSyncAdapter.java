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
    private static final long PERIOD_IN_MILLIS = 1000 * 60 * 60 * 24 * 1;
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

        String genreQuery = Utility.getPreferredGenre(getContext());

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String movieJsonStr = null;

        String apy_key = "54b406237a31ab795b7850ff7ddc2494";
        String sort_by = "popularity.desc";
        int page = 1;

        try {
            final String MOVIE_BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
            final String QUERY_API_KEY = "api_key";
            final String QUERY_ORDEN = "sort_by";
            final String QUERY_PAGINA = "page";
            final String QUERY_GENERO = "with_genres";

            Uri builtUri = Uri.parse(MOVIE_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_API_KEY, apy_key)
                    .appendQueryParameter(QUERY_ORDEN, sort_by)
                    .appendQueryParameter(QUERY_PAGINA, Integer.toString(page))
                    .appendQueryParameter(QUERY_GENERO, genreQuery)
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();

            if (inputStream == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                return;
            }

            movieJsonStr = buffer.toString();
            getMovieDataFromJson(movieJsonStr, genreQuery);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
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

        final String OWM_BACKDROP_PATH = "backdrop_path";
        final String OWM_POPULARITY = "popularity";
        final String OWM_VOTE_COUNT = "vote_count";

        try {
            JSONObject movieJson = new JSONObject(movieJsonStr);
            JSONArray movieArray = movieJson.getJSONArray(OWM_LIST);

            Vector<ContentValues> cVMovieVector = new Vector<ContentValues>(movieArray.length());
            Vector<ContentValues> cVMovieByGenreVector = new Vector<ContentValues>(movieArray.length());

            for(int i = 0; i < movieArray.length(); i++) {
                String identifier;
                String releaseDate;
                String posterPath;
                String title;
                String voteAverage;
                String backdropPath;
                String popularity;
                String voteCount;

                JSONObject movie = movieArray.getJSONObject(i);
                identifier = movie.getString(OWM_IDENTIFIER);
                releaseDate = movie.getString(OWM_DATE_RELEASE);
                posterPath = movie.getString(OWM_POSTER_PATH);
                title = movie.getString(OWM_TITLE);
                voteAverage = movie.getString(OWM_VOTE_AVERAGE);

                backdropPath = movie.getString(OWM_BACKDROP_PATH);
                popularity = movie.getString(OWM_POPULARITY);
                voteCount = movie.getString(OWM_VOTE_COUNT);

                ContentValues movieValues = new ContentValues();
                movieValues.put(MovieContract.MovieEntry.COLUMN_IDENTIFIER, identifier);
                movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, posterPath);
                movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
                movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
                movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);
                movieValues.put(MovieContract.MovieEntry.COLUMN_BACKDROP_PATH, backdropPath);
                movieValues.put(MovieContract.MovieEntry.COLUMN_POPULARITY, popularity);
                movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_COUNT, voteCount);

                ContentValues movieByGenreValues = new ContentValues();
                movieByGenreValues.put(MovieContract.GenreMovieEntry.COLUMN_MOVIE_KEY, identifier);
                movieByGenreValues.put(MovieContract.GenreMovieEntry.COLUMN_GENRE_KEY, genreSetting);

                cVMovieVector.add(movieValues);
                cVMovieByGenreVector.add(movieByGenreValues);
            }

            int inserted = 0;

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

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    private void notifyMovie() {
        Context context = getContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if ( displayNotifications ) {
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);

            //if (true) {
            if (System.currentTimeMillis() - lastSync >= PERIOD_IN_MILLIS) {
                Log.v("+++++++++++", "notificar pero ya !!!");

                Uri ramdonMovieUri = MovieContract.MovieEntry.buildRandomMovie();
                Cursor cursor = context.getContentResolver().query(ramdonMovieUri, NOTIFY_MOVIE_PROJECTION, null, null, null);

                if (cursor.moveToFirst()) {

                    String movieIdentifier = cursor.getString(INDEX_IDENTIFIER);
                    String movieTitle = cursor.getString(INDEX_TITLE);
                    String movieVoteAverage = cursor.getString(INDEX_VOTE_AVERAGE);

                    Resources resources = context.getResources();
                    int iconId = R.drawable.ic_notification;
                    Bitmap largeIcon = BitmapFactory.decodeResource(resources,
                            R.drawable.art_notification);
                    String title = context.getString(R.string.app_name);

                    String contentText = String.format(context.getString(R.string.format_notification),
                            movieTitle,
                            movieVoteAverage);

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(getContext())
                                    .setColor(resources.getColor(R.color.moviescopio_theme_light))
                                    .setSmallIcon(iconId)
                                    .setLargeIcon(largeIcon)
                                    .setContentTitle(title)
                                    .setContentText(contentText);

                    Intent resultIntent = new Intent(context, MainActivity.class);

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

                    mNotificationManager.notify(MOVIE_NOTIFICATION_ID, mBuilder.build());

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();
                }
            }
        }
    }

    long addMovie(String identifier, String posterPath, String releaseDate, String title, double voteAverage) {
        long movieId;

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
            ContentValues movieValues = new ContentValues();

            movieValues.put(MovieContract.MovieEntry.COLUMN_IDENTIFIER, identifier);
            movieValues.put(MovieContract.MovieEntry.COLUMN_POSTER_PATH, posterPath);
            movieValues.put(MovieContract.MovieEntry.COLUMN_RELEASE_DATE, releaseDate);
            movieValues.put(MovieContract.MovieEntry.COLUMN_TITLE, title);
            movieValues.put(MovieContract.MovieEntry.COLUMN_VOTE_AVERAGE, voteAverage);

            Uri insertedUri = getContext().getContentResolver().insert(
                    MovieContract.MovieEntry.CONTENT_URI, movieValues
            );

            movieId = ContentUris.parseId(insertedUri);
        }

        movieCursor.close();

        return movieId;
    }

    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

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

    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    public static Account getSyncAccount(Context context) {

        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);


        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));


        if ( null == accountManager.getPassword(newAccount) ) {

            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        MoviescopioSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}