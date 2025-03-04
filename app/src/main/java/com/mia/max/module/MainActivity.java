package com.mia.max.module;

import android.os.Bundle;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.max.ads.mia.ads.wrapper.ApInterstitialAd;
import com.max.ads.mia.ads.wrapper.ApNativeAd;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.rewarded.RewardedAd;

public class MainActivity extends AppCompatActivity {
    private ApInterstitialAd mInterstitialAd;
    private Button btnLoad, btnShow, btnIap, btnLoadReward, btnShowReward;
    private FrameLayout frAds;
    private ShimmerFrameLayout shimmerAds;
    private ApNativeAd mApNativeAd;
    private RewardedAd rewardedAds;
    private boolean isEarn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnLoad = findViewById(R.id.btnLoad);
        btnShow = findViewById(R.id.btnShow);
        btnIap = findViewById(R.id.btnIap);
        btnLoadReward = findViewById(R.id.btnLoadReward);
        btnShowReward = findViewById(R.id.btnShowReward);
        frAds = findViewById(R.id.fr_ads);
        shimmerAds = findViewById(R.id.shimmer_native);

    }
}
