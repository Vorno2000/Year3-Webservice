/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DeezDonutz;

import java.io.Serializable;

/**
 *
 * @author Vorno
 */
public class RequestedBean implements Serializable{
    private String requestedBean;
    
    public RequestedBean() {
        requestedBean = null;
    }
    
    public void setRequestedBean(String productName) {
        this.requestedBean = productName;
    }
    
    public String getRequestedBean() {
        return this.requestedBean;
    }
}
