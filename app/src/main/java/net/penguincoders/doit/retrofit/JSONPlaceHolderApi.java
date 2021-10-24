package net.penguincoders.doit.retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface JSONPlaceHolderApi {
    @POST("getBasket")
    public Call<myResponse> getBasket(
            @Body myRequest body
    );
}
