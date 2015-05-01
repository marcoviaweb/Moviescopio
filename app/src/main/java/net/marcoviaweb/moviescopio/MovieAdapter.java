package net.marcoviaweb.moviescopio;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class MovieAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 2;
    private final int VIEW_TYPE_MOVIE_PRINCIPAL = 0;
    private final int VIEW_TYPE_MOVIE_LIST = 1;

    private final String BASE_POSTER_PATH = "http://image.tmdb.org/t/p/w92";

    private boolean mUsePrincipalLayout = true;

    public static class ViewHolder {
        public final TextView titleView;
        public final TextView dateReleaseView;
        public final ImageView posterView;
        public final ImageView voteAverageIconView;

        public ViewHolder(View view) {
            titleView = (TextView) view.findViewById(R.id.list_item_title_textview);
            dateReleaseView = (TextView) view.findViewById(R.id.list_item_daterelease_textview);
            posterView = (ImageView) view.findViewById(R.id.list_item_poster_imageview);
            voteAverageIconView = (ImageView) view.findViewById(R.id.list_voteaverage_icon);
        }
    }

    public MovieAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_MOVIE_PRINCIPAL: {
                layoutId = R.layout.list_item_movies_principal;
                break;
            }
            case VIEW_TYPE_MOVIE_LIST: {
                layoutId = R.layout.list_item_movies;
                break;
            }
        }

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType) {
            case VIEW_TYPE_MOVIE_PRINCIPAL: {

                String dateRelease = cursor.getString(MovieFragment.COL_RELEASE_DATE);
                viewHolder.dateReleaseView.setText(dateRelease);

                String posterPath = BASE_POSTER_PATH + cursor.getString(MovieFragment.COL_MOVIE_POSTER_PATH);
                Picasso.with(context)
                        .load(BASE_POSTER_PATH + posterPath)
                        .into(viewHolder.posterView);

                break;
            }
            case VIEW_TYPE_MOVIE_LIST: {
                // Por ahora no tiene imagen principal
                break;
            }
        }

        String movieId = cursor.getString(MovieFragment.COL_MOVIE_IDENTIFIER);

        String title = cursor.getString(MovieFragment.COL_TITLE);
        viewHolder.titleView.setText(title);

        float voteAverage = cursor.getFloat(MovieFragment.COL_VOTE_AVERAGE);
        viewHolder.voteAverageIconView.setImageResource(Utility.getArtResourceForAverageMovie(voteAverage));

    }

    public void setUsePrincipalLayout(boolean usePrincipalLayout) {
        mUsePrincipalLayout = usePrincipalLayout;
    }


    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUsePrincipalLayout) ? VIEW_TYPE_MOVIE_PRINCIPAL : VIEW_TYPE_MOVIE_LIST;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }






}