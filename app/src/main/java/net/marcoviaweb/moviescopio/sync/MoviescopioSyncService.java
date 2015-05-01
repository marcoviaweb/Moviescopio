package net.marcoviaweb.moviescopio.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MoviescopioSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static MoviescopioSyncAdapter sMoviescopioSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService");
        synchronized (sSyncAdapterLock) {
            if (sMoviescopioSyncAdapter == null) {
                sMoviescopioSyncAdapter = new MoviescopioSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sMoviescopioSyncAdapter.getSyncAdapterBinder();
    }
}