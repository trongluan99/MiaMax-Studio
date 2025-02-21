package com.ads.mia.ads;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAttribution;
import com.adjust.sdk.AdjustConfig;
import com.adjust.sdk.AdjustEventFailure;
import com.adjust.sdk.AdjustEventSuccess;
import com.adjust.sdk.AdjustSessionFailure;
import com.adjust.sdk.AdjustSessionSuccess;
import com.adjust.sdk.LogLevel;
import com.adjust.sdk.OnAttributionChangedListener;
import com.adjust.sdk.OnEventTrackingFailedListener;
import com.adjust.sdk.OnEventTrackingSucceededListener;
import com.adjust.sdk.OnSessionTrackingFailedListener;
import com.adjust.sdk.OnSessionTrackingSucceededListener;
import com.ads.mia.ads.wrapper.ApInterstitialAd;
import com.ads.mia.ads.wrapper.ApNativeAd;
import com.ads.mia.config.MiaAdConfig;
import com.ads.mia.event.MiaAdjust;
import com.ads.mia.max.AppOpenMax;
import com.ads.mia.max.MaxAd;
import com.ads.mia.max.MaxAdCallback;
import com.ads.mia.util.AppUtil;
import com.ads.mia.util.SharePreferenceUtils;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.facebook.FacebookSdk;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.nativead.NativeAdView;

public class MiaAd {
    public static final String TAG_ADJUST = "MiaAdjust";
    public static final String TAG = "MiaAd";
    private static volatile MiaAd INSTANCE;
    private MiaAdConfig adConfig;
    private MiaInitCallback initCallback;
    private Boolean initAdSuccess = false;

