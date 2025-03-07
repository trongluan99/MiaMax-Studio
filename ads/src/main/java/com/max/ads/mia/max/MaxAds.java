package com.max.ads.mia.max;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.MaxRewardedAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.mediation.ads.MaxInterstitialAd;
import com.applovin.mediation.ads.MaxRewardedAd;
import com.applovin.mediation.nativeAds.MaxNativeAdListener;
import com.applovin.mediation.nativeAds.MaxNativeAdLoader;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.mediation.nativeAds.MaxNativeAdViewBinder;
import com.applovin.sdk.AppLovinSdk;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.max.ads.mia.R;
import com.max.ads.mia.admob.AppOpenManager;
import com.max.ads.mia.billing.AppPurchase;
import com.max.ads.mia.dialog.PrepareLoadingAdsDialog;
import com.max.ads.mia.event.MiaLogEventManager;
import com.max.ads.mia.funtion.AdType;
import com.max.ads.mia.util.SharePreferenceUtils;

import java.util.Calendar;

public class MaxAds {
    private static final String TAG = "MaxAd";
    private static MaxAds instance;
    private int currentClicked = 0;
    private int numShowAds = 3;

    private final int maxClickAds = 100;
    private Handler handlerTimeout;
    private Runnable rdTimeout;
    private PrepareLoadingAdsDialog dialog;
    private boolean isTimeout;

    public boolean isShowLoadingSplash = false;
    boolean isTimeDelay = false;
    private Context context;

    private MaxInterstitialAd interstitialSplash;
    private MaxNativeAdView nativeAdView;

    private String tokenAdjust;

    private boolean disableAdResumeWhenClickAds = false;
    private boolean openActivityAfterShowInterAds = false;

    public static MaxAds getInstance() {
        if (instance == null) {
            instance = new MaxAds();
            instance.isShowLoadingSplash = false;
        }
        return instance;
    }


    public void setOpenActivityAfterShowInterAds(boolean openActivityAfterShowInterAds) {
        this.openActivityAfterShowInterAds = openActivityAfterShowInterAds;
    }

