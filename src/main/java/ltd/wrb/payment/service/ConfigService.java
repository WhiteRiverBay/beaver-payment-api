package ltd.wrb.payment.service;

import java.util.Map;

import ltd.wrb.payment.model.EVMChainConfig;

public interface ConfigService {

    Map<Integer, EVMChainConfig> getEVMChains();

}
