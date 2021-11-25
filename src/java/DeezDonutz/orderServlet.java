/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DeezDonutz;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Vorno
 */
@WebServlet(name = "orderServlet", urlPatterns = {"/orderServlet"}, initParams = {
    @WebInitParam(name = "dbProductsTable", value = "DD_Products"),            
    @WebInitParam(name = "dbCustomersTable", value = "DD_Customers"),
    @WebInitParam(name = "dbEmail", value = "email")
})
public class orderServlet extends HttpServlet {

    private Logger logger;
    private String sqlCommand;
    
    @Resource(mappedName = "jdbc/MySQLDeezDonutzResource")
    private DataSource dataSource;
    
    public orderServlet() {
        logger = Logger.getLogger(getClass().getName());
    }
    
    public void init() {
        initGetProducts();
    }
    
    public String initGetProducts() {
        ServletConfig config = getServletConfig();
        String dbProductsTable = config.getInitParameter("dbProductsTable");
        
        sqlCommand = "SELECT * FROM " + dbProductsTable;
        
        return sqlCommand;
    }
    
    public String initCustomerSearch() {
        ServletConfig config = getServletConfig();
        String dbCustomerTable = config.getInitParameter("dbCustomersTable");
        String dbEmail = config.getInitParameter("dbEmail");
        
        sqlCommand = "SELECT * FROM " + dbCustomerTable + " WHERE "+dbEmail+" = ?";
        return sqlCommand;
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        RequestedBean messageBean = new RequestedBean();
        messageBean.setRequestedBean("");
        String INPUTproductName = request.getParameter("productName");
        //check that the inputted product name is legit, then put in bean and send to finalize servlet
        if(INPUTproductName != null && INPUTproductName.length() > 2 && INPUTproductName.length() < 20) {
            RequestedBean requestedBean = new RequestedBean();
            requestedBean.setRequestedBean(INPUTproductName);
            
            request.setAttribute("RequestedProductName", requestedBean);
            
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/finalizeOrderServlet");     
            dispatcher.forward(request, response);
        } //error handling
        else if(INPUTproductName != null){
            messageBean.setRequestedBean("Not a Valid Product Name");
            request.setAttribute("MessageBean", messageBean);

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/makeOrder.jsp");    
            dispatcher.forward(request, response);
        }
        
        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        //query for product table information
        if(sqlCommand != null && dataSource != null) {
            sqlCommand = initGetProducts();
            try {
                conn = dataSource.getConnection();
                prepStmt = conn.prepareStatement(sqlCommand);
                resultSet = prepStmt.executeQuery();
                logger.info("Successfully executed query for DD_Products or DD_Customers table");
            }catch (SQLException e) {
                logger.severe("Could not query for the Products information: "+e);
            }
        }
        //save product table information into a bean to be displayed in the jsp
        if(resultSet != null) {
            ArrayList<ProductBean> tableBeanArr = new ArrayList<>();
            try {
                while(resultSet.next()) {
                    String ProductID = resultSet.getString("Product_ID");
                    String productName = resultSet.getString("productName");
                    String cost = resultSet.getString("cost");
                    ProductBean tableBean = new ProductBean();
                    
                    tableBean.setProductid(ProductID);
                    tableBean.setProductName(productName);
                    tableBean.setCost(cost);
                    
                    tableBeanArr.add(tableBean);
                }
                
                HttpSession session = request.getSession();
                session.setAttribute("ProductsTable", tableBeanArr);
            } catch(SQLException e) {
                logger.severe("Exception in result set for products: "+e);
            }
        }
        
        //////////////////////////////////////////////
        
        String INPUTfirstName = request.getParameter("firstName");
        String INPUTlastName = request.getParameter("lastName");
        String INPUTemail = request.getParameter("email");
        
        boolean validInput = true;
        //check if the customers entered information is legit
        if(INPUTfirstName == null || INPUTfirstName.length() < 2 && INPUTfirstName.length() > 20) {
            validInput = false;
        }
        if(INPUTlastName == null || INPUTlastName.length() < 2 && INPUTlastName.length() > 20) {
            validInput = false;
        }
        if(INPUTemail == null || INPUTemail.length() < 6 && INPUTemail.length() > 40) {
            validInput = false;
        }
        //error handling for email input
        else if(INPUTemail.length() < 7) {
            messageBean.setRequestedBean("Not Enough Characters (more than 6)");
            HttpSession session = request.getSession();
            session.setAttribute("MessageBean", messageBean);
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/beginOrder.jsp");  
            dispatcher.forward(request, response);
        }
        else if(INPUTemail.length() > 40) {
            RequestedBean message = new RequestedBean();
            message.setRequestedBean("Too Many Characters (less than 40)");
            HttpSession session = request.getSession();
            session.setAttribute("MessageBean", message);
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/beginOrder.jsp");  
            dispatcher.forward(request, response);
        }
        //if the input was valid, put all entered information into a bean
        if(validInput) {
            CustomerBean customerBean = new CustomerBean();
            customerBean.setFirstName(INPUTfirstName);
            customerBean.setLastName(INPUTlastName);
            customerBean.setEmail(INPUTemail);
            customerBean.setNewCustomer(true);
            
            HttpSession session = request.getSession();
            session.setAttribute("CustomerBean", customerBean);

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/makeOrder.jsp");  
            dispatcher.forward(request, response);
        }
        //check to see if a customer bean has been created and go to make order jsp if it has
        try {
            HttpSession tempSession = request.getSession();
            CustomerBean tempCustomerBean = (CustomerBean) tempSession.getAttribute("CustomerBean");
            if(tempCustomerBean != null && INPUTemail != null) {
                if(INPUTemail.equals(tempCustomerBean.getEmail())) {
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/makeOrder.jsp");    
                    dispatcher.forward(request, response);
                }
            }
        }catch(NullPointerException e) {
        }
        
        sqlCommand = initCustomerSearch();
        
        conn = null;
        prepStmt = null;
        resultSet = null;
        //query database to check if entered email exists in it
        if(sqlCommand != null && dataSource != null) {
            try {
                conn = dataSource.getConnection();
                prepStmt = conn.prepareStatement(sqlCommand);
                prepStmt.setString(1, INPUTemail);
                resultSet = prepStmt.executeQuery();
                logger.info("Successfully executed query for DD_Customers table");
            } catch(SQLException e) {
                logger.severe("Unable to execute query for DD_Products table or DD_Customers table: "+e);
            }
        }
        try {
            if(resultSet != null && resultSet.next()) { //Customer has purchased before
                try {
                    resultSet.beforeFirst();
                    resultSet.next();
                    String firstName = resultSet.getString("firstName");
                    String lastName = resultSet.getString("lastName");
                    String email = resultSet.getString("email");

                    CustomerBean customerBean = new CustomerBean();
                    customerBean.setFirstName(firstName);
                    customerBean.setLastName(lastName);
                    customerBean.setEmail(email);

                    HttpSession session = request.getSession();
                    session.setAttribute("CustomerBean", customerBean);

                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/makeOrder.jsp");
                    dispatcher.forward(request, response);
                } catch (SQLException e) {
                    logger.severe("Unable to retrieve attributes from resultSet: "+e);
                }
            }
            else { //if customer information is not in the DB then pass email to be autofilled
                RequestedBean emailBean = new RequestedBean();
                emailBean.setRequestedBean(INPUTemail);
                request.setAttribute("EmailBean", emailBean);
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/CustomerDetails.jsp");
                dispatcher.forward(request, response);
            }
            
        } catch(SQLException e) {
            logger.severe("No data in Result Set: "+e);
        }
        try {
            if(prepStmt != null)
                prepStmt.close();
            if(conn != null)
                conn.close();
        } catch(SQLException e) {
        }
    }
    
    public static String filter(String text) {
        StringBuilder buffer = new StringBuilder();
        for(int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
         if (c == '<')
            buffer.append("&lt;");
         else if (c == '>')
            buffer.append("&gt;");
         else if (c == '\"')
            buffer.append("&quot;");
         else if (c == '\'')
            buffer.append("&#39;");
         else if (c == '&')
            buffer.append("&amp;");
         else
            buffer.append(c);
      }
      return buffer.toString();
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
