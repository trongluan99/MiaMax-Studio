package com.max.ads.mia.application;

import androidx.multidex.MultiDexApplication;

import com.max.ads.mia.config.MiaAdConfig;
import com.max.ads.mia.util.AppUtil;
import com.max.ads.mia.util.SharePreferenceUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AdsMultiDexApplication extends MultiDexApplication {

    protected MiaAdConfig mMiaAdConfig;
    protected List<String> listTestDevice;

    @Override
    public void onCreate() {
        super.onCreate();
        listTestDevice = new ArrayList<String>();
        mMiaAdConfig = new MiaAdConfig(this);
        if (SharePreferenceUtils.getInstallTime(this) == 0) {
            SharePreferenceUtils.setInstallTime(this);
        }
        AppUtil.currentTotalRevenue001Ad = SharePreferenceUtils.getCurrentTotalRevenue001Ad(this);
    }


}
