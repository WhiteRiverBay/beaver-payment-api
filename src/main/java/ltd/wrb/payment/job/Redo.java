package ltd.wrb.payment.job;

public interface Redo {
    
    void checkTron(String hash);

    void checkEth(String hash, Integer chainId);
}
