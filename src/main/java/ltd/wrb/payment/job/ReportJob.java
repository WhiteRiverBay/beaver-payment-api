package ltd.wrb.payment.job;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ReportJob {
    
    public boolean enabled() {
        String chatId = System.getenv("TELEGRAM_ADMIN_GROUP_CHAT_ID");
        String token = System.getenv("TELEGRAM_BOT_TOKEN");
        return chatId != null && token != null && !chatId.isEmpty() && !token.isEmpty();
    }

    public void report(String message) {
        if (!enabled()) {
            log.warn("Telegram group chat id or bot token is not set");
        }
        // send message to telegram group
        // TODO: implement telegram bot
    }

}
