package com.pricetracker.bot.wrappers;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class PriceTracker {
  private String url;
  private String store;
  private Double targetPrice;
  private Long userId;
  private Product product;
  private long notificationTime;

  public PriceTracker(String url, String store, Double targetPrice, Long userId, Product product) {
    super();
    this.url = url;
    this.store = store;
    this.targetPrice = targetPrice;
    this.userId = userId;
    this.product = product;
  }

}
