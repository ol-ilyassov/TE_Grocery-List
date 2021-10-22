package net.penguincoders.doit;

import retrofit.Call;
import retrofit.http.POST;
import retrofit.http.Query;

public interface JsonPlaceHolderApi {
    @POST("getBasket")
    Call<ResponseBasket> getBasket(
            @Query("url") String url
    );
}
