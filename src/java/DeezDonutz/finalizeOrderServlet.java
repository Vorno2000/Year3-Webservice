/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DeezDonutz;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

/**
 *
 * @author Vorno
 */
@WebServlet(name = "finalizeOrderServlet", urlPatterns = {"/finalizeOrderServlet"}, initParams = {
    @WebInitParam(name = "dbProductsTable", value = "DD_Products"),
    @WebInitParam(name = "dbOrdersTable", value = "DD_Orders"),
    @WebInitParam(name = "dbCustomersTable", value = "DD_Customers"),
    @WebInitParam(name = "dbCustomerID", value = "Customer_ID"),
    @WebInitParam(name = "dbProductName", value = "productName"),
    @WebInitParam(name = "dbProductID", value = "Product_ID"),
    @WebInitParam(name = "dbDate", value = "date"),
    @WebInitParam(name = "dbAddress", value = "address"),
    @WebInitParam(name = "dbFirstName", value = "firstName"),
    @WebInitParam(name = "dbLastName", value = "lastName"),
    @WebInitParam(name = "dbEmail", value = "email"),
})
public class finalizeOrderServlet extends HttpServlet {

    private Logger logger;
    private String sqlCommand;
    
    @Resource(mappedName = "jdbc/MySQLDeezDonutzResource")
    private DataSource dataSource;
    
    public finalizeOrderServlet() {
        logger = Logger.getLogger(getClass().getName());
    }
    
    public void init() {
        initProductSearch();
    }
    
    public String initProductSearch() {
        ServletConfig config = getServletConfig();
        String dbProductsTable = config.getInitParameter("dbProductsTable");
        String dbProductName = config.getInitParameter("dbProductName");
        
        sqlCommand = "SELECT * FROM " + dbProductsTable + " WHERE " + dbProductName + " = ?";
        return sqlCommand;
    }
    
    public String initUpdateOrder() {
        ServletConfig config = getServletConfig();
        String dbOrdersTable = config.getInitParameter("dbOrdersTable");
        String dbCustomerID = config.getInitParameter("dbCustomerID");
        String dbProductID = config.getInitParameter("dbProductID");
        String dbDate = config.getInitParameter("dbDate");
        String dbAddress = config.getInitParameter("dbAddress");
        
        sqlCommand = "INSERT INTO "+dbOrdersTable+"("+dbCustomerID+", "+dbProductID+", "+dbDate+", "+dbAddress+") VALUES (?, ?, ?, ?)";
        
        return sqlCommand;
    }
    
    public String initUpdateCustomer() {
        ServletConfig config = getServletConfig();
        String dbCustomersTable = config.getInitParameter("dbCustomersTable");
        String dbFirstName = config.getInitParameter("dbFirstName");
        String dbLastName = config.getInitParameter("dbLastName");
        String dbEmail = config.getInitParameter("dbEmail");
        
        sqlCommand = "INSERT INTO "+dbCustomersTable+"("+dbFirstName+", "+dbLastName+", "+dbEmail+") VALUES (?, ?, ?)";
        
        return sqlCommand;
    }
    
    public String initGetCustomerID() {
        ServletConfig config = getServletConfig();
        String dbCustomersTable = config.getInitParameter("dbCustomersTable");
        String dbEmail = config.getInitParameter("dbEmail");
        
        sqlCommand = "SELECT * FROM "+dbCustomersTable+" WHERE "+dbEmail+" = ?";
        
        return sqlCommand;
    }
    
