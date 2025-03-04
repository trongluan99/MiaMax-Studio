package com.mia.max.module;

import androidx.multidex.BuildConfig;

import com.max.ads.mia.admob.Admob;
import com.max.ads.mia.admob.AppOpenManager;
import com.max.ads.mia.ads.MiaAd;
import com.max.ads.mia.application.AdsMultiDexApplication;
import com.max.ads.mia.billing.AppPurchase;
import com.max.ads.mia.config.AdjustConfig;
import com.max.ads.mia.config.MiaAdConfig;

import java.util.ArrayList;
import java.util.List;

public class App extends AdsMultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        initAds();
        initBilling();
    }

    private void initAds() {
        String environment = BuildConfig.DEBUG ? MiaAdConfig.ENVIRONMENT_DEVELOP : MiaAdConfig.ENVIRONMENT_PRODUCTION;
        mMiaAdConfig = new MiaAdConfig(this, environment);

        AdjustConfig adjustConfig = new AdjustConfig(true, getString(R.string.adjust_token));
        mMiaAdConfig.setAdjustConfig(adjustConfig);
        mMiaAdConfig.setFacebookClientToken(getString(R.string.facebook_client_token));
        mMiaAdConfig.setAdjustTokenTiktok(getString(R.string.tiktok_token));

        mMiaAdConfig.setIdAdResumeAdmob("");
        mMiaAdConfig.setIdAdResumeMax("");
        mMiaAdConfig.setEnableAdResumeAdmob(true);
        mMiaAdConfig.setEnableAdResumeMax(false);

        MiaAd.getInstance().init(this, mMiaAdConfig, BuildConfig.DEBUG);
        Admob.getInstance().setDisableAdResumeWhenClickAds(true);
        Admob.getInstance().setOpenActivityAfterShowInterAds(true);
        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity.class);
    }

    private void initBilling() {
        List<String> listIAP = new ArrayList<>();
        listIAP.add("android.test.purchased");
        List<String> listSub = new ArrayList<>();
        AppPurchase.getInstance().initBilling(this, listIAP, listSub);
    }
}
