package com.ads.mia.ads.wrapper;

import android.view.View;

import com.google.android.gms.ads.nativead.NativeAd;

public class ApNativeAd extends ApAdBase {
    private int layoutCustomNative;
    private View nativeView;

    public ApNativeAd(StatusAd status) {
        super(status);
    }

    public ApNativeAd(int layoutCustomNative, View nativeView) {
        this.layoutCustomNative = layoutCustomNative;
        this.nativeView = nativeView;
        status = StatusAd.AD_LOADED;
    }


    @Override
    boolean isReady() {
        return nativeView != null;
    }


    public int getLayoutCustomNative() {
        return layoutCustomNative;
    }

    public void setLayoutCustomNative(int layoutCustomNative) {
        this.layoutCustomNative = layoutCustomNative;
    }

    public View getNativeView() {
        return nativeView;
    }

    public void setNativeView(View nativeView) {
        this.nativeView = nativeView;
    }

    public String toString() {
        return "Status:" + status + " == nativeView:" + nativeView;
    }

}
