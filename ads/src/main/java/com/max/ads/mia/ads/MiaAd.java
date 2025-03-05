package com.max.ads.mia.ads;

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
import com.max.ads.mia.admob.Admob;
import com.max.ads.mia.admob.AdmobAdCallback;
import com.max.ads.mia.admob.AppOpenManager;
import com.max.ads.mia.ads.wrapper.ApInterstitialAd;
import com.max.ads.mia.ads.wrapper.ApInterstitialPriorityAd;
import com.max.ads.mia.ads.wrapper.ApNativeAd;
import com.max.ads.mia.config.MiaAdConfig;
import com.max.ads.mia.event.MiaAdjust;
import com.max.ads.mia.max.AppOpenMax;
import com.max.ads.mia.max.MaxAds;
import com.max.ads.mia.max.MaxAdCallback;
import com.max.ads.mia.util.AppUtil;
import com.max.ads.mia.util.SharePreferenceUtils;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.facebook.FacebookSdk;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;

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
        MaxAds.getInstance().setNumShowAds(countClickToShowAds);
    }

    public void setCountClickToShowAds(int countClickToShowAds, int currentClicked) {
        MaxAds.getInstance().setNumToShowAds(countClickToShowAds, currentClicked);
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

        initAdmob(context, adConfig);
        initMax(context, adConfig, enableDebugMediation);

        if (adConfig.isEnableAdResumeMax()) {
            AppOpenMax.getInstance().init(adConfig.getApplication(), adConfig.getIdAdResumeMax());
        }
        FacebookSdk.setClientToken(adConfig.getFacebookClientToken());
        FacebookSdk.sdkInitialize(context);
    }

    private void initAdmob(Application context, MiaAdConfig adConfig) {
        Admob.getInstance().init(context, adConfig.getListDeviceTest(), adConfig.getAdjustTokenTiktok());
        if (adConfig.isEnableAdResumeAdmob()) {
            AppOpenManager.getInstance().init(adConfig.getApplication(), adConfig.getIdAdResumeAdmob());
        }
    }

    private void initMax(Application context, MiaAdConfig adConfig, Boolean enableDebugMediation) {
        MaxAds.getInstance().init(context, new MaxAdCallback() {
            @Override
            public void initMaxSuccess() {
                super.initMaxSuccess();
                initAdSuccess = true;
                if (initCallback != null) {
                    initCallback.initAdSuccess();
                }
            }
        }, enableDebugMediation, adConfig.getAdjustTokenTiktok());

        if (adConfig.isEnableAdResumeMax()) {
            AppOpenMax.getInstance().init(adConfig.getApplication(), adConfig.getIdAdResumeMax());
        }
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

    /**
     * Max Ad
     */

    public void loadBannerMax(Activity mActivity, String id) {
        MaxAds.getInstance().loadBanner(mActivity, id);
    }

    public void loadBannerMax(Activity mActivity, String id, MaxAdCallback adCallback) {
        MaxAds.getInstance().loadBanner(mActivity, id, adCallback);
    }

    public void loadBannerFragmentMax(Activity mActivity, String id, View rootView) {
        MaxAds.getInstance().loadBannerFragment(mActivity, id, rootView);
    }

    public void loadBannerFragmentMax(Activity mActivity, String id, View rootView, MaxAdCallback adCallback) {
        MaxAds.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback);
    }

    public void loadSplashInterstitialAdsMax(AppCompatActivity context, String id, long timeOut, long timeDelay, MaxAdCallback adListener) {
        MaxAds.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, true, adListener);
    }

    public void onCheckShowSplashWhenFailMax(AppCompatActivity activity, MaxAdCallback callback, int timeDelay) {
        MaxAds.getInstance().onCheckShowSplashWhenFail(activity, callback, timeDelay);
    }

    public ApInterstitialAd getInterstitialAdsMax(Context context, String id, MaxAdCallback adListener) {
        ApInterstitialAd apInterstitialAd = new ApInterstitialAd();
        MaxInterstitialAd maxInterstitialAd = MaxAds.getInstance().getInterstitialAds(context, id);
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

    public void forceShowInterstitialMax(@NonNull AppCompatActivity context, ApInterstitialAd mInterstitialAd,
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
        MaxAds.getInstance().showInterstitialAdByTimes(context, mInterstitialAd.getMaxInterstitialAd(), new MaxAdCallback() {
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

    public void loadNativeAdResultCallbackMax(final Activity activity, String id,
                                              int layoutCustomNative, MaxAdCallback callback) {
        MaxAds.getInstance().loadNativeAd(activity, id, layoutCustomNative, new MaxAdCallback() {
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

    public void loadNativeAdMax(final Activity activity, String id,
                                int layoutCustomNative, FrameLayout adPlaceHolder, ShimmerFrameLayout
                                        containerShimmerLoading, MaxAdCallback callback) {
        MaxAds.getInstance().loadNativeAd(activity, id, layoutCustomNative, new MaxAdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(MaxNativeAdView unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                populateNativeAdViewMax(activity, new ApNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
            }

            @Override
            public void onAdFailedToLoad(@Nullable MaxError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "onAdFailedToLoad : NativeAd");
            }
        });
    }

    public void populateNativeAdViewMax(Activity activity, ApNativeAd apNativeAd, FrameLayout adPlaceHolder, ShimmerFrameLayout containerShimmerLoading) {
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

    public MaxRewardedAd initRewardAdsMax(AppCompatActivity activity, String id, MaxAdCallback callback) {
        return MaxAds.getInstance().getRewardAd(activity, id, callback);
    }

    public void showRewardAdsMax(Activity context, MaxRewardedAd rewardedAd, MaxAdCallback adCallback) {
        MaxAds.getInstance().showRewardAd(context, rewardedAd, adCallback);
    }


    /**
     * Admob Ad
     */

    public void loadBannerAdmob(Activity mActivity, String id, AdmobAdCallback adCallback) {
        Admob.getInstance().loadBanner(mActivity, id, adCallback);
    }

    public void loadCollapsibleBannerAdmob(Activity activity, String id, String gravity, AdmobAdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBanner(activity, id, gravity, adCallback);
    }

    public void loadCollapsibleBannerFragmentAdmob(Activity activity, String id, View rootView, String gravity, AdmobAdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBannerFragment(activity, id, rootView, gravity, adCallback);
    }

    public void loadCollapsibleBannerSizeMediumAdmob(Activity activity, String id, String gravity, AdSize sizeBanner, AdmobAdCallback adCallback) {
        Admob.getInstance().loadCollapsibleBannerSizeMedium(activity, id, gravity, sizeBanner, adCallback);
    }

    public void loadBannerFragmentAdmob(Activity mActivity, String id, View rootView) {
        Admob.getInstance().loadBannerFragment(mActivity, id, rootView);
    }

    public void loadBannerFragmentAdmob(Activity mActivity, String id, View rootView, AdmobAdCallback adCallback) {
        Admob.getInstance().loadBannerFragment(mActivity, id, rootView, adCallback);
    }

    public void loadInlineBannerAdmob(Activity mActivity, String idBanner, String inlineStyle) {
        Admob.getInstance().loadInlineBanner(mActivity, idBanner, inlineStyle);
    }

    public void loadInlineBannerAdmob(Activity mActivity, String idBanner, String inlineStyle, AdmobAdCallback adCallback) {
        Admob.getInstance().loadInlineBanner(mActivity, idBanner, inlineStyle, adCallback);
    }

    public void loadBannerInlineFragmentAdmob(Activity mActivity, String idBanner, View rootView, String inlineStyle) {
        Admob.getInstance().loadInlineBannerFragment(mActivity, idBanner, rootView, inlineStyle);
    }

    public void loadBannerInlineFragmentAdmob(Activity mActivity, String idBanner, View rootView, String inlineStyle, AdmobAdCallback adCallback) {
        Admob.getInstance().loadInlineBannerFragment(mActivity, idBanner, rootView, inlineStyle, adCallback);
    }

    public void loadSplashInterstitialAdsAdmob(Context context, String id, long timeOut, long timeDelay, AdmobAdCallback adListener) {
        Admob.getInstance().loadSplashInterstitialAds(context, id, timeOut, timeDelay, true, adListener);
    }

    public void onCheckShowSplashWhenFailAdmob(AppCompatActivity activity, AdmobAdCallback callback, int timeDelay) {
        Admob.getInstance().onCheckShowSplashWhenFail(activity, callback, timeDelay);
    }

    public ApInterstitialAd getInterstitialAdsAdmob(Context context, String id, AdmobAdCallback adListener) {
        ApInterstitialAd apInterstitialAd = new ApInterstitialAd();
        Admob.getInstance().getInterstitialAds(context, id, new AdmobAdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                apInterstitialAd.setInterstitialAd(interstitialAd);
                adListener.onApInterstitialLoad(apInterstitialAd);
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.d(TAG, "Admob onAdFailedToLoad");
                adListener.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                Log.d(TAG, "Admob onAdFailedToShow");
                adListener.onAdFailedToShow(adError);
            }

        });
        return apInterstitialAd;
    }

    public void forceShowInterstitialAdmob(@NonNull Context context, ApInterstitialAd mInterstitialAd,
                                           @NonNull final AdmobAdCallback callback, boolean shouldReloadAds) {
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
        AdmobAdCallback adCallback = new AdmobAdCallback() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                callback.onAdClosed();
                if (shouldReloadAds) {
                    Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdmobAdCallback() {
                        @Override
                        public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                            super.onInterstitialLoad(interstitialAd);
                            mInterstitialAd.setInterstitialAd(interstitialAd);
                            callback.onInterstitialLoad(mInterstitialAd.getInterstitialAd());
                        }

                        @Override
                        public void onAdFailedToLoad(@Nullable LoadAdError i) {
                            super.onAdFailedToLoad(i);
                            mInterstitialAd.setInterstitialAd(null);
                            callback.onAdFailedToLoad(i);
                        }

                        @Override
                        public void onAdFailedToShow(@Nullable AdError adError) {
                            super.onAdFailedToShow(adError);
                            callback.onAdFailedToShow(adError);
                        }

                    });
                } else {
                    mInterstitialAd.setInterstitialAd(null);
                }
            }

            @Override
            public void onNextAction() {
                super.onNextAction();
                callback.onNextAction();
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
                if (shouldReloadAds) {
                    Admob.getInstance().getInterstitialAds(context, mInterstitialAd.getInterstitialAd().getAdUnitId(), new AdmobAdCallback() {
                        @Override
                        public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                            super.onInterstitialLoad(interstitialAd);
                            mInterstitialAd.setInterstitialAd(interstitialAd);
                            callback.onInterstitialLoad(mInterstitialAd.getInterstitialAd());
                        }

                        @Override
                        public void onAdFailedToLoad(@Nullable LoadAdError i) {
                            super.onAdFailedToLoad(i);
                            callback.onAdFailedToLoad(i);
                        }

                        @Override
                        public void onAdFailedToShow(@Nullable AdError adError) {
                            super.onAdFailedToShow(adError);
                            callback.onAdFailedToShow(adError);
                        }

                    });
                } else {
                    mInterstitialAd.setInterstitialAd(null);
                }
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
        };
        Admob.getInstance().forceShowInterstitial(context, mInterstitialAd.getInterstitialAd(), adCallback);
    }

    public void loadNativeAdResultCallbackAdmob(final Activity activity, String id,
                                                int layoutCustomNative, AdmobAdCallback callback) {
        Admob.getInstance().loadNativeAd(((Context) activity), id, new AdmobAdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                callback.onNativeAdLoaded(new ApNativeAd(layoutCustomNative, unifiedNativeAd));
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                callback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }
        });
    }

    public void loadNativeAdAdmob(final Activity activity, String id,
                                  int layoutCustomNative, FrameLayout adPlaceHolder, ShimmerFrameLayout
                                          containerShimmerLoading, AdmobAdCallback callback) {
        Admob.getInstance().loadNativeAd(((Context) activity), id, new AdmobAdCallback() {
            @Override
            public void onUnifiedNativeAdLoaded(@NonNull NativeAd unifiedNativeAd) {
                super.onUnifiedNativeAdLoaded(unifiedNativeAd);
                callback.onNativeAdLoaded(new ApNativeAd(layoutCustomNative, unifiedNativeAd));
                populateNativeAdViewAdmob(activity, new ApNativeAd(layoutCustomNative, unifiedNativeAd), adPlaceHolder, containerShimmerLoading);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                callback.onAdImpression();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                callback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdFailedToShow(@Nullable AdError adError) {
                super.onAdFailedToShow(adError);
                callback.onAdFailedToShow(adError);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                callback.onAdClicked();
            }
        });
    }

    public void populateNativeAdViewAdmob(Activity activity, ApNativeAd apNativeAd, FrameLayout adPlaceHolder, ShimmerFrameLayout containerShimmerLoading) {
        if (apNativeAd.getAdmobNativeAd() == null && apNativeAd.getNativeView() == null) {
            containerShimmerLoading.setVisibility(View.GONE);
            return;
        }
        @SuppressLint("InflateParams") NativeAdView adView = (NativeAdView) LayoutInflater.from(activity).inflate(apNativeAd.getLayoutCustomNative(), null);
        containerShimmerLoading.stopShimmer();
        containerShimmerLoading.setVisibility(View.GONE);
        adPlaceHolder.setVisibility(View.VISIBLE);
        Admob.getInstance().populateUnifiedNativeAdView(apNativeAd.getAdmobNativeAd(), adView);
        adPlaceHolder.removeAllViews();
        adPlaceHolder.addView(adView);
    }

    public void initRewardAdsAdmob(Context context, String id) {
        Admob.getInstance().initRewardAds(context, id);
    }

    public void initRewardAdsAdmob(Context context, String id, AdmobAdCallback callback) {
        Admob.getInstance().initRewardAds(context, id, callback);
    }

    public void getRewardInterstitialAdmob(Context context, String id, AdmobAdCallback callback) {
        Admob.getInstance().getRewardInterstitial(context, id, callback);
    }

    public void showRewardInterstitialAdmob(Activity activity, RewardedInterstitialAd rewardedInterstitialAd, AdmobAdCallback adCallback) {
        Admob.getInstance().showRewardInterstitial(activity, rewardedInterstitialAd, adCallback);
    }

    public void showRewardAdsAdmob(Activity context, AdmobAdCallback adCallback) {
        Admob.getInstance().showRewardAds(context, adCallback);
    }

    public void showRewardAdsAdmob(Activity context, RewardedAd rewardedAd, AdmobAdCallback adCallback) {
        Admob.getInstance().showRewardAds(context, rewardedAd, adCallback);
    }

    public void loadInterSplashPriority4SameTimeAdmob(final Context context,
                                                      String idAdsHigh1,
                                                      String idAdsHigh2,
                                                      String idAdsHigh3,
                                                      String idAdsNormal,
                                                      long timeOut,
                                                      long timeDelay,
                                                      AdmobAdCallback adListener) {
        Admob.getInstance().loadInterSplashPriority4SameTime(context, idAdsHigh1, idAdsHigh2, idAdsHigh3, idAdsNormal, timeOut, timeDelay, adListener);
    }

    public void onShowSplashPriority4Admob(AppCompatActivity activity, AdmobAdCallback adListener) {
        Admob.getInstance().onShowSplashPriority4(activity, adListener);
    }

    public void onCheckShowSplashPriority4WhenFailAdmob(AppCompatActivity activity, AdmobAdCallback callback, int timeDelay) {
        Admob.getInstance().onCheckShowSplashPriority4WhenFail(activity, callback, timeDelay);
    }

    private boolean isFinishLoadNativeAdHigh1 = false;
    private boolean isFinishLoadNativeAdHigh2 = false;
    private boolean isFinishLoadNativeAdHigh3 = false;
    private boolean isFinishLoadNativeAdNormal = false;

    private ApNativeAd apNativeAdHigh2;
    private ApNativeAd apNativeAdHigh3;
    private ApNativeAd apNativeAdNormal;

    public void loadNative4SameTimeAdmob(final Activity activity, String idAdHigh1, String idAdHigh2, String idAdHigh3, String idAdNormal, int layoutCustomNative, AdmobAdCallback adCallback) {
        isFinishLoadNativeAdHigh1 = false;
        isFinishLoadNativeAdHigh2 = false;
        isFinishLoadNativeAdHigh3 = false;

        apNativeAdHigh2 = null;
        apNativeAdHigh3 = null;
        apNativeAdNormal = null;

        loadNativeAdResultCallbackAdmob(activity, idAdHigh1, layoutCustomNative, new AdmobAdCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);
                adCallback.onNativeAdLoaded(nativeAd);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isFinishLoadNativeAdHigh2 && apNativeAdHigh2 != null) {
                    adCallback.onNativeAdLoaded(apNativeAdHigh2);
                } else if (isFinishLoadNativeAdHigh3 && apNativeAdHigh3 != null) {
                    adCallback.onNativeAdLoaded(apNativeAdHigh3);
                } else if (isFinishLoadNativeAdNormal && apNativeAdNormal != null) {
                    adCallback.onNativeAdLoaded(apNativeAdNormal);
                } else {
                    // waiting for ads loaded
                    isFinishLoadNativeAdHigh1 = true;
                }
            }
        });

        loadNativeAdResultCallbackAdmob(activity, idAdHigh2, layoutCustomNative, new AdmobAdCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);
                if (isFinishLoadNativeAdHigh1) {
                    adCallback.onNativeAdLoaded(nativeAd);
                } else {
                    isFinishLoadNativeAdHigh2 = true;
                    apNativeAdHigh2 = nativeAd;
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isFinishLoadNativeAdHigh1) {
                    if (isFinishLoadNativeAdHigh3 && apNativeAdHigh3 != null) {
                        adCallback.onNativeAdLoaded(apNativeAdHigh3);
                    } else if (isFinishLoadNativeAdNormal && apNativeAdNormal != null) {
                        adCallback.onNativeAdLoaded(apNativeAdNormal);
                    } else {
                        isFinishLoadNativeAdHigh2 = true;
                    }
                } else {
                    isFinishLoadNativeAdHigh2 = true;
                    apNativeAdHigh2 = null;
                }
            }
        });

        loadNativeAdResultCallbackAdmob(activity, idAdHigh3, layoutCustomNative, new AdmobAdCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2) {
                    adCallback.onNativeAdLoaded(nativeAd);
                } else {
                    isFinishLoadNativeAdHigh3 = true;
                    apNativeAdHigh3 = nativeAd;
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2) {
                    if (isFinishLoadNativeAdNormal && apNativeAdNormal != null) {
                        adCallback.onNativeAdLoaded(apNativeAdNormal);
                    } else {
                        isFinishLoadNativeAdHigh3 = true;
                    }
                } else {
                    isFinishLoadNativeAdHigh3 = true;
                    apNativeAdHigh3 = null;
                }
            }
        });

        loadNativeAdResultCallbackAdmob(activity, idAdNormal, layoutCustomNative, new AdmobAdCallback() {
            @Override
            public void onNativeAdLoaded(@NonNull ApNativeAd nativeAd) {
                super.onNativeAdLoaded(nativeAd);
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2 && isFinishLoadNativeAdHigh3) {
                    adCallback.onNativeAdLoaded(nativeAd);
                } else {
                    isFinishLoadNativeAdNormal = true;
                    apNativeAdNormal = nativeAd;
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                if (isFinishLoadNativeAdHigh1 && isFinishLoadNativeAdHigh2 && isFinishLoadNativeAdHigh3) {
                    adCallback.onNativeAdLoaded(apNativeAdNormal);
                } else {
                    isFinishLoadNativeAdNormal = true;
                    apNativeAdNormal = null;
                }
            }
        });
    }

    public void loadPriorityInterstitialAdsAdmob(Context context, ApInterstitialPriorityAd apInterstitialPriorityAd, AdmobAdCallback adCallback) {
        loadPriorityInterstitialAdsFromAdmob(context, apInterstitialPriorityAd, adCallback);
    }

    public void loadPriorityInterstitialAdsFromAdmob(Context context,
                                                     ApInterstitialPriorityAd apInterstitialPriorityAd,
                                                     AdmobAdCallback adCallback) {
        if (!apInterstitialPriorityAd.getHigh1PriorityId().isEmpty()
                && !apInterstitialPriorityAd.getHigh1PriorityInterstitialAd().isReady()
        ) {
            loadAdsInterHigh1PriorityAdmob(context, apInterstitialPriorityAd, adCallback);
        }

        if (!apInterstitialPriorityAd.getHigh2PriorityId().isEmpty()
                && !apInterstitialPriorityAd.getHigh2PriorityInterstitialAd().isReady()
        ) {
            loadAdsInterHigh2PriorityAdmob(context, apInterstitialPriorityAd, adCallback);
        }

        if (!apInterstitialPriorityAd.getHigh3PriorityId().isEmpty()
                && !apInterstitialPriorityAd.getHigh3PriorityInterstitialAd().isReady()
        ) {
            loadAdsInterHigh3PriorityAdmob(context, apInterstitialPriorityAd, adCallback);
        }

        if (!apInterstitialPriorityAd.getNormalPriorityId().isEmpty()
                && !apInterstitialPriorityAd.getNormalPriorityInterstitialAd().isReady()
        ) {
            loadInterNormalPriorityAdmob(context, apInterstitialPriorityAd, adCallback);
        }
    }

    private void loadAdsInterHigh1PriorityAdmob(Context context, ApInterstitialPriorityAd apInterstitialPriorityAd, AdmobAdCallback adCallback) {
        Admob.getInstance().getInterstitialAds(context, apInterstitialPriorityAd.getHigh1PriorityId(), new AdmobAdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.d(TAG, "onInterstitialLoad idAdsNormalPriority");
                apInterstitialPriorityAd.getHigh1PriorityInterstitialAd().setInterstitialAd(interstitialAd);
                adCallback.onApInterstitialLoad(apInterstitialPriorityAd.getHigh1PriorityInterstitialAd());
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "onAdFailedToLoad: idAdsNormalPriority: " + i);
                adCallback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                adCallback.onAdImpression();
            }
        });
    }

    private void loadAdsInterHigh2PriorityAdmob(Context context, ApInterstitialPriorityAd apInterstitialPriorityAd, AdmobAdCallback adCallback) {
        Admob.getInstance().getInterstitialAds(context, apInterstitialPriorityAd.getHigh2PriorityId(), new AdmobAdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.d(TAG, "onInterstitialLoad idAdsNormalPriority");
                apInterstitialPriorityAd.getHigh2PriorityInterstitialAd().setInterstitialAd(interstitialAd);
                adCallback.onApInterstitialLoad(apInterstitialPriorityAd.getHigh2PriorityInterstitialAd());
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "onAdFailedToLoad: idAdsNormalPriority: " + i);
                adCallback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                adCallback.onAdImpression();
            }
        });
    }

    private void loadAdsInterHigh3PriorityAdmob(Context context, ApInterstitialPriorityAd apInterstitialPriorityAd, AdmobAdCallback adCallback) {
        Admob.getInstance().getInterstitialAds(context, apInterstitialPriorityAd.getHigh3PriorityId(), new AdmobAdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.d(TAG, "onInterstitialLoad idAdsNormalPriority");
                apInterstitialPriorityAd.getHigh3PriorityInterstitialAd().setInterstitialAd(interstitialAd);
                adCallback.onApInterstitialLoad(apInterstitialPriorityAd.getHigh3PriorityInterstitialAd());
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "onAdFailedToLoad: idAdsNormalPriority: " + i);
                adCallback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                adCallback.onAdImpression();
            }
        });
    }

    private void loadInterNormalPriorityAdmob(Context context, ApInterstitialPriorityAd apInterstitialPriorityAd, AdmobAdCallback adCallback) {
        Admob.getInstance().getInterstitialAds(context, apInterstitialPriorityAd.getNormalPriorityId(), new AdmobAdCallback() {
            @Override
            public void onInterstitialLoad(@Nullable InterstitialAd interstitialAd) {
                super.onInterstitialLoad(interstitialAd);
                Log.d(TAG, "onInterstitialLoad idAdsNormalPriority");
                apInterstitialPriorityAd.getNormalPriorityInterstitialAd().setInterstitialAd(interstitialAd);
                adCallback.onApInterstitialLoad(apInterstitialPriorityAd.getNormalPriorityInterstitialAd());
            }

            @Override
            public void onAdFailedToLoad(@Nullable LoadAdError i) {
                super.onAdFailedToLoad(i);
                Log.e(TAG, "onAdFailedToLoad: idAdsNormalPriority: " + i);
                adCallback.onAdFailedToLoad(i);
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                adCallback.onAdClicked();
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                adCallback.onAdImpression();
            }
        });
    }

    public void forceShowInterstitialPriorityAdmob(Context context, ApInterstitialPriorityAd apInterstitialPriorityAd, AdmobAdCallback adCallback, boolean isReloadAds) {
        ApInterstitialAd interstitialAd;
        if (apInterstitialPriorityAd.getHigh1PriorityInterstitialAd() != null
                && apInterstitialPriorityAd.getHigh1PriorityInterstitialAd().isReady()
        ) {
            interstitialAd = apInterstitialPriorityAd.getHigh1PriorityInterstitialAd();
        } else if (apInterstitialPriorityAd.getHigh2PriorityInterstitialAd() != null
                && apInterstitialPriorityAd.getHigh2PriorityInterstitialAd().isReady()
        ) {
            interstitialAd = apInterstitialPriorityAd.getHigh2PriorityInterstitialAd();
        } else if (apInterstitialPriorityAd.getHigh3PriorityInterstitialAd() != null
                && apInterstitialPriorityAd.getHigh3PriorityInterstitialAd().isReady()
        ) {
            interstitialAd = apInterstitialPriorityAd.getHigh3PriorityInterstitialAd();
        } else if (apInterstitialPriorityAd.getNormalPriorityInterstitialAd() != null
                && apInterstitialPriorityAd.getNormalPriorityInterstitialAd().isReady()
        ) {
            interstitialAd = apInterstitialPriorityAd.getNormalPriorityInterstitialAd();
        } else {
            adCallback.onNextAction();
            if (isReloadAds) {
                loadPriorityInterstitialAdsAdmob(context, apInterstitialPriorityAd, new AdmobAdCallback());
            }
            return;
        }
        forceShowInterstitialAdmob(context,
                interstitialAd,
                new AdmobAdCallback() {
                    @Override
                    public void onNextAction() {
                        super.onNextAction();
                        adCallback.onNextAction();
                    }

                    @Override
                    public void onAdClosed() {
                        super.onAdClosed();
                        interstitialAd.setInterstitialAd(null);
                        adCallback.onAdClosed();
                        if (isReloadAds) {
                            loadPriorityInterstitialAdsAdmob(context, apInterstitialPriorityAd, new AdmobAdCallback());
                        }
                    }

                    @Override
                    public void onInterstitialShow() {
                        super.onInterstitialShow();
                        adCallback.onInterstitialShow();
                    }

                    @Override
                    public void onAdClicked() {
                        super.onAdClicked();
                        adCallback.onAdClicked();
                    }

                    @Override
                    public void onAdFailedToShow(@Nullable AdError adError) {
                        super.onAdFailedToShow(adError);
                        adCallback.onAdFailedToShow(adError);
                    }

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        adCallback.onAdImpression();
                    }
                },
                false
        );
    }
}
