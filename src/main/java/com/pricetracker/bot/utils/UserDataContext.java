package com.pricetracker.bot.utils;

import java.util.List;

import com.pricetracker.bot.wrappers.PriceTracker;
import lombok.ToString;

@ToString
public class UserDataContext {
  public String previusCommand;
  public PriceTracker currentTracker;
  public List<PriceTracker> trackingList;
}
