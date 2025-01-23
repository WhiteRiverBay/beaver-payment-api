package ltd.wrb.payment.service.telegram.group;

import java.util.Calendar;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.model.stat.ChainIdAmount;
import ltd.wrb.payment.model.stat.PaymentOrderStat;
import ltd.wrb.payment.model.stat.TradeTypeAmount;
import ltd.wrb.payment.service.StatService;
import ltd.wrb.payment.service.telegram.BotHandler;

@Component
@Slf4j
public class GroupTextHandler implements BotHandler {

    @Autowired(required = false)
    private TelegramBot bot;

    @Autowired
    private StatService statService;

    @Override
    public boolean update(Update update) {
        // group chat id
        long chatId = update.message().chat().id();
        log.info("group chat id: {}", chatId);

        Long groupIdInConfig = Long.valueOf(System.getenv("TELEGRAM_ADMIN_GROUP_CHAT_ID"));
        if (chatId == groupIdInConfig) {
            String text = update.message().text();

            if ("stat".equals(text)) {

                _stat(update);

            }
            return true;
        }
        return false;
    }

    private void _stat(Update update) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance();
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);
        end.set(Calendar.MILLISECOND, 999);

        Calendar startFromBegin = Calendar.getInstance();
        startFromBegin.set(Calendar.YEAR, 2022);

        List<TradeTypeAmount> typeAmountToday = statService.getAccountLogStatGroupByTradeType(start.getTime(),
                end.getTime());
        List<TradeTypeAmount> typeAmountFromBegin = statService
                .getAccountLogStatGroupByTradeType(startFromBegin.getTime(), end.getTime());

        log.info("typeAmountToday: {}", typeAmountToday);
        log.info("typeAmountFromBegin: {}", typeAmountFromBegin);

        // statService.getChainDepositStatGroupByChainId(null, null)
        List<ChainIdAmount> chainIdAmountToday = statService.getChainDepositStatGroupByChainId(start.getTime(),
                end.getTime());
        List<ChainIdAmount> chainIdAmountFromBegin = statService
                .getChainDepositStatGroupByChainId(startFromBegin.getTime(), end.getTime());

        log.info("chainIdAmountToday: {}", chainIdAmountToday);
        log.info("chainIdAmountFromBegin: {}", chainIdAmountFromBegin);

        // statService.getOrderStatGroupByStatus(null, null)
        List<PaymentOrderStat> orderStatToday = statService.getOrderStatGroupByStatus(start.getTime(), end.getTime());
        List<PaymentOrderStat> orderStatFromBegin = statService.getOrderStatGroupByStatus(startFromBegin.getTime(),
                end.getTime());

        log.info("orderStatToday: {}", orderStatToday);
        log.info("orderStatFromBegin: {}", orderStatFromBegin);

        StringBuilder sb = new StringBuilder();
        sb.append("<b>今日新增</b>\n");

        sb.append("账户变动统计\n");
        for (TradeTypeAmount typeAmount : typeAmountToday) {
            sb.append("<b>" + typeAmount.getType().name() + "</b>").append(": ").append(typeAmount.getAmount())
                    .append("\n");
        }

        sb.append("链充值统计\n");
        for (ChainIdAmount chainIdAmount : chainIdAmountToday) {
            sb.append("<b>").append(chainIdAmount.getChainId()).append("</b>").append(": ")
                    .append(chainIdAmount.getAmount()).append("\n");
        }

        sb.append("订单数\n");
        for (PaymentOrderStat orderStat : orderStatToday) {
            sb.append("<b>").append(orderStat.getStatus().name()).append("</b>").append(": ")
                    .append(orderStat.getCount()).append("\n");
        }

        sb.append("订单金额\n");
        for (PaymentOrderStat orderStat : orderStatToday) {
            sb.append("<b>").append(orderStat.getStatus().name()).append("</b>").append(": ")
                    .append(orderStat.getSumAmount()).append("\n");
        }

        sb.append("最大金额\n");
        for (PaymentOrderStat orderStat : orderStatToday) {
            sb.append("<b>").append(orderStat.getStatus().name()).append("</b>").append(": ")
                    .append(orderStat.getMaxAmount()).append("\n");
        }

        sb.append("平均金额\n");
        for (PaymentOrderStat orderStat : orderStatToday) {
            sb.append("<b>").append(orderStat.getStatus().name()).append("</b>").append(": ")
                    .append(orderStat.getAvgAmount()).append("\n");
        }

        sb.append("\n");

        sb.append("<b>历史汇总统计<b>\n");

        sb.append("账户变动统计\n");
        for (TradeTypeAmount typeAmount : typeAmountFromBegin) {
            sb.append("<b>").append(typeAmount.getType().name()).append("</b>").append(": ")
                    .append(typeAmount.getAmount()).append("\n");
        }

        sb.append("链充值统计\n");
        for (ChainIdAmount chainIdAmount : chainIdAmountFromBegin) {
            sb.append("<b>").append(chainIdAmount.getChainId()).append("</b>").append(": ")
                    .append(chainIdAmount.getAmount()).append("\n");
        }

        sb.append("订单数\n");
        for (PaymentOrderStat orderStat : orderStatFromBegin) {
            sb.append("<b>").append(orderStat.getStatus().name()).append("</b>").append(": ")
                    .append(orderStat.getCount()).append("\n");
        }

        sb.append("订单金额\n");
        for (PaymentOrderStat orderStat : orderStatFromBegin) {
            sb.append("<b>").append(orderStat.getStatus().name()).append("</b>").append(": ")
                    .append(orderStat.getSumAmount()).append("\n");
        }

        SendMessage message = new SendMessage(update.message().chat().id(), sb.toString());
        message.parseMode(ParseMode.HTML);
        bot.execute(message);
    }
}
