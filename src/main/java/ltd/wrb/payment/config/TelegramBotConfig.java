package ltd.wrb.payment.config;


import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pengrad.telegrambot.TelegramBot;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

@Configuration
@Slf4j
public class TelegramBotConfig {
    @Bean
    public TelegramBot getBot() {
        System.out.println("Telegram service is running...");
        log.info("Telegram service is running...");
        String botToken = System.getenv("TELEGRAM_BOT_TOKEN");

        if (botToken == null || StringUtils.isEmpty(botToken)) {
            // throw new RuntimeException("BOT_TOKEN is not set");
            log.error("BOT_TOKEN is not set");
            return null;
        }

        // OkHttpClient okHttpClient = new OkHttpClient();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .pingInterval(Duration.ofMinutes(1))
                .readTimeout(Duration.ofSeconds(10))
                .build();

        String proxyHost = System.getenv("PROXY_HOST");
        String proxyPort = System.getenv("PROXY_PORT");
        String proxyType = System.getenv("PROXY_TYPE");

        if (proxyHost != null && !StringUtils.isEmpty(proxyHost) && proxyPort != null && proxyType != null) {
            if (proxyType.equals("http")) {
                okHttpClient = okHttpClient.newBuilder().proxy(new Proxy(Proxy.Type.HTTP,
                        new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))))
                        .pingInterval(Duration.ofMinutes(1))
                        .readTimeout(Duration.ofSeconds(10))
                        .build();
            } else if (proxyType.equals("socks")) {
                okHttpClient = okHttpClient.newBuilder().proxy(new Proxy(Proxy.Type.SOCKS,
                        new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))))
                        .pingInterval(Duration.ofMinutes(1))
                        .readTimeout(Duration.ofSeconds(10))
                        .build();
            }
        }

        TelegramBot bot = new TelegramBot.Builder(botToken).okHttpClient(okHttpClient).build();
        return bot;
    }
}