package ltd.wrb.payment.util;

import com.pengrad.telegrambot.model.User;

public class TelegramUtil {

    public static String getDisplayName(User user, boolean withAt) {
        String name = "";
        if (user.firstName() != null) {
            name += user.firstName();
        }
        if (user.lastName() != null) {
            name += " " + user.lastName();
        }
        if (user.username() != null && withAt) {
            name += " @" + user.username();
        }
        return name;
    }
}