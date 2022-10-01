package com.pricetracker.bot.trackers;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.pricetracker.bot.utils.UserDataContext;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@NoArgsConstructor
public class PriceTrackingBot extends TelegramLongPollingBot {

  private static Logger log = LoggerFactory.getLogger(PriceTrackingBot.class);
  private static final Integer THREADS = 3;
  private String botApiToken;
  private String botUsername;
  public Map<Long, UserDataContext> userDataContext;

  public PriceTrackingBot(Map<Long, UserDataContext> userDataContext, String token, String username) {
    this.userDataContext = userDataContext;
    this.botApiToken = token;
    this.botUsername = username;
  }

  private static ExecutorService executors = Executors.newFixedThreadPool(THREADS);

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {
      Long userId = update.getMessage().getChat().getId();
      UserDataContext dataContext = userDataContext.get(userId);
      executors.execute(new ProcessUserUpdate(userDataContext, update, this));
    } else {

    }

  }

  public void send(SendMessage sm) {
    try {
      execute(sm);
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }
  }

  @Override
  public String getBotUsername() {
    return this.botUsername;
  }

  @Override
  public String getBotToken() {
    return this.botApiToken;
  }

}
