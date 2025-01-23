/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package ltd.wrb.payment;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import ltd.wrb.payment.util.TRONUtil;
import org.bouncycastle.util.encoders.Hex;

/**
 *
 * @author ethan
 */
public class TronUtilTest {

    @Test
    public void testGenerateKeyPair() throws Exception {
        // Generate key pair
        TRONUtil.TKeyPair keyPair = TRONUtil.createKeyPair();
        
        // Verify key pair is not null
        assertNotNull(keyPair);

        System.out.println("privateKey: " + Hex.toHexString(keyPair.privateKey));
        System.out.println("publicKey: " + Hex.toHexString(keyPair.publicKey)); 

        TRONUtil.importHexPrivateKey(Hex.toHexString(keyPair.privateKey));      
        

    }

}
