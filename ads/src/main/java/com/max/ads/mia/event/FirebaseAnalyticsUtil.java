package com.max.ads.mia.event;

import android.content.Context;
import android.os.Bundle;

import com.max.ads.mia.config.MiaAdConfig;
import com.google.firebase.analytics.FirebaseAnalytics;

public class FirebaseAnalyticsUtil {
    private static final String TAG = "FirebaseAnalyticsUtil";

    public static void logEventWithAds(Context context, Bundle params) {
        FirebaseAnalytics.getInstance(context).logEvent("paid_ad_impression", params);
    }

    public static void logEventWithAds(Context context, String event, Bundle params) {
        FirebaseAnalytics.getInstance(context).logEvent(event, params);
    }


    static void logPaidAdImpressionValue(Context context, Bundle bundle, int mediationProvider) {
        if (mediationProvider == MiaAdConfig.PROVIDER_MAX)
            FirebaseAnalytics.getInstance(context).logEvent("max_paid_ad_impression_value", bundle);
        else
            FirebaseAnalytics.getInstance(context).logEvent("paid_ad_impression_value", bundle);
    }

    public static void logClickAdsEvent(Context context, Bundle bundle) {

        FirebaseAnalytics.getInstance(context).logEvent("event_user_click_ads", bundle);
    }

    public static void logCurrentTotalRevenueAd(Context context, String eventName, Bundle bundle) {
        FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle);
    }

    public static void logTotalRevenue001Ad(Context context, Bundle bundle) {
        FirebaseAnalytics.getInstance(context).logEvent("paid_ad_impression_value_001", bundle);
    }

}
