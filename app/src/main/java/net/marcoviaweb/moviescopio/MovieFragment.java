package net.marcoviaweb.moviescopio;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by mavendano on 16/04/2015.
 */
public class MovieFragment extends Fragment {

    private ArrayAdapter<String> mMoviesAdapter;

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
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mMoviesAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_movies,
                R.id.list_item_movies_textview,
                new ArrayList<String>()
        );

        ListView listView = (ListView) rootView.findViewById(R.id.listview_movies);
        listView.setAdapter(mMoviesAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String movie = mMoviesAdapter.getItem(position);
                Intent intent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, movie);
                startActivity(intent);
            }
        });

        return rootView;
    }

    private void updateMovie() {
        FetchMovieTask movieTask = new FetchMovieTask();


        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String genre = sharedPrefs.getString(
                getString(R.string.pref_genre_key),
                getString(R.string.pref_genre_scifi));

        movieTask.execute(genre);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateMovie();
    }

    public class FetchMovieTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchMovieTask.class.getSimpleName();

        private String[] getWeatherDataFromJson(String forecastJsonStr) throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "results";
            final String OWM_DATE_RELEASE = "release_date";
            final String OWM_POSTER_PATH = "poster_path";
            final String OWM_TITLE = "title";
            final String OWM_VOTE_AVERAGE = "vote_average";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray movieArray = forecastJson.getJSONArray(OWM_LIST);

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.
            // Cheating to convert this to UTC time, which is what we want anyhow
            // Cheating to convert this to UTC time, which is what we want anyhow
            String[] resultStrs = new String[20];

            for (int i = 0; i < movieArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String dateRelease;
                String posterPath;
                String title;
                String voteAverage;

                // Get the JSON object representing the day
                JSONObject movie = movieArray.getJSONObject(i);
                dateRelease = movie.getString(OWM_DATE_RELEASE);
                posterPath = movie.getString(OWM_POSTER_PATH);
                title = movie.getString(OWM_TITLE);
                voteAverage = movie.getString(OWM_VOTE_AVERAGE);

                resultStrs[i] = dateRelease + " - " + posterPath + " - " + title + " - " + voteAverage;
            }

            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Movie entry: " + s);
            }
            return resultStrs;

        }


        @Override
        protected String[] doInBackground(String... params) {
            //
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String moviesJsonStr = null;

            String apy_key = "54b406237a31ab795b7850ff7ddc2494";
            String sort_by = "popularity.desc";
            int page = 1;

            try {
                final String FORECAST_BASE_URL = "http://api.themoviedb.org/3/discover/movie?";
                final String QUERY_API_KEY = "api_key";
                final String QUERY_ORDEN = "sort_by";
                final String QUERY_PAGINA = "page";
                final String QUERY_GENERO = "with_genres";

                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(QUERY_API_KEY, apy_key)
                        .appendQueryParameter(QUERY_ORDEN, sort_by)
                        .appendQueryParameter(QUERY_PAGINA, Integer.toString(page))
                        .appendQueryParameter(QUERY_GENERO, params[0])
                        .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Build URI" + builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    return null;
                }
                moviesJsonStr = buffer.toString();

                Log.v(LOG_TAG, "Movie JSON String: " + moviesJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
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
                return getWeatherDataFromJson(moviesJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            //
            return null;
        }

        @Override
        protected void onPostExecute(String[] result) {
            if (result != null) {
                mMoviesAdapter.clear();
                for (String movieStr : result) {
                    mMoviesAdapter.add(movieStr);
                }
            }
        }
    }
}
