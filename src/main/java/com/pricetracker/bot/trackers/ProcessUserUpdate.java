package com.pricetracker.bot.trackers;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.pricetracker.bot.user.MysqlDao;
import com.pricetracker.bot.utils.UserDataContext;
import com.pricetracker.bot.wrappers.PriceTracker;
import com.pricetracker.bot.wrappers.Product;
import com.pricetracker.bot.wrappers.User;
import com.pricetracker.scrapper.PriceScrapper;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Update;

@NoArgsConstructor
public class ProcessUserUpdate implements Runnable {

  private static Logger log = LoggerFactory.getLogger(ProcessUserUpdate.class);

  // private static PriceTracker priceTracker;
  private UserDataContext dataContext;
  private Map<Long, UserDataContext> userDataContext;

  private Update update;
  private PriceTrackingBot bot;

  public ProcessUserUpdate(Map<Long, UserDataContext> userDataContext, Update update, PriceTrackingBot bot) {
    this.userDataContext = userDataContext;
    this.update = update;
    this.bot = bot;
  }

  @Override
  public void run() {

    if (update.hasMessage() && update.getMessage().hasText()) {
      String msg = update.getMessage().getText();
      long chatId = update.getMessage().getChatId();
      Long userId = update.getMessage().getChat().getId();
      dataContext = userDataContext.get(userId) != null ? userDataContext.get(userId) : new UserDataContext();

      SendMessage sm = new SendMessage();
      sm.setChatId(chatId);

      MysqlDao userDao = new MysqlDao();

      System.out.println("User Command :- " + bot.userDataContext.get(userId));

      if (StringUtils.equalsAnyIgnoreCase(dataContext.previusCommand, "track", "/track")) {
        if (!StringUtils.equalsAnyIgnoreCase(msg, "/cancel", "cancel")) {
          log.info("Tracking for URL - {}", msg);
          validateAndAddURL(msg, sm, userDao, userId);
        } else {
          sm.setText("Cancelled, Start New Command.");
          dataContext.previusCommand = msg;
          bot.userDataContext.put(userId, dataContext);
          send(sm);
        }
      } else if (StringUtils.equalsIgnoreCase(dataContext.previusCommand, "url")) {
        if (!StringUtils.equalsAnyIgnoreCase(msg, "/cancel", "cancel")) {
          log.info("Target Price - {}", msg);
          validateTargetPrice(msg, sm, userDao, userId);
        } else {
          sm.setText("Cancelled, Start New Command.");
          dataContext.previusCommand = msg;
          bot.userDataContext.put(userId, dataContext);
          send(sm);
        }
      } else if (StringUtils.equalsAnyIgnoreCase(msg, "/start", "start")) { // Start Command
        dataContext.previusCommand = msg;
        bot.userDataContext.put(userId, dataContext);
        log.info("User Command Started !");
        userStart(update, sm, chatId, userDao);
      } else if (StringUtils.equalsAnyIgnoreCase(msg, "/track", "track")) {
        dataContext.previusCommand = msg;
        bot.userDataContext.put(userId, dataContext);
        sm.setText("Enter product URL :- ");
        send(sm);
      } else if (StringUtils.equalsAnyIgnoreCase(msg, "/list", "list")) {
        dataContext.previusCommand = msg;
        bot.userDataContext.put(userId, dataContext);
        String message = createListMessage(userId);
        if (message != null && message.length() > 0) {
          sm.setText(message);
        } else {
          sm.setText("Nothing to Track, Add from menu :)");
        }
        send(sm);
      } else if (StringUtils.equalsAnyIgnoreCase(msg, "/cancel", "cancel")) {
        dataContext.previusCommand = msg;
        bot.userDataContext.put(userId, dataContext);
        sm.setText("Cancelled, Start New Command.");
        send(sm);
      } else if (StringUtils.equalsAnyIgnoreCase(msg, "/help", "help")) {
        dataContext.previusCommand = msg;
        bot.userDataContext.put(userId, dataContext);
        String helpMessage = "Click on track in menu to track a URL.\n\n -> To delete Item,  First hit LIST command from menu then you have number for item, After that write delete-number_from_list to delete";
        sm.setText(helpMessage);
        send(sm);
      } else if (StringUtils.startsWith(msg, "delete-")) {
        dataContext.previusCommand = msg;
        bot.userDataContext.put(userId, dataContext);
        String deleteIndex = StringUtils.substringAfter(msg, "delete-");
        int delIndex = Integer.valueOf(deleteIndex) - 1;
        PriceTracker tracker = dataContext.trackingList.get(delIndex);
        try {
          sm.setText("URL - " + tracker.getUrl() + ", Removed from Tracking");
          userDao.deleteIDByURLAndUserId(tracker.getUrl(), tracker.getUserId());
          dataContext.trackingList.remove(delIndex);
        } catch (SQLException e) {
          sm.setText("Sorry, something went wrong from server side , Try Again");
          log.error("Error while deleting data , PT : {}", tracker);
          e.printStackTrace();
        }
        send(sm);
      } else {
        dataContext.previusCommand = "noCommand";
        bot.userDataContext.put(userId, dataContext);
        sm.setText("Invalid Command, Select from Menu !");
        send(sm);

      }

    }
  }

