/*
 * Copyright (c) 2022 Castle
 */

package com.sample.demo_keystore.castle.api.model;

import com.google.gson.annotations.SerializedName;

/**
 * Copyright (c) 2022 Castle
 */
public class UserJwt {
    @SerializedName("jwt")
    private final String jwt;

    public UserJwt(String jwt) {
        this.jwt = jwt;
    }
}