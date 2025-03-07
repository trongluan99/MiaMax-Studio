package com.max.ads.mia.max;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAppOpenAd;
import com.applovin.sdk.AppLovinSdk;
import com.max.ads.mia.admob.AppOpenManager;
import com.max.ads.mia.config.MiaAdConfig;
import com.max.ads.mia.event.MiaLogEventManager;
import com.max.ads.mia.funtion.AdType;


public class MaxOpenSplashManager {
    private static final String TAG = "MaxOpenSplashManager";
    private MaxAppOpenAd openAdSplash;
    private boolean isShowingAd = false;

    private static MaxOpenSplashManager instance;

    public static MaxOpenSplashManager getInstance() {
        if (instance == null) {
            instance = new MaxOpenSplashManager();
            instance.isShowingAd = false;
        }
        return instance;
    }

    public void loadOpenMaxSplash(Context context, String idOpenSplash, int timeDelay, int timeOut, boolean isShowAdIfReady, MaxAdCallback adCallback) {
        if (!isNetworkConnected(context)) {
            (new Handler()).postDelayed(new Runnable() {
                public void run() {
                    adCallback.onNextAction();
                }
            }, timeDelay);
        } else {
            final long currentTimeMillis = System.currentTimeMillis();
            final Runnable timeOutRunnable = () -> {
                adCallback.onNextAction();
                isShowingAd = false;
            };
            final Handler handler = new Handler();
            handler.postDelayed(timeOutRunnable, timeOut);
            openAdSplash = new MaxAppOpenAd(idOpenSplash, context);
            openAdSplash.setListener(new MaxAdListener() {
                @Override
                public void onAdLoaded(MaxAd maxAd) {
                    handler.removeCallbacks(timeOutRunnable);
                    if (isShowAdIfReady) {
                        long elapsedTime = System.currentTimeMillis() - currentTimeMillis;
                        if (elapsedTime >= timeDelay) {
                            elapsedTime = 0L;
                        }

                        Handler handler1 = new Handler();
                        Runnable showAppOpenSplashRunnable = () -> {
                            showAdIfReady(context, adCallback);
                        };
                        handler1.postDelayed(showAppOpenSplashRunnable, elapsedTime);
                    } else {
                        adCallback.onAdSplashReady();
                    }
                }

                @Override
                public void onAdDisplayed(MaxAd maxAd) {
                    Log.d(TAG, "onAdDisplayed: ");
                }

                @Override
                public void onAdHidden(MaxAd maxAd) {
                    Log.d(TAG, "onAdHidden: ");
                }

                @Override
                public void onAdClicked(MaxAd maxAd) {
                    Log.d(TAG, "onAdClicked: ");
                }

                @Override
                public void onAdLoadFailed(String s, MaxError maxError) {
                    Log.d(TAG, "onAdLoadFailed: ");
                    openAdSplash.loadAd();
                }

                @Override
                public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                    Log.d(TAG, "onAdDisplayFailed: ");
                }
            });
            openAdSplash.loadAd();
        }
    }

    private void showAdIfReady(Context context, MaxAdCallback adCallback) {
        if (!AppLovinSdk.getInstance(context).isInitialized()) {
            if (adCallback != null) {
                adCallback.onNextAction();
                return;
            }
            return;
        }

        if (openAdSplash.isReady()) {
            openAdSplash.setListener(new MaxAdListener() {
                @Override
                public void onAdLoaded(MaxAd maxAd) {
                    if (adCallback != null) {
                        adCallback.onAdLoaded();
                    }
                }

                @Override
                public void onAdDisplayed(MaxAd maxAd) {
                    isShowingAd = true;
                    if (adCallback != null) {
                        adCallback.onAdDisplayed();
                    }
                    AppOpenMax.getInstance().setInterstitialShowing(true);
                    AppOpenManager.getInstance().setInterstitialShowing(true);
                }

                @Override
                public void onAdHidden(MaxAd maxAd) {
                    if (adCallback != null) {
                        adCallback.onNextAction();
                    }
                    AppOpenMax.getInstance().setInterstitialShowing(false);
                    AppOpenManager.getInstance().setInterstitialShowing(false);
                }

                @Override
                public void onAdClicked(MaxAd maxAd) {
                    if (adCallback != null) {
                        adCallback.onAdClicked();
                    }
                }

                @Override
                public void onAdLoadFailed(String s, MaxError maxError) {
                    if (adCallback != null) {
                        adCallback.onAdLoadFailed();
                    }
                }

                @Override
                public void onAdDisplayFailed(MaxAd maxAd, MaxError maxError) {
                    if (adCallback != null) {
                        adCallback.onAdDisplayFailed();
                    }
                }
            });
            openAdSplash.setRevenueListener(maxAd -> {
                MiaLogEventManager.logPaidAdImpression(context, maxAd, AdType.APP_OPEN);
                if (MiaAdConfig.ADJUST_TOKEN_TIKTOK != null) {
                    MiaLogEventManager.logPaidAdjustWithTokenMax(maxAd, maxAd.getAdUnitId(), MiaAdConfig.ADJUST_TOKEN_TIKTOK);
                }
            });
            openAdSplash.showAd();
        } else {
            openAdSplash.loadAd();
        }
    }

    public void onCheckShowSplashWhenFail(AppCompatActivity activity, MaxAdCallback adCallback, int timeDelay) {
        (new Handler(activity.getMainLooper())).postDelayed(new Runnable() {
            public void run() {
                if (openAdSplash != null && !isShowingAd) {
                    Log.d(TAG, "show ad splash when show fail in background");
                    showAdIfReady(activity, adCallback);
                }
            }
        }, timeDelay);
    }

    private boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}
