package com.example.android.sunshine.app.sync;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;


/**
 * Created by Brian on 2/3/2016.
 */
public class WatchSync implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, MessageApi.MessageListener {

    private GoogleApiClient mGoogleApiClient;
    private long mLowTemp;
    private long mHighTemp;
    private int mWeatherId;


    public WatchSync(Context context) {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void setValues(long low, long high, int weather) {
        mLowTemp = low;
        mHighTemp = high;
        mWeatherId = weather;
    }

    public void sendWatchData(long low, long high, int weather) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/watchData").setUrgent();
        putDataMapReq.getDataMap().putLong("LowTemp", low);
        putDataMapReq.getDataMap().putLong("HighTemp", high);
        putDataMapReq.getDataMap().putInt("WeatherId", weather);
        putDataMapReq.getDataMap().putLong("Time", System.currentTimeMillis());
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void onConnected(Bundle bundle) {
        Log.d("WatchSync", "OnConnected()");
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        sendWatchData(mLowTemp, mHighTemp, mWeatherId);
    }


    public void onConnectionSuspended(int result) {
        Log.e("WatchSync", "OnConnectionSuspended()");
    }
    public void onConnectionFailed(ConnectionResult result) {
        Log.e("WatchSync", "OnConnectionFailed()");
    }

    public void disconnect() {
        mGoogleApiClient.disconnect();
    }

    public void sendMessage( final String path, final String text ) {
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes( mGoogleApiClient ).await();
                for(Node node : nodes.getNodes()) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, node.getId(), path, text.getBytes() ).await();
                }
            }
        }).start();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d("WatchSync", "onMessageReceived");
        if( messageEvent.getPath().equalsIgnoreCase("dataRequest")) {
            sendWatchData(mLowTemp, mHighTemp, mWeatherId);
        }
    }
}
