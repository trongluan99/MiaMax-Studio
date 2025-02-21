package com.ads.mia.ads.wrapper;

import com.applovin.mediation.ads.MaxInterstitialAd;

public class ApInterstitialAd extends ApAdBase {
    private MaxInterstitialAd maxInterstitialAd;

    public ApInterstitialAd() {
    }

    public void setMaxInterstitialAd(MaxInterstitialAd maxInterstitialAd) {
        this.maxInterstitialAd = maxInterstitialAd;
        status = StatusAd.AD_LOADED;
    }

    @Override
    public boolean isReady() {
        return maxInterstitialAd != null && maxInterstitialAd.isReady();
    }

    public MaxInterstitialAd getMaxInterstitialAd() {
        return maxInterstitialAd;
    }
}
