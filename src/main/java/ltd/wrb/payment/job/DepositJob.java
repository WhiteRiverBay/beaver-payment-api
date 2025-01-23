package ltd.wrb.payment.job;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.job.evm.EVMScanner;
import ltd.wrb.payment.job.trx.TronScanner;
import ltd.wrb.payment.model.EVMChainConfig;

@Component
@Slf4j
public class DepositJob implements InitializingBean {

    @Autowired
    private OnDeposit onDeposit;

    private Map<Integer, Scanner> scanners = new HashMap<>();

    public void restartScanner(Integer chainId) {
        Scanner scanner = scanners.get(chainId);
        if (scanner != null) {
            scanner.restart();
        }
    }   

    public Map<Integer, Boolean> getScannerStatus() {
        Map<Integer, Boolean> status = new HashMap<>();
        for (Integer chainId : scanners.keySet()) {
            status.put(chainId, scanners.get(chainId).isRunning());
        }
        return status;
    }   

    @Override
    public void afterPropertiesSet() throws Exception {
        startEvmScanner();
        startTronScanner();
    }

    protected void startEvmScanner() {
        log.info("Starting EVM scanner");
        Map<Integer, EVMChainConfig> configs = EVMChainConfig.load();
        for (EVMChainConfig config : configs.values()) {
            EVMScanner scanner = new EVMScanner(config, onDeposit);
            scanner.init();
            scanner.start();
            scanners.put(config.getChainId(), scanner);
        }
        log.info("EVM scanner started");
    }

    protected void startTronScanner() {
        boolean  enabled = Boolean.parseBoolean(System.getenv("TRON_SCANNER_ENABLED"));
        if (!enabled) {
            log.info("Tron scanner is disabled");
            return;
        }
        log.info("Starting Tron scanner");
        TronScanner scanner = new TronScanner(onDeposit);
        scanner.init();
        scanner.start();
        scanners.put(728126428, scanner);
        log.info("Tron scanner started");
    }
}