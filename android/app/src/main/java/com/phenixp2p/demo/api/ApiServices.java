/*
 * Copyright (c) 2016. PhenixP2P Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0(the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.phenixp2p.demo.api;

import com.google.gson.JsonElement;
import com.phenixp2p.demo.api.response.Authentication;
import com.phenixp2p.demo.api.response.StreamToken;
import com.phenixp2p.demo.model.StreamList;

import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import rx.Observable;

public interface ApiServices {

  @FormUrlEncoded
  @POST("login")
  Observable<Authentication> login(@Field("name") String name, @Field("password") String password);

  @POST("stream")
  Observable<StreamToken> stream(@Body JsonElement jsonElement);

  @PUT("streams")
  Observable<StreamList> listStreams(@Body JsonElement jsonElement);
}