    public static synchronized MiaAd getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MiaAd();
        }
        return INSTANCE;
    }

    public MiaAdConfig getAdConfig() {
        return adConfig;
    }

    public void setCountClickToShowAds(int countClickToShowAds) {
        MaxAd.getInstance().setNumShowAds(countClickToShowAds);
    }

    public void setCountClickToShowAds(int countClickToShowAds, int currentClicked) {
        MaxAd.getInstance().setNumToShowAds(countClickToShowAds, currentClicked);
    }

    public void init(Application context, MiaAdConfig adConfig, Boolean enableDebugMediation) {
        if (adConfig == null) {
            throw new RuntimeException("Cant not set GamAdConfig null");
        }
        this.adConfig = adConfig;
        AppUtil.VARIANT_DEV = adConfig.isVariantDev();
        if (adConfig.isEnableAdjust()) {
            MiaAdjust.enableAdjust = true;
            setupAdjust(adConfig.isVariantDev(), adConfig.getAdjustConfig().getAdjustToken());
        }

        MaxAd.getInstance().init(context, new MaxAdCallback() {
            @Override
            public void initMaxSuccess() {
                super.initMaxSuccess();
                initAdSuccess = true;
                if (initCallback != null) {
                    initCallback.initAdSuccess();
                }
            }
        }, enableDebugMediation, adConfig.getAdjustTokenTiktok());

        if (adConfig.isEnableAdResume()) {
            AppOpenMax.getInstance().init(adConfig.getApplication(), adConfig.getIdAdResume());
        }
        FacebookSdk.setClientToken(adConfig.getFacebookClientToken());
        FacebookSdk.sdkInitialize(context);
    }

    public void setInitCallback(MiaInitCallback initCallback) {
        this.initCallback = initCallback;
        if (initAdSuccess)
            initCallback.initAdSuccess();
    }

    private void setupAdjust(Boolean buildDebug, String adjustToken) {
        String environment = buildDebug ? AdjustConfig.ENVIRONMENT_SANDBOX : AdjustConfig.ENVIRONMENT_PRODUCTION;
        AdjustConfig config = new AdjustConfig(adConfig.getApplication(), adjustToken, environment);

        // Change the log level.
        config.setLogLevel(LogLevel.VERBOSE);
        config.setPreinstallTrackingEnabled(true);
        config.setOnAttributionChangedListener(new OnAttributionChangedListener() {
            @Override
            public void onAttributionChanged(AdjustAttribution attribution) {
                Log.d(TAG_ADJUST, "Attribution callback called!");
                Log.d(TAG_ADJUST, "Attribution: " + attribution.toString());
            }
        });

        // Set event success tracking delegate.
        config.setOnEventTrackingSucceededListener(new OnEventTrackingSucceededListener() {
            @Override
            public void onFinishedEventTrackingSucceeded(AdjustEventSuccess eventSuccessResponseData) {
                Log.d(TAG_ADJUST, "Event success callback called!");
                Log.d(TAG_ADJUST, "Event success data: " + eventSuccessResponseData.toString());
            }
        });
        // Set event failure tracking delegate.
        config.setOnEventTrackingFailedListener(new OnEventTrackingFailedListener() {
            @Override
            public void onFinishedEventTrackingFailed(AdjustEventFailure eventFailureResponseData) {
                Log.d(TAG_ADJUST, "Event failure callback called!");
                Log.d(TAG_ADJUST, "Event failure data: " + eventFailureResponseData.toString());
            }
        });

        // Set session success tracking delegate.
        config.setOnSessionTrackingSucceededListener(new OnSessionTrackingSucceededListener() {
            @Override
            public void onFinishedSessionTrackingSucceeded(AdjustSessionSuccess sessionSuccessResponseData) {
                Log.d(TAG_ADJUST, "Session success callback called!");
                Log.d(TAG_ADJUST, "Session success data: " + sessionSuccessResponseData.toString());
            }
        });

        // Set session failure tracking delegate.
        config.setOnSessionTrackingFailedListener(new OnSessionTrackingFailedListener() {
            @Override
            public void onFinishedSessionTrackingFailed(AdjustSessionFailure sessionFailureResponseData) {
                Log.d(TAG_ADJUST, "Session failure callback called!");
                Log.d(TAG_ADJUST, "Session failure data: " + sessionFailureResponseData.toString());
            }
        });


        config.setSendInBackground(true);
        Adjust.onCreate(config);
        adConfig.getApplication().registerActivityLifecycleCallbacks(new AdjustLifecycleCallbacks());
    }

    private static final class AdjustLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityResumed(Activity activity) {
            Adjust.onResume();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            Adjust.onPause();
        }

        @Override
        public void onActivityStopped(Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
        }
    }

    public void loadBanner(Activity mActivity, String id) {
        MaxAd.getInstance().loadBanner(mActivity, id);
    }

    public void loadBanner(Activity mActivity, String id, MaxAdCallback adCallback) {
        MaxAd.getInstance().loadBanner(mActivity, id, adCallback);
    }

    public void loadBannerFragment(Activity mActivity, String id, View rootView) {
        MaxAd.getInstance().loadBannerFragment(mActivity, id, rootView);
    }

    public void loadBannerFragment(Activity mActivity, String id, View rootView, MaxAdCallback adCallback) {
        MaxAd.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback);
    }

    public void loadSplashInterstitialAds(Context context, String id, long timeOut, long timeDelay, MaxAdCallback adListener) {
        MaxAd.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, true, adListener);
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, MaxAdCallback callback, int timeDelay) {
        MaxAd.getInstance().onCheckShowSplashWhenFail(activity, callback, timeDelay);
    }

    public ApInterstitialAd getInterstitialAds(Context context, String id, MaxAdCallback adListener) {
        ApInterstitialAd apInterstitialAd = new ApInterstitialAd();
        MaxInterstitialAd maxInterstitialAd = MaxAd.getInstance().getInterstitialAds(context, id);
        maxInterstitialAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(@NonNull com.applovin.mediation.MaxAd maxAd) {
                apInterstitialAd.setMaxInterstitialAd(maxInterstitialAd);
                adListener.onInterstitialLoad(apInterstitialAd.getMaxInterstitialAd());
            }

            @Override
            public void onAdDisplayed(@NonNull com.applovin.mediation.MaxAd maxAd) {

            }

            @Override
            public void onAdHidden(@NonNull com.applovin.mediation.MaxAd maxAd) {
                adListener.onAdClosed();
            }

            @Override
            public void onAdClicked(@NonNull com.applovin.mediation.MaxAd maxAd) {
                adListener.onAdClicked();
            }

            @Override
            public void onAdLoadFailed(@NonNull String s, @NonNull MaxError maxError) {
                adListener.onAdFailedToLoad(maxError);
            }

            @Override
            public void onAdDisplayFailed(@NonNull com.applovin.mediation.MaxAd maxAd, @NonNull MaxError maxError) {
                adListener.onAdFailedToShow(maxError);
            }
        });
        apInterstitialAd.setMaxInterstitialAd(maxInterstitialAd);
        return apInterstitialAd;
    }

    public void forceShowInterstitial(@NonNull Context context, ApInterstitialAd mInterstitialAd,
                                      @NonNull final MaxAdCallback callback, boolean shouldReloadAds) {
        if (System.currentTimeMillis() - SharePreferenceUtils.getLastImpressionInterstitialTime(context)
                < MiaAd.getInstance().adConfig.getIntervalInterstitialAd() * 1000L
        ) {
            callback.onNextAction();
            return;
        }
        if (mInterstitialAd == null || mInterstitialAd.isNotReady()) {
            callback.onNextAction();
            return;
        }
        MaxAd.getInstance().showInterstitialAdByTimes(context, mInterstitialAd.getMaxInterstitialAd(), new MaxAdCallback() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                callback.onAdClosed();
                callback.onNextAction();
                if (shouldReloadAds)
                    mInterstitialAd.getMaxInterstitialAd().loadAd();

            }

            @Override
            public void onInterstitialLoad(@Nullable MaxInterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.d(TAG, "Max inter onAdLoaded:");
            }

            @Override
            public void onAdFailedToShow(@Nullable MaxError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
                if (shouldReloadAds)
                    mInterstitialAd.getMaxInterstitialAd().loadAd();
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }

            @Override
            public void onInterstitialShow() {
                super.onInterstitialShow();
                callback.onInterstitialShow();
            }
        }, false);
    }

    public void loadNativeAdResultCallback(final Activity activity, String id,
                                           int layoutCustomNative, MaxAdCallback callback) {
        MaxAd.getInstance().loadNativeAd(activity, id, layoutCustomNative, new MaxAdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                callback.onNativeAdLoaded(new ApNativeAd(layoutCustomNative, unifiedNativeAd));
            }

            @Override
            public void onAdFailedToLoad(@Nullable MaxError i) {
                super.onAdFailedToLoad(i);
                callback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }
        });
    }

    public void loadNativeAd(final Activity activity, String id,
                             int layoutCustomNative, FrameLayout adPlaceHolder, ShimmerFrameLayout
                                     containerShimmerLoading, MaxAdCallback callback) {
        MaxAd.getInstance().loadNativeAd(activity, id, layoutCustomNative, new MaxAdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                populateNativeAdView(activity, new ApNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
            }

            @Override
            public void onAdFailedToLoad(@Nullable MaxError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "onAdFailedToLoad : NativeAd");
            }
        });
    }

    public void populateNativeAdView(Activity activity, ApNativeAd apNativeAd, FrameLayout adPlaceHolder, ShimmerFrameLayout containerShimmerLoading) {
        if (apNativeAd.getNativeView() == null) {
            containerShimmerLoading.setVisibility(View.GONE);
            return;
        }
        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(apNativeAd.getLayoutCustomNative(), null);
        containerShimmerLoading.stopShimmer();
        containerShimmerLoading.setVisibility(View.GONE);
        adPlaceHolder.setVisibility(View.VISIBLE);
        adPlaceHolder.removeAllViews();
        if (apNativeAd.getNativeView().getParent() != null) {
            ((ViewGroup) apNativeAd.getNativeView().getParent()).removeAllViews();
        }
        adPlaceHolder.addView(apNativeAd.getNativeView());
    }

    public MaxRewardedAd initRewardAds(AppCompatActivity activity, String id, MaxAdCallback callback) {
        return MaxAd.getInstance().getRewardAd(activity, id, callback);
    }

    public void showRewardAds(Activity context, MaxRewardedAd rewardedAd, MaxAdCallback adCallback) {
        MaxAd.getInstance().showRewardAd(context, rewardedAd, adCallback);
    }
}
