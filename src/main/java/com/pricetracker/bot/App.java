package com.pricetracker.bot;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import com.pricetracker.bot.trackers.PriceTrackingBot;
import com.pricetracker.bot.user.MysqlDao;
import com.pricetracker.bot.utils.UserDataContext;
import com.pricetracker.bot.wrappers.PriceTracker;
import com.pricetracker.scrapper.PriceTracking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class App {

	private static Logger log = LoggerFactory.getLogger(App.class);

	private static final Long SCHEDULE_MILLI_SEC = 60000l;

	public static Map<Long, UserDataContext> userDataContext = new HashMap<>();

	public static void main(String[] args) throws TelegramApiException {

		String apiToken = System.getenv("TELEBOT_API_TOKEN");
		String botUsername = System.getenv("TELEBOT_USERNAME");

		try {
			log.info("Getting All Data from DB");

			// TODO - Directly return UserDataContext from MysqlDao
			Map<Long, List<PriceTracker>> userData = MysqlDao.getAllTrackingData();
			for (Long userId : userData.keySet()) {
				UserDataContext dataContext = new UserDataContext();
				dataContext.trackingList = userData.get(userId);
				userDataContext.put(userId, dataContext);
			}

		} catch (SQLException e) {
			log.error("Error while getting Tracking data");
			e.printStackTrace();
			System.exit(0);
		}

		log.info("Starting Price tracking Bot");
		TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
		telegramBotsApi.registerBot(new PriceTrackingBot(userDataContext, apiToken, botUsername));

		// PRICE TRACKING JOB
		log.info("Scheduling Price tracking Job");
		new Timer().schedule(new PriceTracking(userDataContext, new PriceTrackingBot()), 0, SCHEDULE_MILLI_SEC);

	}

}
