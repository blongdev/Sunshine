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
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.List;

public class WatchListenerService extends WearableListenerService {

    GoogleApiClient mGoogleApiClient;
    WatchListenerService mWatchListenerService;

    public WatchListenerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        //android.os.Debug.waitForDebugger();

        mWatchListenerService = this;

        mGoogleApiClient = new GoogleApiClient.Builder(getBaseContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d("WatchListenerService", "onConnected");
                        Wearable.DataApi.addListener(mGoogleApiClient, mWatchListenerService);
                        //sendDataRequests();
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


        mGoogleApiClient.connect();
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
                    long low = dataMap.getLong("LowTemp");
                    long high = dataMap.getLong("HighTemp");
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
    }

    @Override
    public void onPeerConnected(Node peer) {
        requestData(peer);
    }

    public void requestData(final Node node) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), "dataRequest", null).await();
            }
        }).start();
    }

    public void sendDataRequests() {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult result =
                        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                List<Node> nodes = result.getNodes();
                for(Node node : nodes) {
                    MessageApi.SendMessageResult messageResult = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), "dataRequest", null).await();
                }
            }
        }).start();
    }

    public void onDestroy() {
        Log.d("WatchListenerService", "onDestroy");
    }
}
