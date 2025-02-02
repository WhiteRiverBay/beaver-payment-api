package ltd.wrb.payment.service.impl;

import java.math.BigInteger;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bouncycastle.util.encoders.Hex;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;

import lombok.extern.slf4j.Slf4j;
import ltd.wrb.payment.enums.ChainType;
import ltd.wrb.payment.model.Wallet;
import ltd.wrb.payment.repository.AddressPoolRepository;
import ltd.wrb.payment.repository.WalletRepository;
import ltd.wrb.payment.service.WalletService;
import ltd.wrb.payment.util.EncryptUtil;
import ltd.wrb.payment.util.KeyPairBytes;
import ltd.wrb.payment.util.RSAUtil;
import ltd.wrb.payment.util.SolanaUtil;
import ltd.wrb.payment.util.TRONUtil;
import ltd.wrb.payment.util.TRONUtil.TKeyPair;
import ltd.wrb.payment.util.Web3jUtil;

@Service
@Slf4j
public class WalletServiceImpl implements WalletService, InitializingBean {

    @Autowired
    private AddressPoolRepository addressPoolRepository;
    @Autowired
    private WalletRepository walletRepository;

    @Override
    public Wallet getEvmWallet(String userId, boolean craeteIfNotExist) {
        Wallet wallet = walletRepository.findByUidAndChainType(userId, ChainType.EVM);
        if (wallet == null && craeteIfNotExist) {
            try {
                Credentials credentials = Web3jUtil.getRandomCredentials();

                String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);

                PublicKey publicKey = RSAUtil.getPublicKeyFromPem(getRSAPublicKeyFromEnv());

                String[] datas = EncryptUtil.encrypt(privateKey, publicKey);

                wallet = Wallet.builder()
                        .uid(userId)
                        .chainType(ChainType.EVM)
                        .address(Keys.toChecksumAddress(credentials.getAddress()))
                        .encryptedPrivateKey(datas[0])
                        .encryptedAesKey(datas[1])
                        .build();
                save(wallet);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create wallet", ex);
            }
        }
        return wallet;
    }

    @Override
    public Wallet getTronWallet(String userId, boolean craeteIfNotExist) {
        Wallet wallet = walletRepository.findByUidAndChainType(userId, ChainType.TRON);
        if (wallet == null && craeteIfNotExist) {
            try {
                TKeyPair keyPair = TRONUtil.createKeyPair();
                String privateKey = Hex.toHexString(keyPair.privateKey);

                PublicKey rsaPublicKey = RSAUtil.getPublicKeyFromPem(getRSAPublicKeyFromEnv());

                String tronAddress = TRONUtil.getTronAddress(keyPair.publicKey);

                String[] datas = EncryptUtil.encrypt(privateKey, rsaPublicKey);

                wallet = Wallet.builder()
                        .uid(userId)
                        .chainType(ChainType.TRON)
                        .address(tronAddress)
                        .encryptedPrivateKey(datas[0])
                        .encryptedAesKey(datas[1])
                        .build();
                save(wallet);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create wallet", ex);
            }
        }
        return wallet;
    }

    @Override
    public Wallet getSolanaWallet(String userId, boolean craeteIfNotExist) {
        Wallet wallet = walletRepository.findByUidAndChainType(userId, ChainType.SOLANA);
        if (wallet == null && craeteIfNotExist) {
            try {
                KeyPairBytes keyPair = SolanaUtil.createKeyPair();
                String privateKey = keyPair.getPrivateKeyHex();

                PublicKey rsaPublicKey = RSAUtil.getPublicKeyFromPem(getRSAPublicKeyFromEnv());

                String solanaAddress = keyPair.getAddress();

                String[] datas = EncryptUtil.encrypt(privateKey, rsaPublicKey);

                wallet = Wallet.builder()
                        .uid(userId)
                        .chainType(ChainType.SOLANA)
                        .address(solanaAddress)
                        .encryptedPrivateKey(datas[0])
                        .encryptedAesKey(datas[1])
                        .build();
                save(wallet);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to create wallet", ex);
            }
        }
        return wallet;
    }

    @Override
    public Map<ChainType, String> initWallets(String userId) {
        Map<ChainType, String> result = new HashMap<>();
        Wallet w1 = getEvmWallet(userId, true);
        Wallet w2 = getTronWallet(userId, true);
        result.put(w1.getChainType(), w1.getAddress());
        result.put(w2.getChainType(), w2.getAddress());
        return result;
    }

    @Override
    public BigInteger balanceOf(String address, ChainType chainType, Integer chainId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public BigInteger balanceOfContract(String address, String contractAddress, ChainType chainType, Integer chainId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public Map<ChainType, String> getWallets(String userId) {
        List<Wallet> wallets = walletRepository.findByUid(userId);
        Map<ChainType, String> result = new HashMap<>();
        for (Wallet wallet : wallets) {
            result.put(wallet.getChainType(), wallet.getAddress());
        }
        return result;
    }

    protected void save(Wallet wallet) {
        walletRepository.save(wallet);
    }

    private String getRSAPublicKeyFromEnv() {
        String publicKeyPemPath = System.getenv("WALLET_RSA_KEY");
        return cn.hutool.core.io.FileUtil.readUtf8String(publicKeyPemPath);
    }

    @Override
    public Wallet getByAddressAndChainType(String address, ChainType chainType) {
        return walletRepository.findByAddressAndChainType(address, chainType);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // rsa key can not be null
        if (getRSAPublicKeyFromEnv() == null) {
            throw new RuntimeException("WALLET_RSA_KEY is required");
        }

        // rsa pem public key can not be null
        String pem = getRSAPublicKeyFromEnv();
        // check if the key is valid
        PublicKey pk = RSAUtil.getPublicKeyFromPem(pem);
        if (pk == null) {
            throw new RuntimeException("WALLET_RSA_KEY is invalid");
        }
    }
}
