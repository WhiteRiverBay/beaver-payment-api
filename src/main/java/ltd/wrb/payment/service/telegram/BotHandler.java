package ltd.wrb.payment.service.telegram;

import com.pengrad.telegrambot.model.Update;

public interface BotHandler {
    boolean update(Update update);
}