package net.marcoviaweb.moviescopio;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends ActionBarActivity implements MovieFragment.Callback {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String MOVIEFRAGMENT_TAG = "MFTAG";

    private boolean mTwoPane;
    private String mGenre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGenre = Utility.getPreferredGenre(this);

        setContentView(R.layout.activity_main);
        if (findViewById(R.id.movie_detail_container) != null) {
            mTwoPane = true;
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.movie_detail_container, new DetailFragment(), MOVIEFRAGMENT_TAG)
                        .commit();
            }
        } else {
            mTwoPane = false;
        }

        MovieFragment movieFragment =  ((MovieFragment)getSupportFragmentManager()
                .findFragmentById(R.id.fragment_movie_list));
        movieFragment.setUseTodayLayout(!mTwoPane);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String genre = Utility.getPreferredGenre(this);

        if (genre != null && !genre.equals(mGenre)) {
            MovieFragment ff = (MovieFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_movie_list);
            if ( null != ff ) {
                ff.onGenreChanged();
            }
            DetailFragment df = (DetailFragment)getSupportFragmentManager().findFragmentByTag(MOVIEFRAGMENT_TAG);
            if (null != df) {
                df.onGenreChanged(genre);
            }
            mGenre = genre;
        }
    }

    @Override
    public void onItemSelected(Uri contentUri) {
        if (mTwoPane) {
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, contentUri);

            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.movie_detail_container, fragment, MOVIEFRAGMENT_TAG)
                    .commit();
        } else {
            Intent intent = new Intent(this, DetailActivity.class)
                    .setData(contentUri);
            startActivity(intent);
        }
    }
}
