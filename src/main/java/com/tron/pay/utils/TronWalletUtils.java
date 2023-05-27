package com.tron.pay.utils;

import cn.hutool.core.util.HexUtil;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Sha256Hash;

import java.math.BigDecimal;

public class TronWalletUtils {

    private static final String ADDRESS_PREFIX = "41";

    public static String convertToBase58(String hexString) {
        String addressWithPrefix = hexString;
        byte[] addressBytes = HexUtil.decodeHex(addressWithPrefix);

        byte[] hash = Sha256Hash.hashTwice(addressBytes);
        byte[] checksum = new byte[4];
        System.arraycopy(hash, 0, checksum, 0, 4);

        byte[] finalAddress = new byte[addressBytes.length + checksum.length];
        System.arraycopy(addressBytes, 0, finalAddress, 0, addressBytes.length);
        System.arraycopy(checksum, 0, finalAddress, addressBytes.length, checksum.length);

        return Base58.encode(finalAddress);
    }

    public static String convertToHex(String base58String) {
        byte[] decoded = Base58.decodeChecked(base58String);
        if (decoded.length < 21) {
            throw new IllegalArgumentException("Invalid decoded address length: " + decoded.length);
        }
        byte[] hexBytes = new byte[decoded.length - 1];
        System.arraycopy(decoded, 1, hexBytes, 0, decoded.length - 1);
        return HexUtil.encodeHexStr(hexBytes);
    }

    public static String toTronAmount(String amount) {
        BigDecimal value = new BigDecimal(amount);
        long tronValue = value.multiply(BigDecimal.valueOf(1000000L)).longValueExact();
        return String.valueOf(tronValue);
    }

    public static String fromTronAmount(String tronValue) {
        BigDecimal value = new BigDecimal(tronValue);
        BigDecimal normalValue = value.divide(BigDecimal.valueOf(1000000L));
        return normalValue.toPlainString();
    }
}