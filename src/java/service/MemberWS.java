/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package service;

import Entity.Member;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import static service.MemberentityFacadeREST.generatePasswordHash;
import static service.MemberentityFacadeREST.generatePasswordSalt;

/**
 * REST Web Service
 *
 * @author Matthew Wang
 */

@Path("memberws")
public class MemberWS {

  @Context
  private UriInfo context;

  /**
   * Creates a new instance of MemberWS
   */
  public MemberWS() {
  }

  @GET
  @Path("getMemberProfile")
  @Produces({"application/json"})
  public Response getMemberProfile(@QueryParam("email") String email) {
    try {
      Class.forName("com.mysql.jdbc.Driver");
      String connURL = "jdbc:mysql://localhost:3306/islandfurniture-it07?user=root&password=12345";
      Connection conn = DriverManager.getConnection(connURL);

      String sqlStr = "SELECT * FROM memberentity WHERE email = ?";
      PreparedStatement pstmt = conn.prepareStatement(sqlStr);
      pstmt.setString(1, email);
      ResultSet rs = pstmt.executeQuery();

      Member member = new Member();
      if (rs.next()) {
        //retrieve data from resultset (memberentity table)
        Long id = rs.getLong("id");
        String name = rs.getString("name");
        String emailAddress = rs.getString("email");
        String phoneNo = rs.getString("phone");
        String country = rs.getString("city");
        String address = rs.getString("address");
        int securityQn = rs.getInt("securityquestion");
        String securityAns = rs.getString("securityanswer");
        int age = rs.getInt("age");
        int income = rs.getInt("income");
        int loyaltyPoints = rs.getInt("loyaltypoints");
        double cumulativeSpending = rs.getDouble("cumulativespending");

        //Set values for member fields
        member.setId(id);
        member.setName(name);
        member.setEmail(emailAddress);
        member.setPhone(phoneNo);
        member.setCity(country);
        member.setAddress(address);
        member.setSecurityQuestion(securityQn);
        member.setSecurityAnswer(securityAns);
        member.setAge(age);
        member.setIncome(income);
        member.setLoyaltyPoints(loyaltyPoints);
        member.setCumulativeSpending(cumulativeSpending);
      }
      GenericEntity<Member> myEnt = new GenericEntity<Member>(member) {
      };

      rs.close();
      pstmt.close();
      conn.close();

      return Response
              .status(200)
              .entity(myEnt)
              .build();

    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
      return Response.status(Response.Status.NOT_FOUND).build();
    }
  }

  //this function is used by ECommerce_MemberEditProfileServlet
  @PUT
  @Path("updateMemberProfile")
  @Consumes({"application/json"})
  public Response updateMemberProfile(
          @QueryParam("name") String name, @QueryParam("phone") String phoneNo,
          @QueryParam("city") String city, @QueryParam("address") String address,
          @QueryParam("securityQn") int securityQn, @QueryParam("securityAns") String securityAns,
          @QueryParam("age") int age, @QueryParam("income") int income,
          @QueryParam("password") String password, @QueryParam("email") String email) {
    try {
      Class.forName("com.mysql.jdbc.Driver");
      String connURL = "jdbc:mysql://localhost:3306/islandfurniture-it07?user=root&password=12345";
      Connection conn = DriverManager.getConnection(connURL);

      String sqlStr = "UPDATE memberentity SET name = ?, phone = ?, city = ?, address = ?, "
              + "securityquestion = ?, securityanswer = ?, age = ?, income = ?";

      //Check if user entered a new password
      if (!password.equals("")) {
        sqlStr += ", passwordhash = ?, passwordsalt = ?";
      }
      sqlStr += " WHERE email = ?";

      PreparedStatement pstmt = conn.prepareStatement(sqlStr);
      pstmt.setString(1, name);
      pstmt.setString(2, phoneNo);
      pstmt.setString(3, city);
      pstmt.setString(4, address);
      pstmt.setInt(5, securityQn);
      pstmt.setString(6, securityAns);
      pstmt.setInt(7, age);
      pstmt.setInt(8, income);

      if (!password.equals("")) {
        //Generate password salt
        String salt = generatePasswordSalt();
        //Create password hash
        String hash = generatePasswordHash(salt, password);

        pstmt.setString(9, hash);
        pstmt.setString(10, salt);
        pstmt.setString(11, email);
      } else {
        pstmt.setString(9, email);
      }

      int result = pstmt.executeUpdate();

      pstmt.close();
      conn.close();

      if (result > 0) {
        return Response.status(Response.Status.OK).build();
      }

    } catch (ClassNotFoundException | SQLException e) {
      e.printStackTrace();
    }
    return Response.status(Response.Status.NOT_FOUND).build();
  }

  /**
   * PUT method for updating or creating an instance of MemberWS
   * @param content representation for the resource
   */
  @PUT
  @Consumes(MediaType.APPLICATION_JSON)
  public void putJson(String content) {
  }
}
