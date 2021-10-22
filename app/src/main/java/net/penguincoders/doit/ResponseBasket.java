package net.penguincoders.doit;

import java.io.Serializable;

public class ResponseBasket implements Serializable {
    public Basket basket;

    public ResponseBasket(Basket basket) {
        this.basket = basket;
    }

    public Basket getBasket() {
        return basket;
    }

    public void setBasket(Basket basket) {
        this.basket = basket;
    }
}
