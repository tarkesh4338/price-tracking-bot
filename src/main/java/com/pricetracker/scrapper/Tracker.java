package com.pricetracker.scrapper;

import java.util.concurrent.Callable;

import com.pricetracker.bot.trackers.PriceTrackingBot;
import com.pricetracker.bot.user.MysqlDao;
import com.pricetracker.bot.wrappers.PriceTracker;
import com.pricetracker.bot.wrappers.Product;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@AllArgsConstructor
@NoArgsConstructor
public class Tracker implements Callable<Void> {

  private static Logger log = LoggerFactory.getLogger(Tracker.class);

  private PriceTracker track;
  private PriceTrackingBot bot;
  private static final Long THIRTY_MIN_MILLIS = 1800000l;

  @Override
  public Void call() {
    try {
      long currentTimeMillis = System.currentTimeMillis();
      // currentTimeMillis -
      long timeDiff = currentTimeMillis - track.getNotificationTime();
      if (timeDiff > THIRTY_MIN_MILLIS) {
        log.info("Getting Current price for : {}", track.getProduct().getProductName());
        Product product;
        if (StringUtils.containsIgnoreCase(track.getUrl(), "FLIPKART")) {
          product = PriceScrapper.getFlipkartProductDetail(track.getUrl());
        } else {
          product = PriceScrapper.getMyntraProductDetail(track.getUrl());
        }
        log.info("Item  - {} , Current Price - {}, TargetPrice : {}", product.getProductName(), product.getPrice(),
            track.getTargetPrice());
        track.setProduct(product);
        if (product.getPrice() < track.getTargetPrice()) {
          SendMessage sm = new SendMessage();
          sm.setChatId(track.getUserId());

          sm.setText("Check for below Item , Price is now less than target Price.\n\n Item Name :- "
              + product.getProductName() + "\n\nCurrent price :- " + product.getPrice() + "\n\nPrevious Price :- "
              + track.getProduct().getPrice() + "\n\n Your Target Price : - " + track.getTargetPrice()
              + "\n\n Item Link :- " + track.getUrl());

          log.info("Notifying to User");
          bot.send(sm);
          track.setNotificationTime(currentTimeMillis);
          MysqlDao.updateCurrentPrice(track);
        }
      }
      return null;
    } catch (Exception e) {
      log.error("Error while getting Data for {}", track);
      e.printStackTrace();
    }
    return null;
  }
}
