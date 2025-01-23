package ltd.wrb.payment.job;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;

import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.enums.TradeType;
import ltd.wrb.payment.model.EVMChainConfig;
import ltd.wrb.payment.model.TradeLog;
import ltd.wrb.payment.repository.TradeLogRepository;
import ltd.wrb.payment.service.PaymentService;
import ltd.wrb.payment.util.Web3jUtil;

@Slf4j
@Component
public class TransactionUpdater {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private TradeLogRepository tradeLogRepository;

    @Scheduled(fixedRate = 1500)
    public void execute() {

        List<TradeLog> tradeLogs = tradeLogRepository.findByNeedConfirmLogs(TradeType.DEPOSIT);

        if (tradeLogs.isEmpty()) {
            return;
        }

        Map<Integer, Number> currentBlockNumber = new HashMap<>();
        Map<Integer, EVMChainConfig> chainConfigs = EVMChainConfig.load();
        Map<Integer, Web3j> web3js = new HashMap<>();

        for (EVMChainConfig chainConfig : chainConfigs.values()) {
            Web3j web3j = Web3j.build(new HttpService(chainConfig.getRpc()));
            web3js.put(chainConfig.getChainId(), web3j);
            try {
                currentBlockNumber.put(chainConfig.getChainId(), web3j.ethBlockNumber().send().getBlockNumber());
            } catch (IOException e) {
                log.error("Failed to get block number for chain {}", chainConfig.getChainId(), e);
            }
        }

        for (TradeLog tradeLog : tradeLogs) {
            Number _current = currentBlockNumber.get(tradeLog.getChainId()).intValue();
            int diff = _current.intValue() - Integer.parseInt(tradeLog.getBlockNumber());

            Web3j web3j = web3js.get(tradeLog.getChainId());
            if (web3j == null) {
                log.error("Web3j not found for chain {}", tradeLog.getChainId());
                continue;
            }
            TransactionReceipt tx;
            try {
                tx = Web3jUtil.getTransactionReceipt(web3j, tradeLog.getTxHash());
                if (tx == null) {
                    log.error("Transaction receipt not found for tx hash {}", tradeLog.getTxHash());
                    continue;
                }
                if (tx.getStatus().equals("0x1")) {
                    // 记录确认的区块数
                    tradeLog.setConfirmedBlocks(diff);
                    tradeLogRepository.save(tradeLog);

                    // 处理入账成功逻辑 - 找到最新的一个订单，检查是否能pay了
                    if (diff >= tradeLog.getConfirmedBlocksRequired()) {
                        paymentService.tryPayLastOrder(tradeLog.getUid(), true);
                    }
                } else {
                    // 处理入账失败逻辑
                    log.error("Transaction failed for tx hash {} status {}", tradeLog.getTxHash(), tx.getStatus());
                    tradeLog.setStatus(0);
                    tradeLogRepository.save(tradeLog);
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("Failed to get transaction receipt for tx hash {}", tradeLog.getTxHash());
                continue;
            }

        }
    }
}
