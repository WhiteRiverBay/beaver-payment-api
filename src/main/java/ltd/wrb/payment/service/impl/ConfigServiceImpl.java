package ltd.wrb.payment.service.impl;

import java.util.Map;

import org.springframework.stereotype.Service;

import ltd.wrb.payment.model.EVMChainConfig;
import ltd.wrb.payment.service.ConfigService;

@Service
public class ConfigServiceImpl implements ConfigService {

    @Override
    public Map<Integer, EVMChainConfig> getEVMChains() {
        return EVMChainConfig.load();
    }

}
