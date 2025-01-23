package ltd.wrb.payment.model;

import java.util.Map;

import org.apache.commons.io.FileUtils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class EVMChainConfig {

    private int chainId;

    private String chainName;

    private String name;

    private String symbol;

    private int decimals;

    private String rpc;

    // private String usdtContract;

    // private int usdtDecimals;

    private String explorer;

    // private String usdSymbol;

    private String depositDescription;

    private Map<String, Object> description;

    private List<EVMChainConfigToken> usdtContracts = new ArrayList<>();

    private Integer confirmBlock = 1;

    private Integer confirmTimeDelaySeconds = 20;

    private BigDecimal confirmThresholdAmountInDecimals = new BigDecimal(-1);   

    public static Map<Integer, EVMChainConfig> load() {
        String src = System.getenv("WALLET_EVM_CHAINS_SRC");
        String url = System.getenv("WALLET_EVM_CHAINS_FILE_OR_URL");

        if (src == null || src.isEmpty()) {
            return null;
        }

        if (src.equals("url")) {
            return EVMChainConfig.fromUrl(url);
        } else if (src.equals("file")) {
            return EVMChainConfig.fromFile(url);
        } else {
            throw new RuntimeException("Invalid WALLET_EVM_CHAINS_SRC: " + src);
        }
    }

    public static Map<Integer, EVMChainConfig> fromUrl(String url) {
        try {
            String jsonContent = FileUtil.readUtf8String(url);
            return EVMChainConfig.parse(jsonContent);
        } catch (Exception e) {
            throw new RuntimeException("url not found: " + url);
        }
    }

    public static Map<Integer, EVMChainConfig> fromFile(String file) {
        try {
            String jsonContent = FileUtils.readFileToString(FileUtils.getFile(file), "UTF-8");
            return EVMChainConfig.parse(jsonContent);
        } catch (Exception e) {
            throw new RuntimeException("file not found: " + file);
        }
    }

    private static Map<Integer, EVMChainConfig> parse(String jsonContent) {
        if (jsonContent == null || jsonContent.isEmpty()) {
            throw new RuntimeException("empty content");
        }
        List<EVMChainConfig> chains = JSONUtil.toList(jsonContent, EVMChainConfig.class);
        Map<Integer, EVMChainConfig> chainMap = new HashMap<>();
        for (EVMChainConfig chain : chains) {
            chainMap.put(chain.getChainId(), chain);
        }
        return chainMap;
    }

    public static EVMChainConfig getChainConfig(int chainId) {
        Map<Integer, EVMChainConfig> chains = EVMChainConfig.load();
        if (chains == null) {
            return null;
        }
        return chains.get(chainId);
    }

}