    public String initGetAddress() {
        ServletConfig config = getServletConfig();
        String dbOrdersTable = config.getInitParameter("dbOrdersTable");
        String dbCustomersTable = config.getInitParameter("dbCustomersTable");
        String dbCustomerID = config.getInitParameter("dbCustomerID");
        String dbEmail = config.getInitParameter("dbEmail");
        
        sqlCommand = "SELECT * FROM "+dbOrdersTable+", "+dbCustomersTable+
                " WHERE "+dbOrdersTable+"."+dbCustomerID+" = "+dbCustomersTable+"."+dbCustomerID+
                        " AND "+dbEmail+" = ?";
        return sqlCommand;
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        boolean placeOrder = false;
        String INPUTproductName = null;
        RequestedBean messageBean = new RequestedBean();
        messageBean.setRequestedBean("");
        
        HttpSession session = request.getSession();
        CustomerBean CustomerInfo = (CustomerBean) session.getAttribute("CustomerBean");
        ProductBean ProductInfo = (ProductBean) session.getAttribute("ProductBean");
        //check if a product name has been entered so that it knows whether to place the order yet
        try {
            RequestedBean requestedBean = (RequestedBean) request.getAttribute("RequestedProductName");
            INPUTproductName = requestedBean.getRequestedBean();
        }catch(NullPointerException e) {
            placeOrder = true;
        }
        String INPUTaddress = request.getParameter("address");
        
        if(INPUTaddress == null) {
            placeOrder = false;
        }
        
        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        //this uses all the entered data and the customers beans to place the order and update the orders database table
        if(placeOrder) {
            Date todaysDate = new Date();
            todaysDate = Calendar.getInstance().getTime();
            
            java.sql.Date sqlDate = new java.sql.Date(todaysDate.getTime());
            //check if its a new customer and if it is, add customer too the customers table
            if(CustomerInfo.getNewCustomer()) {
                sqlCommand = initUpdateCustomer();
                try {
                    conn = dataSource.getConnection();
                    prepStmt = conn.prepareStatement(sqlCommand);
                    prepStmt.setString(1, CustomerInfo.getFirstName());
                    prepStmt.setString(2, CustomerInfo.getLastName());
                    prepStmt.setString(3, CustomerInfo.getEmail());
                    
                    prepStmt.executeUpdate();
                }catch(SQLException e) {
                    logger.severe("Unable to update Customer information: "+e);
                }    
            }
            int Customer_ID = 0;
            //get the customers ID
            try {
                sqlCommand = initGetCustomerID();
                
                conn = dataSource.getConnection();
                prepStmt = conn.prepareStatement(sqlCommand);
                prepStmt.setString(1, CustomerInfo.getEmail());
                resultSet = prepStmt.executeQuery();
                resultSet.next();
                
                Customer_ID = resultSet.getInt("Customer_ID");
            }catch (SQLException e) {
                logger.severe("Unable to retrieve customers ID: "+e);
            }
            //place the order in the order table
            try {
                if(Customer_ID != 0) {
                    sqlCommand = initUpdateOrder();

                    conn = dataSource.getConnection();
                    prepStmt = conn.prepareStatement(sqlCommand);
                    prepStmt.setInt(1, Customer_ID);
                    prepStmt.setInt(2, Integer.parseInt(ProductInfo.getProductid()));
                    prepStmt.setDate(3, sqlDate);
                    prepStmt.setString(4, INPUTaddress);
                    
                    prepStmt.executeUpdate();
                    
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/completeOrder.jsp");    
                    dispatcher.forward(request, response);
                    
                }
            }catch(SQLException e) {
                logger.severe("Unable to create order: "+e);
            }
        }
        
        boolean newCustomer = CustomerInfo.getNewCustomer();
        //if its not a new customer, get the address and put into bean to be autofilled
        if(!newCustomer) {
            sqlCommand = initGetAddress();
            try {
                conn = dataSource.getConnection();
                prepStmt = conn.prepareStatement(sqlCommand);
                prepStmt.setString(1, CustomerInfo.getEmail());
                
                resultSet = prepStmt.executeQuery();
                resultSet.last();
                String address = resultSet.getString("address");
                
                RequestedBean addressBean = new RequestedBean();
                addressBean.setRequestedBean(address);
                
                request.setAttribute("AddressBean", addressBean);
            }catch(SQLException e) {
                logger.severe("Unable to find Address for Customer: "+e);
            }
        }
        //get the product information for the entered productname
        if(sqlCommand != null && dataSource != null) {
            try {
                sqlCommand = initProductSearch();
                conn = dataSource.getConnection();
                prepStmt = conn.prepareStatement(sqlCommand);
                prepStmt.setString(1, INPUTproductName);
                resultSet = prepStmt.executeQuery();
                logger.info("Successfully executed query for DD_Products table");
            }catch (SQLException e) {
                logger.severe("Could not query for the Products information: "+e);
            }
        }
        //put the products information in a bean
        try {
            if(resultSet != null && resultSet.next()) {
                resultSet.beforeFirst();
                resultSet.next();
                
                String ProductId = resultSet.getString("Product_ID");
                String productName = resultSet.getString("productName");
                String cost = resultSet.getString("cost");
                
                ProductBean productBean = new ProductBean();
                productBean.setProductid(ProductId);
                productBean.setProductName(productName);
                productBean.setCost(cost);
                
                session.setAttribute("ProductBean", productBean);
                
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/finalizeOrder.jsp");    
                dispatcher.forward(request, response);
            }
        }catch(SQLException e) {
            logger.severe("No data in Result Set: "+e);
        }
        //if not place order, then there was a mistake with the entry so error handle
        if(!placeOrder) {
            messageBean.setRequestedBean("No Existing Product With That Name");
            request.setAttribute("MessageBean", messageBean);

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/makeOrder.jsp");    
            dispatcher.forward(request, response);
        }
        
        
        try {
            if(prepStmt != null)
                prepStmt.close();
            if(conn != null)
                conn.close();
        } catch(SQLException e) {
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