    public void init(Context context, MaxAdCallback adCallback, String tokenAdjust) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = context.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }
        AppLovinSdk.getInstance(context).setMediationProvider("max");
        AppLovinSdk.initializeSdk(context, configuration -> {
            adCallback.initMaxSuccess();
        });
        this.tokenAdjust = tokenAdjust;
        this.context = context;
    }

    public void init(Context context, MaxAdCallback adCallback, Boolean enableDebug, String tokenAdjust) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = Application.getProcessName();
            String packageName = context.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }
        if (enableDebug) AppLovinSdk.getInstance(context).showMediationDebugger();
        AppLovinSdk.getInstance(context).setMediationProvider("max");
        AppLovinSdk.initializeSdk(context, configuration -> {
            Log.d(TAG, "init: applovin success");
            adCallback.initMaxSuccess();
        });
        this.tokenAdjust = tokenAdjust;
        this.context = context;
    }

    public void setNumShowAds(int numShowAds) {
        this.numShowAds = numShowAds;
    }

    public void setNumToShowAds(int numShowAds, int currentClicked) {
        this.numShowAds = numShowAds;
        this.currentClicked = currentClicked;
    }

    public MaxInterstitialAd getInterstitialSplash() {
        return interstitialSplash;
    }

    public void setDisableAdResumeWhenClickAds(boolean disableAdResumeWhenClickAds) {
        this.disableAdResumeWhenClickAds = disableAdResumeWhenClickAds;
    }

    public void loadSplashInterstitialAds(final AppCompatActivity context, String id, long timeOut, long timeDelay, MaxAdCallback adListener) {
        isTimeDelay = false;
        isTimeout = false;
        Log.i(TAG, "loadSplashInterstitialAds  start time loading:" + Calendar.getInstance().getTimeInMillis() + " ShowLoadingSplash:" + isShowLoadingSplash);

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onAdClosed();
            }
            return;
        }

        interstitialSplash = getInterstitialAds(context, id);
        new Handler().postDelayed(() -> {
            //check delay show ad splash
            if (interstitialSplash != null && interstitialSplash.isReady()) {
                Log.i(TAG, "loadSplashInterstitialAds:show ad on delay ");
                onShowSplash(context, adListener);
                return;
            }
            Log.i(TAG, "loadSplashInterstitialAds: delay validate");
            isTimeDelay = true;
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeout = new Handler();
            rdTimeout = () -> {
                Log.e(TAG, "loadSplashInterstitialAds: on timeout");
                isTimeout = true;
                if (interstitialSplash != null && interstitialSplash.isReady()) {
                    Log.i(TAG, "loadSplashInterstitialAds:show ad on timeout ");
                    onShowSplash(context, adListener);
                    return;
                }
                if (adListener != null) {
                    adListener.onAdClosed();
                    isShowLoadingSplash = false;
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeOut);
        }

        isShowLoadingSplash = true;

        interstitialSplash.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {
                Log.e(TAG, "loadSplashInterstitialAds end time loading success: " + Calendar.getInstance().getTimeInMillis() + " time limit:" + isTimeout);
                if (isTimeout) return;
                if (isTimeDelay) {
                    onShowSplash(context, adListener);
                    Log.i(TAG, "loadSplashInterstitialAds: show ad on loaded ");
                }
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {
                AppOpenMax.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().setInterstitialShowing(true);
            }

            @Override
            public void onAdHidden(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                if (disableAdResumeWhenClickAds) {
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "onAdLoadFailed: " + error.getMessage());
                if (isTimeout) return;
                if (adListener != null) {
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    Log.e(TAG, "loadSplashInterstitialAds: load fail " + error.getMessage());
                    adListener.onAdFailedToLoad(error);
                    adListener.onNextAction();
                }
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {
                if (adListener != null) {
                    adListener.onAdFailedToShow(error);
                    adListener.onNextAction();
                }
            }
        });
    }

    public void loadSplashInterstitialAds(final Activity context, String id, long timeOut, long timeDelay, boolean showSplashIfReady, MaxAdCallback adListener) {
        isTimeDelay = false;
        isTimeout = false;
        Log.i(TAG, "loadSplashInterstitialAds  start time loading:" + Calendar.getInstance().getTimeInMillis() + " ShowLoadingSplash:" + isShowLoadingSplash);

        if (AppPurchase.getInstance().isPurchased(context)) {
            if (adListener != null) {
                adListener.onAdClosed();
            }
            return;
        }

        interstitialSplash = getInterstitialAds(context, id);
        new Handler().postDelayed(() -> {
            //check delay show ad splash
            if (interstitialSplash != null && interstitialSplash.isReady()) {
                Log.i(TAG, "loadSplashInterstitialAds:show ad on delay ");
                if (showSplashIfReady)
                    onShowSplash((AppCompatActivity) context, adListener);
                else
                    adListener.onAdSplashReady();
                return;
            }
            Log.i(TAG, "loadSplashInterstitialAds: delay validate");
            isTimeDelay = true;
        }, timeDelay);

        if (timeOut > 0) {
            handlerTimeout = new Handler();
            rdTimeout = () -> {
                Log.e(TAG, "loadSplashInterstitialAds: on timeout");
                isTimeout = true;
                if (interstitialSplash != null && interstitialSplash.isReady()) {
                    Log.i(TAG, "loadSplashInterstitialAds:show ad on timeout ");
                    if (showSplashIfReady)
                        onShowSplash((AppCompatActivity) context, adListener);
                    else
                        adListener.onAdSplashReady();
                    return;
                }
                if (adListener != null) {
                    adListener.onNextAction();
                    isShowLoadingSplash = false;
                }
            };
            handlerTimeout.postDelayed(rdTimeout, timeOut);
        }

        isShowLoadingSplash = true;

        interstitialSplash.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {
                Log.e(TAG, "loadSplashInterstitialAds end time loading success: " + Calendar.getInstance().getTimeInMillis() + " time limit:" + isTimeout);
                if (isTimeout) return;
                if (isTimeDelay) {
                    if (showSplashIfReady)
                        onShowSplash((AppCompatActivity) context, adListener);
                    else
                        adListener.onAdSplashReady();
                    Log.i(TAG, "loadSplashInterstitialAds: show ad on loaded ");
                }
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {
                AppOpenMax.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().setInterstitialShowing(true);
            }

            @Override
            public void onAdHidden(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                if (adListener != null) {
                    adListener.onAdClicked();
                }
                if (disableAdResumeWhenClickAds) {
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "onAdLoadFailed: " + error.getMessage());
                if (isTimeout) return;
                if (adListener != null) {
                    adListener.onNextAction();
                    if (handlerTimeout != null && rdTimeout != null) {
                        handlerTimeout.removeCallbacks(rdTimeout);
                    }
                    Log.e(TAG, "loadSplashInterstitialAds: load fail " + error.getMessage());
                    adListener.onAdFailedToLoad(error);
                }
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {
                if (adListener != null) {
                    adListener.onAdFailedToShow(error);
                    adListener.onNextAction();
                }
            }
        });
    }

    public void onShowSplash(AppCompatActivity activity, MaxAdCallback adListener) {
        isShowLoadingSplash = true;
        Log.d(TAG, "onShowSplash: ");
        if (handlerTimeout != null && rdTimeout != null) {
            handlerTimeout.removeCallbacks(rdTimeout);
        }

        if (adListener != null) {
            adListener.onAdLoaded();
        }
        if (interstitialSplash == null) {
            assert adListener != null;
            adListener.onAdClosed();
            return;
        }
        interstitialSplash.setRevenueListener(ad -> {
            MiaLogEventManager.logPaidAdImpression(context, ad, AdType.INTERSTITIAL);
            if (tokenAdjust != null) {
                MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
            }
        });
        interstitialSplash.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {
                Log.d(TAG, "onAdDisplayed: ");
                AppOpenMax.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().setInterstitialShowing(true);
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }

            @Override
            public void onAdHidden(@NonNull MaxAd ad) {
                AppOpenMax.getInstance().setInterstitialShowing(false);
                AppOpenManager.getInstance().setInterstitialShowing(false);
                isShowLoadingSplash = false;
                if (adListener != null && ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    adListener.onAdClosed();
                    interstitialSplash = null;
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                MiaLogEventManager.logClickAdsEvent(context, interstitialSplash.getAdUnitId());
                if (adListener != null) {
                    adListener.onAdClicked();
                }
                if (disableAdResumeWhenClickAds) {
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {

            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {
                Log.d(TAG, "onAdDisplayFailed: " + error.getMessage());
                interstitialSplash = null;
                isShowLoadingSplash = false;
                if (adListener != null) {
                    adListener.onAdFailedToShow(error);
                    if (!openActivityAfterShowInterAds) {
                        adListener.onNextAction();
                    }
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }
        });

        if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            try {
                if (dialog != null && dialog.isShowing())
                    dialog.dismiss();
                dialog = new PrepareLoadingAdsDialog(activity);
                try {
                    dialog.show();
                    AppOpenManager.getInstance().setInterstitialShowing(true);
                    AppOpenMax.getInstance().setInterstitialShowing(true);
                } catch (Exception e) {
                    assert adListener != null;
                    adListener.onNextAction();
                    return;
                }
            } catch (Exception e) {
                dialog = null;
                assert adListener != null;
                adListener.onAdClosed();
                return;
            }
            new Handler().postDelayed(() -> {
                if (activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    if (openActivityAfterShowInterAds && adListener != null) {
                        adListener.onNextAction();
                        new Handler().postDelayed(() -> {
                            if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                                dialog.dismiss();
                        }, 1500);
                    }
                    if (interstitialSplash != null) {
                        interstitialSplash.showAd(activity);
                        isShowLoadingSplash = false;
                    } else if (adListener != null) {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                        adListener.onNextAction();
                        isShowLoadingSplash = false;
                    }
                } else {
                    if (dialog != null && dialog.isShowing() && !activity.isDestroyed())
                        dialog.dismiss();
                    isShowLoadingSplash = false;
                    assert adListener != null;
                    adListener.onAdClosed();
                }
            }, 800);
        } else {
            Log.e(TAG, "onShowSplash fail ");
            isShowLoadingSplash = false;
        }
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, MaxAdCallback callback, int timeDelay) {
        if (MaxAds.getInstance().getInterstitialSplash() != null && !MaxAds.getInstance().isShowLoadingSplash) {
            new Handler(activity.getMainLooper()).postDelayed(() -> {
                if (MaxAds.getInstance().getInterstitialSplash().isReady()) {
                    Log.i(TAG, "show ad splash when show fail in background");
                    MaxAds.getInstance().onShowSplash(activity, callback);
                } else {
                    callback.onAdClosed();
                }
            }, timeDelay);
        }
    }

    public MaxInterstitialAd getInterstitialAds(Context context, String id) {
        if (AppPurchase.getInstance().isPurchased(context) || MaxHelper.getNumClickAdsPerDay(context, id) >= maxClickAds) {
            Log.d(TAG, "getInterstitialAds: ignore");
            return null;
        }
        final MaxInterstitialAd interstitialAd = new MaxInterstitialAd(id, context);
        interstitialAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {
                Log.d(TAG, "onAdLoaded: getInterstitialAds");
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdHidden(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                if (disableAdResumeWhenClickAds)
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "onAdLoadFailed: getInterstitialAds " + error.getMessage());
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {

            }
        });
        requestInterstitialAds(interstitialAd);
        return interstitialAd;
    }

    private void requestInterstitialAds(MaxInterstitialAd maxInterstitialAd) {
        if (maxInterstitialAd != null && !maxInterstitialAd.isReady()) {
            maxInterstitialAd.loadAd();
        }
    }

    public void forceShowInterstitial(Context context, MaxInterstitialAd interstitialAd, final MaxAdCallback callback, boolean shouldReload) {
        currentClicked = numShowAds;
        showInterstitialAdByTimes(context, interstitialAd, callback, shouldReload);
    }

    public void showInterstitialAdByTimes(final Context context, MaxInterstitialAd interstitialAd, final MaxAdCallback callback, final boolean shouldReloadAds) {
        MaxHelper.setupMaxData(context);
        if (AppPurchase.getInstance().isPurchased(context)) {
            Log.d("DEV_ADS", "showInterstitialAdByTimes: 1");
            callback.onAdClosed();
            return;
        }
        if (interstitialAd == null || !interstitialAd.isReady()) {
            if (callback != null) {
                callback.onAdClosed();
            }
            Log.d("DEV_ADS", "showInterstitialAdByTimes: 2");
            return;
        }

        interstitialAd.setRevenueListener(ad -> {
            MiaLogEventManager.logPaidAdImpression(context, ad, AdType.INTERSTITIAL);
            if (tokenAdjust != null) {
                MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
            }
        });
        interstitialAd.setListener(new MaxAdListener() {
            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {
                callback.onAdLoaded();
                Log.d("DEV_ADS", "onAdLoaded: ");
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {
                AppOpenMax.getInstance().setInterstitialShowing(true);
                AppOpenManager.getInstance().setInterstitialShowing(true);
                Log.d("DEV_ADS", "onAdDisplayed: ");
            }


            @Override
            public void onAdHidden(@NonNull MaxAd ad) {
                SharePreferenceUtils.setLastImpressionInterstitialTime(context);
                AppOpenMax.getInstance().setInterstitialShowing(false);
                AppOpenManager.getInstance().setInterstitialShowing(false);
                if (callback != null && ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                    callback.onAdClosed();
                    if (!openActivityAfterShowInterAds) {
                        callback.onNextAction();
                    }
                    if (shouldReloadAds) {
                        requestInterstitialAds(interstitialAd);
                    }
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
                Log.d(TAG, "onAdHidden: " + ProcessLifecycleOwner.get().getLifecycle().getCurrentState());
                Log.d("DEV_ADS", "onAdHidden: ");
            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                if (callback != null) {
                    callback.onAdClicked();
                }
                if (disableAdResumeWhenClickAds) {
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                }
                Log.d("DEV_ADS", "onAdClicked: ");
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.d("DEV_ADS", "onAdLoadFailed: ");
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {
                Log.e(TAG, "onAdDisplayFailed: " + error.getMessage());
                Log.d("DEV_ADS", "onAdDisplayFailed: ");
                if (callback != null) {
                    callback.onAdClosed();
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                }
            }
        });
        if (MaxHelper.getNumClickAdsPerDay(context, interstitialAd.getAdUnitId()) < maxClickAds) {
            showInterstitialAd((Activity) context, interstitialAd, callback);
            Log.d("DEV_ADS", "MaxHelper.getNumClickAdsPerDay: ");
            return;
        }
        if (callback != null) {
            Log.d("DEV_ADS", "showInterstitialAdByTimes: 3");
            callback.onAdClosed();
        }
    }

    private void showInterstitialAd(Activity context, MaxInterstitialAd interstitialAd, MaxAdCallback callback) {
        currentClicked++;
        if (currentClicked >= numShowAds && interstitialAd != null) {
            if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                try {
                    if (dialog != null && dialog.isShowing()) dialog.dismiss();
                    dialog = new PrepareLoadingAdsDialog(context);
                    try {
                        Log.d(TAG, "showInterstitialAd: 1");
                        callback.onInterstitialShow();
                        dialog.setCancelable(false);
                        dialog.show();
                    } catch (Exception e) {
                        Log.d(TAG, "showInterstitialAd: 2");
                        callback.onAdClosed();
                        return;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "showInterstitialAd: 3");
                    dialog = null;
                }

                new Handler().postDelayed(() -> {
                    if (ProcessLifecycleOwner.get().getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                        if (openActivityAfterShowInterAds && callback != null) {
                            Log.d(TAG, "showInterstitialAd: 4");
                            callback.onNextAction();
                            new Handler().postDelayed(() -> {
                                if (dialog != null && dialog.isShowing() && !context.isDestroyed()) {
                                    Log.d(TAG, "showInterstitialAd: 5");
                                    dialog.dismiss();
                                }
                            }, 1500);
                        }
                        Log.d(TAG, "showInterstitialAd: 6");
                        interstitialAd.showAd(context);
                    } else {
                        if (dialog != null && dialog.isShowing() && !context.isDestroyed()) {
                            Log.d(TAG, "showInterstitialAd: 7");
                            dialog.dismiss();
                        }
                        Log.d(TAG, "showInterstitialAd: 8");
                        callback.onAdClosed();
                    }
                }, 800);
            }
            currentClicked = 0;
        } else if (callback != null) {
            if (dialog != null) {
                dialog.dismiss();
            }
            callback.onAdClosed();
        }
    }

    public void loadBanner(final Activity mActivity, String id) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer);
    }

    public void loadBanner(final Activity mActivity, String id, final MaxAdCallback adCallback) {
        final FrameLayout adContainer = mActivity.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, adCallback);
    }

    public void loadBannerFragment(final Activity mActivity, String id, final View rootView) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer);
    }

    public void loadBannerFragment(final Activity mActivity, String id, final View rootView, final MaxAdCallback adCallback) {
        final FrameLayout adContainer = rootView.findViewById(R.id.banner_container);
        final ShimmerFrameLayout containerShimmer = rootView.findViewById(R.id.shimmer_container_banner);
        loadBanner(mActivity, id, adContainer, containerShimmer, adCallback);
    }

    private void loadBanner(final Activity mActivity, String id, final FrameLayout adContainer, final ShimmerFrameLayout containerShimmer) {
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        MaxAdView adView = new MaxAdView(id, mActivity);
        adView.setRevenueListener(ad -> {
            MiaLogEventManager.logPaidAdImpression(mActivity, ad, AdType.BANNER);
            if (tokenAdjust != null) {
                MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
            }
        });
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int heightPx = mActivity.getResources().getDimensionPixelSize(R.dimen.banner_height);
        adView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
        adContainer.addView(adView);
        adView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {
                Log.d(TAG, "onAdLoaded: banner");
                containerShimmer.stopShimmer();
                containerShimmer.setVisibility(View.GONE);
                adContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdHidden(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                if (disableAdResumeWhenClickAds) {
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "onAdLoadFailed: banner " + error.getMessage() + "   code:" + error.getCode());
                containerShimmer.stopShimmer();
                adContainer.setVisibility(View.GONE);
                containerShimmer.setVisibility(View.GONE);
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {

            }
        });
        adView.loadAd();
    }

    private void loadBanner(final Activity mActivity, String id, final FrameLayout adContainer, final ShimmerFrameLayout containerShimmer, final MaxAdCallback adCallback) {
        if (AppPurchase.getInstance().isPurchased(mActivity)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();
        MaxAdView adView = new MaxAdView(id, mActivity);
        adView.setRevenueListener(ad -> {
            MiaLogEventManager.logPaidAdImpression(mActivity, ad, AdType.BANNER);
            if (tokenAdjust != null) {
                MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
            }
        });
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        // Banner height on phones and tablets is 50 and 90, respectively
        int heightPx = mActivity.getResources().getDimensionPixelSize(R.dimen.banner_height);
        adView.setLayoutParams(new FrameLayout.LayoutParams(width, heightPx));
        adContainer.addView(adView);
        adView.setListener(new MaxAdViewAdListener() {
            @Override
            public void onAdExpanded(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdCollapsed(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {
                Log.d(TAG, "onAdLoaded: banner");
                containerShimmer.stopShimmer();
                containerShimmer.setVisibility(View.GONE);
                adContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {
                if (adCallback != null) {
                    adCallback.onAdImpression();
                }
            }

            @Override
            public void onAdHidden(@NonNull MaxAd ad) {

            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                if (adCallback != null) {
                    adCallback.onAdClicked();
                }
                if (disableAdResumeWhenClickAds){
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "onAdLoadFailed: banner " + error.getMessage() + "   code:" + error.getCode());
                containerShimmer.stopShimmer();
                adContainer.setVisibility(View.GONE);
                containerShimmer.setVisibility(View.GONE);
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {

            }
        });
        adView.loadAd();
    }

    public void loadNative(final Activity mActivity, String adUnitId) {
        final FrameLayout frameLayout = mActivity.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_native);
        loadNativeAd(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_max_free_size);
    }

    public void loadNativeSmall(final Activity mActivity, String adUnitId) {
        final FrameLayout frameLayout = mActivity.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = mActivity.findViewById(R.id.shimmer_container_native);
        loadNativeAd(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_max_medium);
    }

    public void loadNativeFragment(final Activity mActivity, String adUnitId, View parent) {
        final FrameLayout frameLayout = parent.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = parent.findViewById(R.id.shimmer_container_native);
        loadNativeAd(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_max_free_size);
    }

    public void loadNativeSmallFragment(final Activity mActivity, String adUnitId, View parent) {
        final FrameLayout frameLayout = parent.findViewById(R.id.fl_adplaceholder);
        final ShimmerFrameLayout containerShimmer = parent.findViewById(R.id.shimmer_container_native);
        loadNativeAd(mActivity, containerShimmer, frameLayout, adUnitId, R.layout.custom_native_max_medium);
    }


    public void loadNativeAd(Activity activity, ShimmerFrameLayout containerShimmer, FrameLayout nativeAdLayout, String id, int layoutCustomNative) {

        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();

        nativeAdLayout.removeAllViews();
        nativeAdLayout.setVisibility(View.GONE);
        MaxNativeAdViewBinder binder = new MaxNativeAdViewBinder.Builder(layoutCustomNative).setTitleTextViewId(R.id.ad_headline).setBodyTextViewId(R.id.ad_body).setAdvertiserTextViewId(R.id.ad_advertiser).setIconImageViewId(R.id.ad_app_icon).setMediaContentViewGroupId(R.id.ad_media).setOptionsContentViewGroupId(R.id.ad_options_view).setCallToActionButtonId(R.id.ad_call_to_action).build();

        nativeAdView = new MaxNativeAdView(binder, activity);

        MaxNativeAdLoader nativeAdLoader = new MaxNativeAdLoader(id, activity);
        nativeAdLoader.setRevenueListener(ad -> {
            MiaLogEventManager.logPaidAdImpression(activity, ad, AdType.NATIVE);
            if (tokenAdjust != null) {
                MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
            }
        });
        nativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(MaxNativeAdView nativeAdView, @NonNull MaxAd ad) {
                Log.d(TAG, "onNativeAdLoaded ");
                containerShimmer.stopShimmer();
                containerShimmer.setVisibility(View.GONE);
                nativeAdLayout.setVisibility(View.VISIBLE);
                nativeAdLayout.addView(nativeAdView);
            }

            @Override
            public void onNativeAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "onAdFailedToLoad: " + error.getMessage());
                containerShimmer.stopShimmer();
                containerShimmer.setVisibility(View.GONE);
                nativeAdLayout.setVisibility(View.GONE);
            }

            @Override
            public void onNativeAdClicked(@NonNull MaxAd ad) {
                Log.e(TAG, "`onNativeAdClicked`: ");
                containerShimmer.setVisibility(View.VISIBLE);
                containerShimmer.startShimmer();
                nativeAdLayout.removeAllViews();
                nativeAdLayout.setVisibility(View.GONE);

                nativeAdView = new MaxNativeAdView(binder, activity);
                nativeAdLoader.loadAd(nativeAdView);
                if (disableAdResumeWhenClickAds){
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            }
        });
        nativeAdLoader.loadAd(nativeAdView);
    }

    public void loadNativeAd(Activity activity, ShimmerFrameLayout containerShimmer, FrameLayout nativeAdLayout, String id, int layoutCustomNative, MaxAdCallback callback) {

        if (AppPurchase.getInstance().isPurchased(context)) {
            containerShimmer.setVisibility(View.GONE);
            return;
        }
        containerShimmer.setVisibility(View.VISIBLE);
        containerShimmer.startShimmer();

        nativeAdLayout.removeAllViews();
        nativeAdLayout.setVisibility(View.GONE);
        MaxNativeAdViewBinder binder = new MaxNativeAdViewBinder.Builder(layoutCustomNative).setTitleTextViewId(R.id.ad_headline).setBodyTextViewId(R.id.ad_body).setAdvertiserTextViewId(R.id.ad_advertiser).setIconImageViewId(R.id.ad_app_icon).setMediaContentViewGroupId(R.id.ad_media).setOptionsContentViewGroupId(R.id.ad_options_view).setCallToActionButtonId(R.id.ad_call_to_action).build();

        nativeAdView = new MaxNativeAdView(binder, activity);

        MaxNativeAdLoader nativeAdLoader = new MaxNativeAdLoader(id, activity);
        nativeAdLoader.setRevenueListener(ad -> {
            MiaLogEventManager.logPaidAdImpression(activity, ad, AdType.NATIVE);
            if (tokenAdjust != null) {
                MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
            }
        });
        nativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(MaxNativeAdView nativeAdView, @NonNull MaxAd ad) {
                Log.d(TAG, "onNativeAdLoaded ");
                populateNativeAdView(nativeAdView, nativeAdLayout, containerShimmer);
                callback.onUnifiedNativeAdLoaded(nativeAdView);
            }

            @Override
            public void onNativeAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.e(TAG, "onAdFailedToLoad: " + error.getMessage());
                containerShimmer.stopShimmer();
                containerShimmer.setVisibility(View.GONE);
                nativeAdLayout.setVisibility(View.GONE);

                callback.onAdFailedToLoad(error);
            }

            @Override
            public void onNativeAdClicked(@NonNull MaxAd ad) {
                Log.e(TAG, "`onNativeAdClicked`: ");
                containerShimmer.setVisibility(View.VISIBLE);
                containerShimmer.startShimmer();
                nativeAdLayout.removeAllViews();
                nativeAdLayout.setVisibility(View.GONE);

                nativeAdView = new MaxNativeAdView(binder, activity);
                nativeAdLoader.loadAd(nativeAdView);
                if (disableAdResumeWhenClickAds) {
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }

                callback.onAdClicked();
            }
        });
        nativeAdLoader.loadAd(nativeAdView);
    }

    public void populateNativeAdView(View apNativeAd, FrameLayout frAds, ShimmerFrameLayout shimmerAds) {
        shimmerAds.stopShimmer();
        shimmerAds.setVisibility(View.GONE);
        frAds.setVisibility(View.VISIBLE);
        frAds.removeAllViews();
        if (apNativeAd.getParent() != null) {
            ((ViewGroup) apNativeAd.getParent()).removeAllViews();
        }
        frAds.addView(apNativeAd);
    }

    public void loadNativeAd(Activity activity, String id, int layoutCustomNative, MaxAdCallback callback) {

        if (AppPurchase.getInstance().isPurchased(context)) {
            callback.onAdClosed();
            return;
        }

        MaxNativeAdViewBinder binder = new MaxNativeAdViewBinder.Builder(layoutCustomNative).setTitleTextViewId(R.id.ad_headline).setBodyTextViewId(R.id.ad_body).setAdvertiserTextViewId(R.id.ad_advertiser).setIconImageViewId(R.id.ad_app_icon).setMediaContentViewGroupId(R.id.ad_media).setOptionsContentViewGroupId(R.id.ad_options_view).setCallToActionButtonId(R.id.ad_call_to_action).build();

        nativeAdView = new MaxNativeAdView(binder, activity);

        MaxNativeAdLoader nativeAdLoader = new MaxNativeAdLoader(id, activity);
        nativeAdLoader.setRevenueListener(ad -> {
            MiaLogEventManager.logPaidAdImpression(activity, ad, AdType.NATIVE);
            if (tokenAdjust != null) {
                MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
            }
        });
        nativeAdLoader.setNativeAdListener(new MaxNativeAdListener() {
            @Override
            public void onNativeAdLoaded(final MaxNativeAdView nativeAdView, final MaxAd ad) {
                Log.d(TAG, "onNativeAdLoaded ");
                callback.onUnifiedNativeAdLoaded(nativeAdView);
            }

            @Override
            public void onNativeAdLoadFailed(final String adUnitId, final MaxError error) {
                Log.e(TAG, "onAdFailedToLoad: " + error.getMessage());
                callback.onAdFailedToLoad(error);
            }

            @Override
            public void onNativeAdClicked(final MaxAd ad) {
                Log.e(TAG, "onNativeAdClicked: ");
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                callback.onAdClicked();
                if (disableAdResumeWhenClickAds){
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            }
        });
        nativeAdLoader.loadAd(nativeAdView);
    }

    public MaxRewardedAd getRewardAd(Activity activity, String id, MaxAdCallback callback) {
        MaxRewardedAd rewardedAd = MaxRewardedAd.getInstance(id, activity);
        rewardedAd.setListener(new MaxRewardedAdListener() {
            @Override
            public void onRewardedVideoStarted(@NonNull MaxAd ad) {
                Log.d(TAG, "onRewardedVideoStarted: ");
            }

            @Override
            public void onRewardedVideoCompleted(@NonNull MaxAd ad) {
                Log.d(TAG, "onRewardedVideoCompleted: ");
            }

            @Override
            public void onUserRewarded(@NonNull MaxAd ad, @NonNull MaxReward reward) {
                callback.onUserRewarded(reward);
                Log.d(TAG, "onUserRewarded: ");
            }

            @Override
            public void onAdLoaded(@NonNull MaxAd ad) {
                Log.d(TAG, "onAdLoaded: ");
                callback.onAdLoaded();
            }

            @Override
            public void onAdDisplayed(@NonNull MaxAd ad) {
                Log.d(TAG, "onAdDisplayed: ");
                callback.onAdDisplayed();
                AppOpenManager.getInstance().setInterstitialShowing(true);
                AppOpenMax.getInstance().setInterstitialShowing(true);
            }

            @Override
            public void onAdHidden(@NonNull MaxAd ad) {
                callback.onAdClosed();
                Log.d(TAG, "onAdHidden: ");
                AppOpenManager.getInstance().setInterstitialShowing(false);
                AppOpenMax.getInstance().setInterstitialShowing(false);
            }

            @Override
            public void onAdClicked(@NonNull MaxAd ad) {
                MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                callback.onAdClicked();
                if (disableAdResumeWhenClickAds) {
                    AppOpenMax.getInstance().disableAdResumeByClickAction();
                    AppOpenManager.getInstance().disableAdResumeByClickAction();
                }
            }

            @Override
            public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                Log.d(TAG, "onAdLoadFailed: " + error.getMessage());
                callback.onAdFailedToLoad(error);
            }

            @Override
            public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {
                Log.d(TAG, "onAdDisplayFailed: " + error.getMessage());
                callback.onAdFailedToShow(error);
            }
        });
        rewardedAd.loadAd();
        return rewardedAd;
    }

    public MaxRewardedAd getRewardAd(Activity activity, String id) {
        MaxRewardedAd rewardedAd = MaxRewardedAd.getInstance(id, activity);
        rewardedAd.loadAd();
        return rewardedAd;
    }

    public void showRewardAd(Activity activity, MaxRewardedAd maxRewardedAd, MaxAdCallback callback) {
        if (maxRewardedAd.isReady()) {
            maxRewardedAd.setRevenueListener(ad -> {
                MiaLogEventManager.logPaidAdImpression(activity, ad, AdType.REWARDED);
                if (tokenAdjust != null) {
                    MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
                }
            });
            maxRewardedAd.setListener(new MaxRewardedAdListener() {
                @Override
                public void onRewardedVideoStarted(@NonNull MaxAd ad) {
                    Log.d(TAG, "onRewardedVideoStarted: ");
                }

                @Override
                public void onRewardedVideoCompleted(@NonNull MaxAd ad) {
                    Log.d(TAG, "onRewardedVideoCompleted: ");
                }

                @Override
                public void onUserRewarded(@NonNull MaxAd ad, @NonNull MaxReward reward) {
                    callback.onUserRewarded(reward);
                    Log.d(TAG, "onUserRewarded: ");
                }

                @Override
                public void onAdLoaded(@NonNull MaxAd ad) {
                    Log.d(TAG, "onAdLoaded: ");
                    callback.onAdLoaded();
                }

                @Override
                public void onAdDisplayed(@NonNull MaxAd ad) {
                    Log.d(TAG, "onAdDisplayed: ");
                    callback.onAdDisplayed();
                    AppOpenManager.getInstance().setInterstitialShowing(true);
                    AppOpenMax.getInstance().setInterstitialShowing(true);
                }

                @Override
                public void onAdHidden(@NonNull MaxAd ad) {
                    callback.onAdClosed();
                    Log.d(TAG, "onAdHidden: ");
                    AppOpenManager.getInstance().setInterstitialShowing(false);
                    AppOpenMax.getInstance().setInterstitialShowing(false);
                }

                @Override
                public void onAdClicked(@NonNull MaxAd ad) {
                    MiaLogEventManager.logClickAdsEvent(context, ad.getAdUnitId());
                    callback.onAdClicked();
                    if (disableAdResumeWhenClickAds) {
                        AppOpenMax.getInstance().disableAdResumeByClickAction();
                        AppOpenManager.getInstance().disableAdResumeByClickAction();
                    }
                }

                @Override
                public void onAdLoadFailed(@NonNull String adUnitId, @NonNull MaxError error) {
                    Log.d(TAG, "onAdLoadFailed: " + error.getMessage());
                    callback.onAdFailedToLoad(error);
                }

                @Override
                public void onAdDisplayFailed(@NonNull MaxAd ad, @NonNull MaxError error) {
                    Log.d(TAG, "onAdDisplayFailed: " + error.getMessage());
                    callback.onAdFailedToShow(error);
                }
            });
            maxRewardedAd.showAd();
        } else {
            Log.e(TAG, "showRewardAd error -  reward ad not ready");
            callback.onAdFailedToShow(null);
        }
    }

    public void showRewardAd(Activity activity, MaxRewardedAd maxRewardedAd, String tokenAdjust) {
        if (maxRewardedAd.isReady()) {
            maxRewardedAd.setRevenueListener(ad -> {
                MiaLogEventManager.logPaidAdImpression(activity, ad, AdType.REWARDED);
                if (tokenAdjust != null) {
                    MiaLogEventManager.logPaidAdjustWithTokenMax(ad, ad.getAdUnitId(), tokenAdjust);
                }
            });
            maxRewardedAd.showAd();
        } else {
            Log.e(TAG, "showRewardAd error -  reward ad not ready");
        }
    }
}