package net.penguincoders.doit.retrofit;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class myResponse {
    @SerializedName("basket")
    @Expose
    private List<Product> basket;

    public myResponse(List<Product> basket) {
        this.basket = basket;
    }

    public List<Product> getBasket() {
        return basket;
    }

    public void setBasket(List<Product> basket) {
        this.basket = basket;
    }
}