  private void validateAndAddURL(String url, SendMessage sm, MysqlDao userDao, Long userId) {
    if (StringUtils.contains(url, "http")) {

      if (!isValidStoreURL(url)) {
        sm.setText("Not supported URL, Use only FLIPKART OR MYNTRA url, Try agin or Cancel from menu");
        send(sm);
        return;
      }
      try {
        int indexOf = StringUtils.indexOf(url, "http");
        String trackUrl = url.substring(indexOf, url.length());
        sm.setText("Please Wait, Getting product detail :)");
        send(sm);

        Product product;
        if (StringUtils.containsIgnoreCase(url, "FLIPKART")) {
          product = PriceScrapper.getFlipkartProductDetail(trackUrl);
          dataContext.currentTracker = new PriceTracker(trackUrl, "FLIPKART", 0.0, userId, product);
        } else {
          product = PriceScrapper.getMyntraProductDetail(trackUrl);
          dataContext.currentTracker = new PriceTracker(trackUrl, "MYNTRA", 0.0, userId, product);
        }

        dataContext.previusCommand = "url";
        bot.userDataContext.put(userId, dataContext);

        sm.setText(
            "Item Detail \n\nITEM :- " + product.getProductName() + "\n\nCurrent Price :- ₹ " + product.getPrice());
        send(sm);

        sm.setText("Enter your target price :- ");
        send(sm);
      } catch (Exception e) {
        sm.setText("Sorry, Something went wrong , Please try again !");
        send(sm);
        e.printStackTrace();
      }
    } else {
      sm.setText("Invalid URL , Enter URL Again Or Cancel from Menu !");
      send(sm);
    }
  }

  private void validateTargetPrice(String price, SendMessage sm, MysqlDao userDao, Long userId) {
    if (NumberUtils.isDigits(price)) {
      dataContext.previusCommand = "start";
      bot.userDataContext.put(userId, dataContext);
      try {
        Product product = dataContext.currentTracker.getProduct();
        dataContext.currentTracker.setTargetPrice(Double.valueOf(price));
        userDao.addURLInTracking(dataContext.currentTracker);
        sm.setText("Verify Item detail");
        send(sm);
        sm.setText("Tracking started \n\nITEM :- " + product.getProductName() + "\n\nCurrent Price :- ₹ "
            + product.getPrice() + " \n\n Target Price :- ₹ " + price);
        send(sm);
        // Add or Update in user tracking list
        // dataContext.trackingList.add(dataContext.currentTracker);
        addOrUpdateInTrackingList();
      } catch (Exception e) {
        sm.setText("Sorry, Something went wrong , Please try again !");
        send(sm);
        e.printStackTrace();
      }
    } else {
      sm.setText("Invalid Target Price , Enter price again Or Cancel from Menu !");
      send(sm);
    }

  }

  private void addOrUpdateInTrackingList() {
    boolean isUpdated = false;
    for (int i = 0; i < dataContext.trackingList.size(); i++) {
      PriceTracker tracker = dataContext.trackingList.get(i);
      if (StringUtils.equalsIgnoreCase(dataContext.currentTracker.getUrl(), tracker.getUrl())) {
        dataContext.trackingList.get(i).setTargetPrice(dataContext.currentTracker.getTargetPrice());
        log.info("Updating Price is user Context");
        isUpdated = true;
        break;
      }
    }
    if (!isUpdated) {
      log.info("Adding new tracker in User Context");
      dataContext.trackingList.add(dataContext.currentTracker);
    }
  }

  private void userStart(Update update, SendMessage sm, Long chatId, MysqlDao ud) {
    Chat chat = update.getMessage().getChat();
    String firstName = chat.getFirstName();
    try {
      if (ud.isUserExist(chat.getId())) {
        sm.setText("Welcome back , " + firstName + " :)");
      } else {
        User user = new User(chat.getId(), firstName, chat.getLastName(), chat.getUserName(), chatId);
        ud.createUser(user);
        sm.setText("Welcome " + firstName + " :)");
      }
      send(sm);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void send(SendMessage sm) {

    bot.send(sm);

  }

  private boolean isValidStoreURL(String url) {
    if (StringUtils.containsAnyIgnoreCase(url, "flipkart", "myntra")) {
      return true;
    }
    return false;
  }

  private String createListMessage(Long userId) {
    StringBuilder sb = new StringBuilder();
    List<PriceTracker> userData = dataContext.trackingList.stream().filter(pt -> pt.getUserId().equals(userId))
        .collect(Collectors.toList());
    // log.info("List for user : {}, List : {}, All Data : {} ", userId, userData, dataContext.trackingList);
    for (int index = 0; index < userData.size(); index++) {
      PriceTracker pt = userData.get(index);
      sb.append((index + 1) + ".\n");
      sb.append("Item  - " + pt.getProduct().getProductName() + "\n");
      sb.append("Current Price - " + pt.getProduct().getPrice() + "\n");
      sb.append("Target Price - " + pt.getTargetPrice() + "\n");
      sb.append("Item URL - " + pt.getUrl());
      sb.append("\n\n");
    }
    return sb.toString();
  }

}
