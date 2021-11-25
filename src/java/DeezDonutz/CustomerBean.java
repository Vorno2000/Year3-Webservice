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
public class CustomerBean implements Serializable {
    private String firstName;
    private String lastName;
    private String email;
    private boolean newCustomer;
    
    public CustomerBean() {
        firstName = null;
        lastName = null;
        email = null;
        newCustomer = false;
    }
    
    public String getFirstName() {
        return this.firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return this.lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getEmail() {
        return this.email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean getNewCustomer() {
        return this.newCustomer;
    }
    
    public void setNewCustomer(boolean isNewCustomer) {
        this.newCustomer = isNewCustomer;
    }
}
