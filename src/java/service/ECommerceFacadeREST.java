package service;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("commerce")
public class ECommerceFacadeREST {

    @Context
    private UriInfo context;

    public ECommerceFacadeREST() {
    }
    
    @GET
    @Produces("application/json")
    public String getJson() {
        //TODO return proper representation object
        throw new UnsupportedOperationException();
    }

    /**
     * PUT method for updating or creating an instance of ECommerce
     *
     * @param content representation for the resource
     * @return an HTTP response with content of the updated or created resource.
     */
    @PUT
    @Consumes("application/json")
    public void putJson(String content) {
    }
    
    @PUT
    @Path("createEcommerceTransactionRecord")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createECommerceTransactionRecord(@QueryParam("memberID") long memberID, 
            @QueryParam("countryID") long countryID, @QueryParam("amountPaid") double amountPaid) {
      try {
        
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/islandfurniture-it07?user=root&password=12345");
        String sqlStr = "INSERT INTO salesrecordentity (AMOUNTDUE, AMOUNTPAID, AMOUNTPAIDUSINGPOINTS, "
                + "CREATEDDATE, CURRENCY, LOYALTYPOINTSDEDUCTED, POSNAME, "
                + "RECEIPTNO, SERVEDBYSTAFF, MEMBER_ID, STORE_ID) "
                +  "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        PreparedStatement pstmt = conn.prepareStatement(sqlStr, PreparedStatement.RETURN_GENERATED_KEYS);
        pstmt.setDouble(1, amountPaid);
        pstmt.setDouble(2, amountPaid);
        pstmt.setDouble(3, 0);
        pstmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
        pstmt.setString(5, "SGD");
        pstmt.setInt(6, 0);
        pstmt.setString(7, "Counter 1");
        pstmt.setString(8, String.valueOf(System.currentTimeMillis()));
        pstmt.setString(9, "Cashier 1");
        pstmt.setLong(10, memberID);
        pstmt.setLong(11, 59);
        
        pstmt.executeUpdate();
        
        long salesRecordID = 0;
        ResultSet rs = pstmt.getGeneratedKeys();
        if (rs.next()) {
          salesRecordID = rs.getLong(1);
          return Response.status(Response.Status.CREATED).entity(Long.toString(salesRecordID)).build();
        } else {
          return Response.status(Response.Status.NOT_FOUND).build();
        }
      } catch (Exception ex) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }
    
    @PUT
    @Path("createECommerceLineItemRecord")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createECommerceLineItemRecord(@QueryParam("salesRecordID") long salesRecordID, @QueryParam("itemID") long itemID, 
            @QueryParam("quantity") int quantity, @QueryParam("countryID") long countryID) {
      try {
         Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/islandfurniture-it07?user=root&password=12345");
         
         String sqlStr = "INSERT INTO lineitementity(QUANTITY, ITEM_ID) VALUES (?, ?)";
         PreparedStatement pstmt = conn.prepareStatement(sqlStr, PreparedStatement.RETURN_GENERATED_KEYS);
         pstmt.setInt(1, quantity);
         pstmt.setLong(2, itemID);
         pstmt.executeUpdate();
         
         long lineItemID = 0;
         ResultSet rs = pstmt.getGeneratedKeys();
         if (rs.next()) {
           System.out.println("First sql cleared!");
           lineItemID = rs.getLong(1);
           sqlStr = "INSERT INTO salesrecordentity_lineitementity VALUES (?, ?)";
           
           pstmt = conn.prepareStatement(sqlStr);
           pstmt.setLong(1, salesRecordID);
           pstmt.setLong(2, lineItemID);
           pstmt.executeUpdate();
           
           //update quantity of item
           //retrieve the warehouse id based on country id
           sqlStr = "SELECT WarehouseEntity_ID FROM country_ecommerce WHERE CountryEntity_ID = ?";
           pstmt = conn.prepareStatement(sqlStr);
           pstmt.setLong(1, countryID);
           rs = pstmt.executeQuery();
           
           Long warehouseID = 0L;
           if(rs.next()) {
             System.out.println("Second sql cleared!");
             warehouseID = rs.getLong(1);
           }
           
           //retrieve lineitem id and quantities based on item id
           sqlStr = "SELECT l.ID, l.QUANTITY, s.ID AS storagebinID, i.VOLUME FROM warehouseentity w, storagebinentity s, "
                   + "storagebinentity_lineitementity sl, lineitementity l, itementity i WHERE i.ID = l.ITEM_ID and "
                   + "sl.lineItems_ID = l.ID and s.ID = sl.StorageBinEntity_ID and w.ID = s.WAREHOUSE_ID and "
                   + "w.ID = ? and l.ITEM_ID = ?";
           pstmt = conn.prepareStatement(sqlStr);
           pstmt.setLong(1, warehouseID);
           pstmt.setLong(2, itemID);
           rs = pstmt.executeQuery();
           
           while (rs.next()) {
             System.out.println("Third sql cleared!");
             lineItemID = rs.getLong(1);
             int remainingQty = rs.getInt(2);
             Long storageBinID = rs.getLong(3);
             int itemVolume = rs.getInt(4);
             
             if (quantity <= 0) {
               break;
             }
             
             if (remainingQty - quantity >= 0) {
               System.out.println("RemainingQty >= 0");
               String updateSqlStr = "UPDATE lineitementity SET QUANTITY = QUANTITY - ? WHERE ID = ?";
               pstmt = conn.prepareStatement(updateSqlStr);
               pstmt.setLong(1, quantity);
               pstmt.setLong(2, lineItemID);
               pstmt.executeUpdate();
               System.out.println("4 sql cleared!");
               break;
             } else {
               System.out.println("RemainingQty < 0");
               quantity -= remainingQty;
               String updateSqlStr = "UPDATE lineitementity SET QUANTITY = 0 WHERE ID = ?";
               pstmt = conn.prepareStatement(updateSqlStr);
               pstmt.setLong(1, lineItemID);
               pstmt.executeUpdate();
               System.out.println("5.1 sql cleared!");
               updateSqlStr = "UPDATE storagebinentity SET FREEVOLUME = VOLUME WHERE ID = ?";
               pstmt = conn.prepareStatement(updateSqlStr);
               pstmt.setLong(1, storageBinID);
               pstmt.executeUpdate();
               System.out.println("5.2 sql cleared!");
             }
           }
           
           return Response.status(Response.Status.CREATED).build();
         } else {
           return Response.status(Response.Status.NOT_FOUND).build();
         }
      } catch (Exception ex) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    }
    
    
}
