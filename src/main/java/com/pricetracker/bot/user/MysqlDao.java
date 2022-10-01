package com.pricetracker.bot.user;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pricetracker.bot.wrappers.PriceTracker;
import com.pricetracker.bot.wrappers.Product;
import com.pricetracker.bot.wrappers.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MysqlDao {

  private static Logger log = LoggerFactory.getLogger(MysqlDao.class);

  private static final String DB_URL = "jdbc:mysql://localhost:3306/telebot";
  // private static final String DB_URL = "jdbc:mysql://mysql:3306/telebot";
  private static final String DB_USER = "root";
  private static final String DB_PASS = "root";
  private static Connection connection;

  public MysqlDao() {
    try {
      connection = getConnection();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void addURLInTracking(PriceTracker pt) throws SQLException {
    Integer id = getIDByURLAndUserId(pt.getUrl(), pt.getUserId());
    if (id != null) {
      changeTargetPrice(pt, id);
      return;
    }
    log.info("Adding in DB, New URL : {}", pt.getUrl());

    PreparedStatement ps = connection.prepareStatement(
        "insert into PriceTracker(url, store, currentPrice, targetPrice, userId, productName) values(?, ?, ?, ?, ?,?)");
    ps.setString(1, pt.getUrl());
    ps.setString(2, pt.getStore());
    ps.setDouble(3, pt.getProduct().getPrice());
    ps.setDouble(4, pt.getTargetPrice());
    ps.setLong(5, pt.getUserId());
    ps.setString(6, pt.getProduct().getProductName());
    ps.execute();
    log.info("Tracking URL Added : {}", pt.getUrl());
  }

  private Integer getIDByURLAndUserId(String url, Long userId) throws SQLException {
    log.info("Checking URL : {}, For User : {}", url, userId);

    PreparedStatement ps = connection.prepareStatement("select id from PriceTracker where url=? and userId=?");
    ps.setString(1, url);
    ps.setLong(2, userId);

    ResultSet resultSet = ps.executeQuery();

    return resultSet.next() ? resultSet.getInt(1) : null;
  }

  public void deleteIDByURLAndUserId(String url, Long userId) throws SQLException {
    log.info("Deleting URL : {}, For User : {}", url, userId);

    PreparedStatement ps = connection.prepareStatement("delete from PriceTracker where url=? and userId=?");
    ps.setString(1, url);
    ps.setLong(2, userId);

    ps.execute();

    log.info("Tracking Deleted for UserId : {}, URL : {}", userId, url);

  }

  private void changeTargetPrice(PriceTracker pt, int id) throws SQLException {
    log.info("Rs {} , New Target Price for URL : {}", pt.getTargetPrice(), pt.getUrl());
    PreparedStatement ps = connection.prepareStatement("update PriceTracker set targetPrice=? where id=?");
    ps.setDouble(1, pt.getTargetPrice());
    ps.setInt(2, id);
    ps.execute();
    log.info("Target Price Updated");
  }

  public boolean isUserExist(Long userId) throws SQLException {
    log.info("isUserExist , Checking for User : {}", userId);
    String query = "select * from User where userid=" + userId;
    Statement st = connection.createStatement();
    ResultSet resultSet = st.executeQuery(query);
    return resultSet.next();
  }

  public void createUser(User user) throws SQLException {
    log.info("Creating new User with Name : {}", user.getFirstName());
    PreparedStatement ps = connection.prepareStatement("insert into User values(?, ?, ?, ?, ?)");
    ps.setLong(1, user.getUserId());
    ps.setString(2, user.getFirstName());
    ps.setString(3, user.getLastName());
    ps.setString(4, user.getUserName());
    ps.setLong(5, user.getChatId());
    ps.execute();
    log.info("User Created with Id : {} , Name : {}", user.getUserId(), user.getFirstName());
  }

  public static Map<Long, List<PriceTracker>> getAllTrackingData() throws SQLException {
    Map<Long, List<PriceTracker>> userData = new HashMap<>();
    Connection conn = getConnection();
    String query = "select * from PriceTracker";
    Statement statement = conn.createStatement();
    ResultSet rs = statement.executeQuery(query);
    while (rs.next()) {
      Long userId = rs.getLong("userId");
      Product product = new Product(rs.getString("productName"), null, rs.getDouble("currentPrice"));
      PriceTracker tracker = new PriceTracker(rs.getString("url"), rs.getString("store"), rs.getDouble("targetPrice"),
          userId, product);
      List<PriceTracker> list = userData.get(userId);
      if (list != null && list.size() > 0) {
        list.add(tracker);
      } else {
        List<PriceTracker> trackers = new ArrayList<>();
        trackers.add(tracker);
        userData.put(userId, trackers);
      }
    }
    return userData;
  }

  public static void updateCurrentPrice(PriceTracker tracker) throws SQLException {
    String query = "update PriceTracker set currentPrice=? where url=? and userId=?";
    Connection conn = getConnection();
    PreparedStatement ps = conn.prepareStatement(query);
    ps.setDouble(1, tracker.getProduct().getPrice());
    ps.setString(2, tracker.getUrl());
    ps.setLong(3, tracker.getUserId());
    ps.execute();

    log.info("Current price updated for iten : {}, Price :  {}, User : {}", tracker.getProduct().getProductName(),
        tracker.getProduct().getPrice(), tracker.getUserId());

  }

  private static Connection getConnection() throws SQLException {
    if (connection != null && !connection.isClosed())
      return connection;
    log.info("Creating DB Connection !");
    connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    return connection;
  }

}
