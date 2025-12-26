package com.example.scanqr;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

@GlideModule
public class MyAppGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(
            @NonNull Context context,
            @NonNull Glide glide,
            @NonNull Registry registry
    ) {
        // OkHttpClient dengan timeout lebih besar
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS) // sebelumnya 2.5 detik dari Glide default
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();

        OkHttpUrlLoader.Factory factory = new OkHttpUrlLoader.Factory(client);

        // Ganti cara Glide fetch data HTTP pakai OkHttp client ini
        registry.replace(GlideUrl.class, InputStream.class, factory);
    }

    // Optional: matikan manifest parsing (biar warning ilang)
    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
