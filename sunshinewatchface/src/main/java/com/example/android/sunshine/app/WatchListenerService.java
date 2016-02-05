package com.example.android.sunshine.app;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WatchListenerService extends WearableListenerService {

    GoogleApiClient mGoogleApiClient;
    WatchListenerService mWatchListenerService;

    public WatchListenerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        mWatchListenerService = this;

        mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("WatchListenerService","onConnected");
                        Wearable.MessageApi.addListener(mGoogleApiClient, mWatchListenerService);
                        Wearable.DataApi.addListener(mGoogleApiClient, mWatchListenerService);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {

                    }
                })
                .build();

        //Wearable.DataApi.addListener(mGoogleApiClient, mWatchListenerService);

        mGoogleApiClient.connect();
    }

    public void onResume() {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("WatchListenerService", "onDataChanged");
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                // DataItem changed
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/watchData") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    int low = dataMap.getInt("LowTemp");
                    int high = dataMap.getInt("HighTemp");
                    int weatherId = dataMap.getInt("WeatherId");

                    Intent intent = new Intent("weather_data");
                    intent.putExtra("LowTemp", low);
                    intent.putExtra("HighTemp", high);
                    intent.putExtra("WeatherId", weatherId);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                // DataItem deleted
            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("WatchListenerService", "onMessageReceived");
        /*
        if( messageEvent.getPath().equalsIgnoreCase( START_ACTIVITY ) ) {
            Intent intent = new Intent( this, MainActivity.class );
            intent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            startActivity( intent );
        } else {
            super.onMessageReceived( messageEvent );
        }
        */
    }

    @Override
    public void onPeerConnected(Node peer) {

    }

    public void onDestroy() {
        Log.d("WatchListenerService", "onDestroy");
    }
}
