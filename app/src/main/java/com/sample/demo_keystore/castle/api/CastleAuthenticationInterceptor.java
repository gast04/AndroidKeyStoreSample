/*
 * Copyright (c) 2020 Castle
 */

package com.sample.demo_keystore.castle.api;

import com.sample.demo_keystore.castle.Castle;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

/** Interceptor used for auth to Castle APIs */
class CastleAuthenticationInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();

        Request.Builder builder =
                originalRequest
                        .newBuilder()
                        .header("X-Castle-Publishable-Api-Key", Castle.publishableKey())
                        .header(Castle.requestTokenHeaderName, Castle.createRequestToken())
                        .header("User-Agent", Castle.userAgent());

        return chain.proceed(builder.build());
    }
}
