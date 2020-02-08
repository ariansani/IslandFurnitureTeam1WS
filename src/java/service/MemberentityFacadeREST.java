package service;

import Entity.Itementity;
import Entity.Lineitementity;
import Entity.Member;
import Entity.Memberentity;
import Entity.Qrphonesyncentity;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Stateless
@Path("entity.memberentity")
public class MemberentityFacadeREST extends AbstractFacade<Memberentity> {

  @PersistenceContext(unitName = "WebService")
  private EntityManager em;

  public MemberentityFacadeREST() {
    super(Memberentity.class);
  }

  @POST
  @Override
  @Consumes({"application/xml", "application/json"})
  public void create(Memberentity entity) {
    super.create(entity);
  }

  @PUT
  @Path("{id}")
  @Consumes({"application/xml", "application/json"})
  public void edit(@PathParam("id") Long id, Memberentity entity) {
    super.edit(entity);
  }

  @DELETE
  @Path("{id}")
  public void remove(@PathParam("id") Long id) {
    super.remove(super.find(id));
  }

  @GET
  @Path("members")
  @Produces({"application/json"})
  public List<Memberentity> listAllMembers() {
    Query q = em.createQuery("Select s from Memberentity s where s.isdeleted=FALSE");
    List<Memberentity> list = q.getResultList();
    for (Memberentity m : list) {
      em.detach(m);
      m.setCountryId(null);
      m.setLoyaltytierId(null);
      m.setLineitementityList(null);
      m.setWishlistId(null);
    }
    List<Memberentity> list2 = new ArrayList();
    list2.add(list.get(0));
    return list;
  }

  //this function is used by ECommerce_MemberLoginServlet
  @GET
  @Path("login")
  @Produces("application/json")
  public Response loginMember(@QueryParam("email") String email, @QueryParam("password") String password) {
    try {
      Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/islandfurniture-it07?zeroDateTimeBehavior=convertToNull&user=root&password=12345");
      String stmt = "SELECT * FROM memberentity m WHERE m.EMAIL=?";
      PreparedStatement ps = conn.prepareStatement(stmt);
      ps.setString(1, email);
      ResultSet rs = ps.executeQuery();
      rs.next();
      String passwordSalt = rs.getString("PASSWORDSALT");
      String passwordHash = generatePasswordHash(passwordSalt, password);
      if (passwordHash.equals(rs.getString("PASSWORDHASH"))) {
        return Response.ok(email, MediaType.APPLICATION_JSON).build();
      } else {
        System.out.println("Login credentials provided were incorrect, password wrong.");
        return Response.status(Response.Status.UNAUTHORIZED).build();
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
  }

  public String generatePasswordSalt() {
    byte[] salt = new byte[16];
    try {
      SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
      sr.nextBytes(salt);
    } catch (NoSuchAlgorithmException ex) {
      System.out.println("\nServer failed to generate password salt.\n" + ex);
    }
    return Arrays.toString(salt);
  }

  public String generatePasswordHash(String salt, String password) {
    String passwordHash = null;
    try {
      password = salt + password;
      MessageDigest md = MessageDigest.getInstance("MD5");
      md.update(password.getBytes());
      byte[] bytes = md.digest();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < bytes.length; i++) {
        sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
      }
      passwordHash = sb.toString();
    } catch (NoSuchAlgorithmException ex) {
      System.out.println("\nServer failed to hash password.\n" + ex);
    }
    return passwordHash;
  }

  
  @GET
  @Path("getMember")
  @Produces({"application/json"})
  public Response getMember(@QueryParam("email") String email) {
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
  @Path("editMember")
  @Consumes({"application/json"})
  public Response editMember(
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
  

  @GET
  @Path("uploadShoppingList")
  @Produces({"application/json"})
  public String uploadShoppingList(@QueryParam("email") String email, @QueryParam("shoppingList") String shoppingList) {
    System.out.println("webservice: uploadShoppingList called");
    System.out.println(shoppingList);
    try {
      Query q = em.createQuery("select m from Memberentity m where m.email=:email and m.isdeleted=false");
      q.setParameter("email", email);
      Memberentity m = (Memberentity) q.getSingleResult();
      List<Lineitementity> list = m.getLineitementityList();
      if (!list.isEmpty()) {
        for (Lineitementity lineItem : list) {
          em.refresh(lineItem);
          em.flush();
          em.remove(lineItem);
        }
      }
      m.setLineitementityList(new ArrayList<Lineitementity>());
      em.flush();

      Scanner sc = new Scanner(shoppingList);
      sc.useDelimiter(",");
      while (sc.hasNext()) {
        String SKU = sc.next();
        Integer quantity = Integer.parseInt(sc.next());
        if (quantity != 0) {
          q = em.createQuery("select i from Itementity i where i.sku=:SKU and i.isdeleted=false");
          q.setParameter("SKU", SKU);
          Itementity item = (Itementity) q.getSingleResult();

          Lineitementity lineItem = new Lineitementity();

          lineItem.setItemId(item);
          lineItem.setQuantity(quantity);
          System.out.println("Item: " + item.getSku());
          System.out.println("Quantity: " + quantity);
          m.getLineitementityList().add(lineItem);
        }
      }
      return "success";
      //return s;
    } catch (Exception e) {
      e.printStackTrace();
      return "fail";
    }
  }

  @GET
  @Path("syncWithPOS")
  @Produces({"application/json"})
  public String tieMemberToSyncRequest(@QueryParam("email") String email, @QueryParam("qrCode") String qrCode) {
    System.out.println("tieMemberToSyncRequest() called");
    try {
      Query q = em.createQuery("SELECT p from Qrphonesyncentity p where p.qrcode=:qrCode");
      q.setParameter("qrCode", qrCode);
      Qrphonesyncentity phoneSyncEntity = (Qrphonesyncentity) q.getSingleResult();
      if (phoneSyncEntity == null) {
        return "fail";
      } else {
        phoneSyncEntity.setMemberemail(email);
        em.merge(phoneSyncEntity);
        em.flush();
        return "success";
      }
    } catch (Exception ex) {
      System.out.println("tieMemberToSyncRequest(): Error");
      ex.printStackTrace();
      return "fail";
    }
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

}
