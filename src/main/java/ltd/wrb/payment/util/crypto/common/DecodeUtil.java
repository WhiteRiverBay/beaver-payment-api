package ltd.wrb.payment.util.crypto.common;

import org.apache.commons.codec.binary.Hex;

import lombok.extern.slf4j.Slf4j;

//import static org.tron.core.Constant.ADD_PRE_FIX_BYTE_MAINNET;

@Slf4j(topic = "Commons")
public class DecodeUtil {
  public static final byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x41;   //41 + address
  public static final int ADDRESS_SIZE = 42;
  public static byte addressPreFixByte = ADD_PRE_FIX_BYTE_MAINNET;

  public static boolean addressValid(byte[] address) {
    if (address == null || address.length == 0) {
      log.warn("Warning: Address is empty !!");
      return false;
    }
    if (address.length != ADDRESS_SIZE / 2) {
      log.warn(
          "Warning: Address length need " + ADDRESS_SIZE + " but " + address.length
              + " !!");
      return false;
    }

    if (address[0] != addressPreFixByte) {
      log.warn("Warning: Address need prefix with " + addressPreFixByte + " but "
          + address[0] + " !!");
      return false;
    }
    //Other rule;
    return true;
  }

  public static String createReadableString(byte[] bytes) {
    // return ByteArray.toHexString(bytes);
    return Hex.encodeHexString(bytes);
  }

}
