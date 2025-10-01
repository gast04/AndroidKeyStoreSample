package com.sample.demo_keystore;

import android.util.Log;

import okhttp3.OkHttpClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Url;

import java.util.concurrent.TimeUnit;

final class VerifyRequest {
    public final String pin;
    public final String castle_token;

    public VerifyRequest(String pin, String castle_token) {
        this.pin = pin;
        this.castle_token = castle_token;
    }
}

final class VerifyResponse {
    public boolean valid;
    public String message;
}

interface PinApi {
    @POST
    Call<VerifyResponse> verify(@Url String url, @Body VerifyRequest body);
}

final class TokenResponse {
    public String token;
}

interface CastleApi {
    @GET
    Call<TokenResponse> fetch_user_token(@Url String url);
}

public class PinWebHandling {

    private String backend_base_url = null;

    private Retrofit retrofit = null;

    private String castle_user_token = null;

    public interface PinVerificationCallback {
        void onSuccess(boolean isValid);

        void onError(Throwable error);
    }

    PinWebHandling(String url) {
        backend_base_url = url;

        // NOTE: this logger is very verbose
        // HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        // interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient http_client =
                new OkHttpClient.Builder()
                        // .addInterceptor(interceptor)
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .writeTimeout(10, TimeUnit.SECONDS)
                        .build();

        retrofit =
                new Retrofit.Builder()
                        .baseUrl(backend_base_url)
                        .client(http_client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
    }

    public String fetchToken() throws Exception {
        Log.v(constants.FUNCTAG, "fetchToken");

        CastleApi api = retrofit.create(CastleApi.class);
        Call<TokenResponse> call = api.fetch_user_token(backend_base_url + "castle_user_jwt/");
        Response<TokenResponse> response = call.execute();

        if (response.isSuccessful() && response.body() != null) {
            Log.d("TOKEN WEB HANDLING", "Token: " + response.body().token);
            return response.body().token;
        } else {
            throw new Exception("Token fetching failed: " + response.code());
        }
    }

    public void verifyPIN(String input, String request_token, PinVerificationCallback callback) {
        Log.v(constants.FUNCTAG, "verifyPINWeb");

        PinApi api = retrofit.create(PinApi.class);
        VerifyRequest req = new VerifyRequest(input, request_token);
        api.verify(backend_base_url + "check/", req)
                .enqueue(
                        new Callback<VerifyResponse>() {
                            @Override
                            public void onResponse(
                                    Call<VerifyResponse> call, Response<VerifyResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    boolean valid = response.body().valid;
                                    callback.onSuccess(valid);
                                } else {
                                    callback.onError(
                                            new Exception(
                                                    "Verification failed: " + response.code()));
                                }
                            }

                            @Override
                            public void onFailure(Call<VerifyResponse> call, Throwable t) {
                                Log.e(constants.WEBTAG, "Network error", t);
                                callback.onError(t);
                            }
                        });
    }
}
