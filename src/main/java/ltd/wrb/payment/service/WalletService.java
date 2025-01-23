package ltd.wrb.payment.service;

import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.model.Wallet;

import java.util.Map;
import java.math.BigInteger;

public interface WalletService {

    Map<ChainType, String> initWallets(String uid);

    Wallet getEvmWallet(String uid, boolean craeteIfNotExist);

    Wallet getTronWallet(String uid, boolean craeteIfNotExist);

    Wallet getSolanaWallet(String uid, boolean craeteIfNotExist);

    Wallet getByAddressAndChainType(String address, ChainType chainType);

    Map<ChainType, String> getWallets(String uid);

    BigInteger balanceOf(String address, ChainType chainType, Integer chainId);

    BigInteger balanceOfContract(String address, String contractAddress, ChainType chainType, Integer chainId);

}
