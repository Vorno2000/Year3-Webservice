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
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Vorno
 */
@WebServlet(name = "catalogServlet", urlPatterns = {"/catalogServlet"}, initParams = {
    @WebInitParam(name = "dbTable", value = "DD_Products"),            
    @WebInitParam(name = "dbProductIDAtt", value = "Product_ID"),
    @WebInitParam(name = "dbProductNameAtt", value = "productName"),
    @WebInitParam(name = "dbCost", value = "cost")
})
public class catalogServlet extends HttpServlet {

    private final char QUOTE = '"';
    private Logger logger;
    private String sqlCommand;
    
    @Resource(mappedName = "jdbc/MySQLDeezDonutzResource")
    private DataSource dataSource;
    
    public catalogServlet() {
        logger = Logger.getLogger(getClass().getName());
    }
    
    public void init() {
        initUpdate();
    }
    
    public String initUpdate() {
        ServletConfig config = getServletConfig();
        String dbTable = config.getInitParameter("dbTable");
        
        sqlCommand = "SELECT * FROM " + dbTable;
        
        return sqlCommand;
    }
    
    public String initInsert() {
        ServletConfig config = getServletConfig();
        String dbTable = config.getInitParameter("dbTable");
        String dbProductNameAtt = config.getInitParameter("dbProductNameAtt");
        String dbCost = config.getInitParameter("dbCost");
        
        sqlCommand = "INSERT INTO " + dbTable +"("+dbProductNameAtt+", "+dbCost+") VALUES (?, ?)";
        
        return sqlCommand;
    }
    
    public String initDelete() {
        ServletConfig config = getServletConfig();
        String dbTable = config.getInitParameter("dbTable");
        String dbProductIDAtt = config.getInitParameter("dbProductIDAtt");
        
        sqlCommand = "DELETE FROM "+dbTable+" WHERE "+dbProductIDAtt+" = ?";
        
        return sqlCommand;
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        String INPUTProduct_ID = request.getParameter("Product_ID");
        String INPUTproductName = request.getParameter("productName");
        String INPUTcost = request.getParameter("cost");
        
        boolean insertValid = true;
        boolean attemptedInsert = false;
        boolean deleteValid = true;
        boolean attemptedDelete = false;
        //check to see if the user tried to insert data and if its a valid input
        if(INPUTcost != null && INPUTproductName != null) {
            try {
                float temp = Float.parseFloat(INPUTcost);
            } catch(NullPointerException | NumberFormatException e) {
                attemptedInsert = true;
                insertValid = false;
            }
        } else
            insertValid = false;
        //check to see if the user attempted to delete and if its a valid input
        if(INPUTProduct_ID != null) {
            try {
                int temp = Integer.parseInt(INPUTProduct_ID);
            } catch(NullPointerException | NumberFormatException e) {
                attemptedDelete = true;
                deleteValid = false;
            }
        } else
            deleteValid = false;
        
        if(insertValid) 
            sqlCommand = initInsert();
        else if(deleteValid) 
            sqlCommand = initDelete();
        else
            sqlCommand = initUpdate();
        
        Connection conn = null;
        PreparedStatement prepStmt = null;
        ResultSet resultSet = null;
        //query to insert a new product name and its cost
        if(sqlCommand != null && dataSource != null) {
            try {
                conn = dataSource.getConnection();
                prepStmt = conn.prepareStatement(sqlCommand);
                if(insertValid) {
                    prepStmt.setString(1, INPUTproductName);
                    prepStmt.setString(2, INPUTcost);
                    prepStmt.executeUpdate();
                }
                else if(deleteValid) {
                    prepStmt.setString(1, INPUTProduct_ID);
                    prepStmt.executeUpdate();
                }
                else
                    resultSet = prepStmt.executeQuery();
                logger.info("Successfully executed query for DD_Products table");
            } catch(SQLException e) {
                logger.severe("Unable to execute query for DD_Products table: "+e);
            }
        }
        //read the html and print it back out with the correct products table
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            File catalog = new File("D:\\Program Files\\NetBeans 8.2\\Projects\\DeezDonutz\\web\\catalog.html");
            String htmlString = FileUtils.readFileToString(catalog, Charset.forName("UTF-8"));
            
            String table = "";
            
            if(resultSet != null) {
                table += "<TABLE class="+QUOTE+"table"+QUOTE+" cellspacing=1 border=5>";
                table += "<TR><TD><B>Product ID</B></TD>"
                        + "<TD><B>Product Name</B></TD>"
                        + "<TD><B>Cost</B></TD></TR>";
                try {
                    while (resultSet.next()) {
                        String Product_ID = resultSet.getString("Product_ID");
                        String productName = resultSet.getString("productName");
                        String cost = resultSet.getString("cost");
                        table += "<TR><TD>" + filter(Product_ID) + "</TD><TD>"
                                + filter(productName) + "</TD><TD>$"
                                + filter(cost) + "</TD></TR>";
                    }
                } catch(SQLException e) {
                    logger.severe("Exception in result set for Products: "+e);
                }
                table += "</TABLE>";
            }
            try {
                if(prepStmt != null)
                    prepStmt.close();
                if(conn != null)
                    conn.close();
            } catch(SQLException e) {
            }
            //error handling for incorrect inputs
            htmlString = htmlString.replace("$table", table);
            if(attemptedInsert) {
                htmlString = htmlString.replace("$insertError", "Format Error");
                htmlString = htmlString.replace("<!--inserterror", "");
                htmlString = htmlString.replace("inserterrorend-->", "");
            }
            if(attemptedDelete) {
                htmlString = htmlString.replace("$deleteError", "Product ID Format Error");
                htmlString = htmlString.replace("<!--deleteerror", "");
                htmlString = htmlString.replace("deleteerrorend-->", "");
            }
            
            out.println(htmlString);
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
