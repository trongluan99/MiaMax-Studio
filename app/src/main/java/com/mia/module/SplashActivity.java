package com.mia.module;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ads.mia.ads.MiaAd;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        /*MiaAd.getInstance().loadSplashInterstitialAds(this, BuildConfig.ad_interstitial_splash, 25000, 5000, new AdCallback() {
            @Override
            public void onNextAction() {
                super.onNextAction();
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        });*/

        MiaAd.getInstance().loadInterSplashPriority4SameTime(this,
                BuildConfig.ad_interstitial_splash,
                BuildConfig.ad_interstitial_splash,
                BuildConfig.ad_interstitial_splash,
                BuildConfig.ad_interstitial_splash, 30000, 5000, new AdCallback() {
                    @Override
                    public void onAdSplashHigh1Ready() {
                        super.onAdSplashHigh1Ready();
                        Log.d("LuanDev", "onAdSplashHigh1Ready: 1");
                        MiaAd.getInstance().onShowSplashPriority4(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                            }
                        });
                    }

                    @Override
                    public void onAdSplashHigh2Ready() {
                        super.onAdSplashHigh2Ready();
                        Log.d("LuanDev", "onAdSplashHigh2Ready: 2");
                        MiaAd.getInstance().onShowSplashPriority4(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                            }
                        });
                    }

                    @Override
                    public void onAdSplashHigh3Ready() {
                        super.onAdSplashHigh3Ready();
                        Log.d("LuanDev", "onAdSplashHigh3Ready: 3");
                        MiaAd.getInstance().onShowSplashPriority4(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                            }
                        });
                    }

                    @Override
                    public void onAdSplashNormalReady() {
                        super.onAdSplashNormalReady();
                        Log.d("LuanDev", "onAdSplashNormalReady: 0");
                        MiaAd.getInstance().onShowSplashPriority4(SplashActivity.this, new AdCallback() {
                            @Override
                            public void onNextAction() {
                                super.onNextAction();
                                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                                finish();
                            }
                        });
                    }
                });


    }
}
