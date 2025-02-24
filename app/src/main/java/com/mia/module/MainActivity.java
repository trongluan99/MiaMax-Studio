package com.mia.module;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ads.mia.ads.MiaAd;
import com.ads.mia.ads.wrapper.ApInterstitialAd;
import com.ads.mia.ads.wrapper.ApNativeAd;
import com.ads.mia.billing.AppPurchase;
import com.ads.mia.funtion.PurchaseListener;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
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
