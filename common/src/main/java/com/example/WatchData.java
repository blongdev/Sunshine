package com.example;

/**
 * Created by Brian on 2/5/2016.
 */
public class WatchData {

    private int mLowTemp;
    private int mHighTemp;
    private int mWeatherId;

    public WatchData(int low, int high, int weather) {
        mLowTemp = low;
        mHighTemp = high;
        mWeatherId = weather;
    }


    public void setLowTemp(int temp) {
        mLowTemp = temp;
    }

    public void setHighTemp(int temp) {
        mHighTemp = temp;
    }

    public void setWeatherId(int id) {
        mWeatherId = id;
    }

    public int getLowTemp() {
        return mLowTemp;
    }

    public int getHighTemp() {
        return mHighTemp;
    }

    public int getWeatherId() {
        return mWeatherId;
    }

}
