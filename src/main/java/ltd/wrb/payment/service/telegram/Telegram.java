package ltd.wrb.payment.service.telegram;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Configuration
public class Telegram implements InitializingBean {

    @Autowired
    private List<BotHandler> botHandlers;

    @Autowired(required = false)
    private TelegramBot bot;

    @Override
    public void afterPropertiesSet() throws Exception {

        if (bot == null) {
            log.error("bot is null");
            return;
        }

        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (isDebug()) {
                    log.info("update: {}", update);
                }
                botHandlers.forEach(handler -> {
                    try {
                        boolean res = handler.update(update);
                        if (res) {
                            log.info("update processed by : {}", handler.getClass().getName());
                        }
                    } catch (Exception e) {
                        log.error("error: {}", e);
                    }
                });

            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, error -> {
            if (error.response() != null) {
                log.error("error: {}", error.response().description());
            } else {
                log.error("error: {}", error);
            }
        });
    }

    // send message to
    public void sendMessage(SendMessage message) {
        if (isDebug()) {
            log.info("send message: {}", message);
        }

        if (bot == null) {
            log.error("bot is null");
            return;
        }

        try {
            SendResponse resp = bot.execute(message);
            log.info("send message: isOK? {} {} {} {}", resp.isOk(), resp.errorCode(), resp.description(), resp.message());
        } catch (Exception e) {
            log.error("error: {}", e);
        }
    }

    private boolean isDebug() {
        String debug = System.getenv("DEBUG");
        return debug != null && debug.equals("true");
    }
}