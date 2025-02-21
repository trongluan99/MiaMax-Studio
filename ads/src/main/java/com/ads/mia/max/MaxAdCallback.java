package com.ads.mia.max;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ads.mia.ads.wrapper.ApNativeAd;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;

public class MaxAdCallback {
    public void initMaxSuccess() {
    }

    public void onAdClosed() {
    }

    public void onAdFailedToLoad(@Nullable MaxError i) {
    }

    public void onAdFailedToShow(@Nullable MaxError adError) {
    }

    public void onAdLoaded() {
    }

    public void onInterstitialLoad(MaxInterstitialAd interstitialAd) {
    }

    public void onAdClicked() {
    }

    public void onUserRewarded(MaxReward reward) {
    }

    public void onAdImpression() {
    }

    public void onAdSplashReady() {
    }

    public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
    }

    public void onNextAction() {
    }

    public void onAdDisplayed() {
    }

    public void onAdLoadFailed() {
    }

    public void onAdDisplayFailed() {
    }

    public void onInterstitialShow() {

    }

    public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {

    }
}
