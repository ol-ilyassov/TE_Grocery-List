package net.penguincoders.doit;

import java.io.Serializable;
import java.util.List;

public class Basket implements Serializable {
    public List<Product> productList;

    public Basket(List<Product> list) {
        this.productList = list;
    }

    public List<Product> getProductList() {
        return productList;
    }

    public void setProductList(List<Product> list) {
        this.productList = list;
    }
}
