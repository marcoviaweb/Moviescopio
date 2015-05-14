package net.marcoviaweb.moviescopio.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MoviescopioAuthenticatorService extends Service {

    private MoviescopioAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new MoviescopioAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
