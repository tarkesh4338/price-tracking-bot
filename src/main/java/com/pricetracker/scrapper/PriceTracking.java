package com.pricetracker.scrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pricetracker.bot.trackers.PriceTrackingBot;
import com.pricetracker.bot.utils.UserDataContext;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AllArgsConstructor
@NoArgsConstructor
public class PriceTracking extends TimerTask {

  private static Logger log = LoggerFactory.getLogger(PriceTracking.class);

  private Map<Long, UserDataContext> userDataContext;
  private PriceTrackingBot bot;
  private static final Integer THREADS = 4;

  @Override
  public void run() {

    ExecutorService executor = Executors.newFixedThreadPool(THREADS);

    List<Callable<Void>> callables = new ArrayList<>();

    for (Long userId : userDataContext.keySet()) {
      userDataContext.get(userId).trackingList.forEach(track -> {
        Tracker tracker = new Tracker(track, bot);
        callables.add(tracker);
      });
    }
    try {
      executor.invokeAll(callables);
    } catch (InterruptedException e) {
      log.error("Error While Executing");
      e.printStackTrace();
    }
    executor.shutdown();

  }
}
