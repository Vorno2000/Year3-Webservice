/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DeezDonutz;

import java.io.Serializable;

public class ProductBean implements Serializable{
    private String productid;
    private String productName;
    private String cost;
    
    public ProductBean() {
        productid = null;
        productName = null;
        cost = null;
    }
    
    public void setProductid(String Productid) {
        this.productid = Productid;
    }
    public String getProductid() {
        return this.productid;
    }
    public void setProductName(String productName) {
        this.productName = productName;
    }
    public String getProductName() {
        return this.productName;
    }
    public void setCost(String cost) {
        this.cost = cost;
    }
    public String getCost() {
        return this.cost;
    }
}
