package com.maxieds.chameleonminilivedebugger;

import android.content.Context;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationManager;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.UUID;

/**
 * <h1>Utils</h1>
 * Misc utility functions for the application.
 *
 * @author Maxie D. Schmidt
 * @since 12/31/17
 */
public class Utils {

    private static final String TAG = Utils.class.getSimpleName();

    public static int getFirstResponseCodeIndex(String s) {
        Pattern pattern = Pattern.compile("^\\D*([1-9][0-9][0-9])");
        Matcher matcher = pattern.matcher(s);
        matcher.find();
        return matcher.start(1);
    }

    public static String formatUIDString(String hexBytesStr, String delim) {
        if(hexBytesStr.length() == 0) {
            return "<uid-unknown>";
        }
        else if(hexBytesStr.equals("NO UID.")) {
            return hexBytesStr;
        }
        return hexBytesStr.replaceAll("..(?!$)", "$0" + delim);
    }

    /**
     * Converts a string representation of a two-digit byte into a corresponding byte type.
     * @param byteStr
     * @return byte representation of the String
     */
    public static byte hexString2Byte(String byteStr) {
        if (byteStr.length() != 2) {
            Log.e(TAG, "Invalid Byte String: " + byteStr);
            //Crashlytics.log(Log.WARN, TAG, "Invalid Byte String Encountered: " + byteStr);
            return 0x00;
        }
        int lsb = Character.digit(byteStr.charAt(1), 16);
        int msb = Character.digit(byteStr.charAt(0), 16);
        return (byte) (lsb | msb << 4);
    }

    // TODO: javadoc
    public static byte[] hexString2Bytes(String byteStr) {
        if (byteStr.length() % 2 != 0) { // left-pad the string:
            byteStr = "0" + byteStr;
        }
        byte[] byteRep = new byte[byteStr.length() / 2];
        for (int b = 0; b < byteStr.length(); b += 2)
            byteRep[b / 2] = hexString2Byte(byteStr.substring(b, b + 2));
        return byteRep;
    }

    /**
     * Returns an ascii print character (or '.' representation for non-print characters) of the input byte.
     * @param b
     * @return char print character (or '.')
     */
    public static char byte2Ascii(byte b) {
        int decAsciiCode = (int) b;
        if (b >= 32 && b <= 127) {
            char ch = (char) b;
            return ch;
        } else
            return '.';
    }

    /**
     * Returns an ascii string representing the byte array.
     * @param bytes
     * @return String ascii representation of the byte array
     */
    public static String bytes2Ascii(byte[] bytes) {
        StringBuilder byteStr = new StringBuilder();
        for (int b = 0; b < bytes.length; b++)
            byteStr.append(String.valueOf(byte2Ascii(bytes[b])));
        return byteStr.toString();
    }

    /**
     * Returns a space-separated string of the input bytes in their two-digit
     * hexadecimal format.
     * @param bytes
     * @return String hex string representation
     */
    public static String bytes2Hex(byte[] bytes) {
        if (bytes == null)
            return "<NULL>";
        else if (bytes.length == 0)
            return "";
        StringBuilder hstr = new StringBuilder();
        hstr.append(String.format(Locale.ENGLISH, "%02x", bytes[0]));
        for (int b = 1; b < bytes.length; b++)
            hstr.append(" " + String.format(Locale.ENGLISH, "%02x", bytes[b]));
        return hstr.toString();
    }

    /**
     * Returns a 32-bit integer obtained from the bytes (in lex. order).
     * @param bytesArray
     * @return 32-bit integer
     */
    public static int bytes2Integer32(byte[] bytesArray) {
        int rint = 0;
        for (int b = 0; b < Math.min(bytesArray.length, 4); b++) {
            int rintMask = 0x000000ff << 4 * b;
            rint |= ((int) bytesArray[b]) & rintMask;
        }
        return rint;
    }

    /**
     * Reverses the order of the bytes in the array.
     * @param bytes
     * @return byte[] reversed array
     */
    public static byte[] reverseBytes(byte[] bytes) {
        byte[] revArray = new byte[bytes.length];
        for (int b = 0; b < revArray.length; b++)
            revArray[revArray.length - b - 1] = bytes[b];
        return revArray;
    }

    /**
     * Returns a byte with its bits reversed in lexocographical order.
     * @param b
     * @return byte reversed byte
     * @ref Utils.reverseBits (reverse hex representation of a byte array).
     */
    public static byte reverseBits(byte b) {
        int bint = (int) b;
        int rb = 0x00;
        int mask = 0x01 << 7;
        for (int s = 0; s < 4; s++) {
            rb = rb | ((bint & mask) >> (8 / (b + 1) - 1));
            mask = mask >>> 1;
        }
        mask = 0x01;
        for (int s = 0; s < 4; s++) {
            rb = rb | ((bint & mask) << (8 / (b + 1) - 1));
            mask = mask << 1;
        }
        return (byte) rb;
    }

    /**
     * Computes the reverse hex representation of the byte array.
     * @param bytes
     * @return byte[] reversed in initial order and byte-wise bits
     */
    public static byte[] reverseBits(byte[] bytes) {
        byte[] revBytes = reverseBytes(bytes);
        for (int b = 0; b < bytes.length; b++) {
            revBytes[b] = reverseBits(revBytes[b]);
        }
        return revBytes;
    }

    /**
     * Returns a standard timestamp of the current Android device's time.
     * @return String timestamp (format: %Y-%m-%d-%T)
     */
    public static String getTimestamp() {
        Time currentTime = new Time();
        currentTime.setToNow();
        return currentTime.format("%Y-%m-%d @ %T");
    }

    /**
     * Parses a CSV (comma delimited) file.
     * @param fdStream
     * @return List of String[] separated line entries
     * @throws IOException
     * @see ApduUtils
     * @see res/raw/*
     */
    public static List<String[]> readCSVFile(InputStream fdStream) throws IOException {
        List<String[]> csvLines = new ArrayList<String[]>();
        BufferedReader br = new BufferedReader(new InputStreamReader(fdStream));
        String csvLine;
        while ((csvLine = br.readLine()) != null) {
            String[] parsedRow = csvLine.split(",");
            csvLines.add(parsedRow);
        }
        fdStream.close();
        return csvLines;
    }

    /**
     * Determine whether an input string is in hex format.
     * @param str
     * @return boolean truth value
     */
    public static boolean stringIsHexadecimal(String str) {
        return str.matches("-?[0-9a-fA-F]+");
    }

    /**
     * Determine whether an input string is in purely decimal format.
     * @param str
     * @return boolean truth value
     */
    public static boolean stringIsDecimal(String str) {
        return str.matches("-?[0-9]+");
    }

    /**
     * Get random bytes seeded by the time. For use with generating random UID's.
     * @param numBytes
     * @return
     */
    public static byte[] getRandomBytes(int numBytes) {
        Random rnGen = new Random(System.currentTimeMillis());
        byte[] randomBytes = new byte[numBytes];
        for (int b = 0; b < numBytes; b++)
            randomBytes[b] = (byte) rnGen.nextInt(0xff);
        return randomBytes;
    }

    public static String trimString(String str, int maxNumChars) {
        if (str.length() <= maxNumChars)
            return str;
        return str.substring(0, maxNumChars) + "...";
    }

    /**
     * Computes a measure of entropy (i.e., how likely the payload data is to be encrypted) by
     * compressing the input byte array and comparing the resulting size (in bytes) to the
     * original array.
     * @param inputBytes
     * @return entropy rating
     */
    public static double computeByteArrayEntropy(byte[] inputBytes) {
        Deflater cmpr = new Deflater();
        cmpr.setLevel(Deflater.BEST_COMPRESSION);
        cmpr.setInput(inputBytes);
        cmpr.finish();
        int cmprByteCount = 0;
        while (!cmpr.finished()) {
            cmprByteCount += cmpr.deflate(new byte[1024]);
        }
        double entropyRatio = (double) cmprByteCount / inputBytes.length;
        Log.i(TAG, String.format(Locale.ENGLISH, "Compressed #%d bytes to #%d bytes ... Entropy ratio = %1.4g", inputBytes.length, cmprByteCount, entropyRatio));
        return entropyRatio;
    }

    public static int parseInt(String numberStr) {
        try {
            int rNum = Integer.parseInt(numberStr);
            return rNum;
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    /**
     * Pretty prints the DUMP_MFU command output according to this link:
     * https://www.manualslib.com/manual/815771/Advanced-Card-Acr122s.html?page=47#manual
     * https://shop.sonmicro.com/Downloads/MIFAREULTRALIGHT-UM.pdf
     * @param mfuBytes
     * @return Pretty String Format of the MFU tag
     */
    public static String prettyPrintMFU(String mfuBytes) {
        String pp = " PG | B0 B1 B2 B3 | LOCK AND/OR SPECIAL REGISTERS\n";
        pp += "=================================================\n";
        for (int page = 0; page < mfuBytes.length(); page += 8) {
            int pageNumber = page / 8;
            Log.i(TAG, String.format("prettyPrintMFU: page#% 2d, page=% 2d", pageNumber, page));
            byte[] pageData = Utils.hexString2Bytes(mfuBytes.substring(page, Math.min(page + 8, mfuBytes.length()) - 1));
            if (pageData.length < 4) {
                byte[] pageDataResized = new byte[4];
                System.arraycopy(pageData, 0, pageDataResized, 0, pageData.length);
                pageData = pageDataResized;
            }
            String specialRegs = "";
            int lockBits = 0;
            if (pageNumber == 0) {
                specialRegs = "SN0-2:BCC0";
            } else if (pageNumber == 1) {
                specialRegs = "SN3-6";
            } else if (pageNumber == 2) {
                specialRegs = "BCC1:INT:LOCK0-1";
                lockBits = (pageData[2] << 2) | pageData[3];
            } else if (pageNumber == 3) {
                specialRegs = "OTP0-3";
            } else if (pageNumber >= 4 && pageNumber <= 15) {
                int lockBit = (1 << (15 - pageNumber)) & 0x0000 & lockBits;
                specialRegs = (lockBit == 0) ? "UNLOCKED" : "NO ACCESS";
            } else if (pageNumber == 16) {
                specialRegs = "CFG0";
            } else if (pageNumber == 17) {
                specialRegs = "CFG1";
            } else if (pageNumber == 18) {
                specialRegs = "PWD0-3";
            } else if (pageNumber == 19) {
                specialRegs = "PACK0-1:RFU0-1";
            } else {
                specialRegs = "ONE WAY CTRS";
            }
            String pageLine = String.format(" % 2d | %02x %02x %02x %02x | [%s]", pageNumber, pageData[0], pageData[1], pageData[2], pageData[3], specialRegs);
            if (page + 4 < mfuBytes.length())
                pageLine += "\n";
            pp += pageLine;
        }
        return pp;
    }

    public UUID getUUIDFromInteger(int id) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = id & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }

    public static int getColorFromTheme(int colorResID) {
        return ThemesConfiguration.getThemeColorVariant(colorResID);
    }

    private static final int GPS_LONGITUDE_CINDEX = 0;
    private static final int GPS_LATITUDE_CINDEX = 1;

    public static String[] getGPSLocationCoordinates() {
        try {
            LocationManager locationManager = (LocationManager) LiveLoggerActivity.getInstance().getSystemService(Context.LOCATION_SERVICE);
            Location locGPSProvider = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locNetProvider = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            long gpsProviderLocTime = 0, netProviderLocTime = 0;
            if (locGPSProvider != null) {
                gpsProviderLocTime = locGPSProvider.getTime();
            }
            if (locNetProvider != null) {
                netProviderLocTime = locNetProvider.getTime();
            }
            Location bestLocProvider = (gpsProviderLocTime - netProviderLocTime > 0) ? locGPSProvider : locGPSProvider;
            String[] gpsAttrsArray = new String[]{
                    String.format(Locale.ENGLISH, "%g", bestLocProvider.getLatitude()),
                    String.format(Locale.ENGLISH, "%g", bestLocProvider.getLongitude())
            };
            return gpsAttrsArray;
        } catch(SecurityException secExcpt) {
            Log.w(TAG, "Exception getting GPS coords: " + secExcpt.getMessage());
            secExcpt.printStackTrace();
            return new String[] {
                    "UNK",
                    "UNK"
            };
        }
    }

    public static String getGPSLocationString() {
        String[] gpsCoords = Utils.getGPSLocationCoordinates();
        String gpsLocStr = String.format(Locale.ENGLISH, " -- Location at %s LONG, %s LAT -- ",
                gpsCoords[Utils.GPS_LONGITUDE_CINDEX], gpsCoords[Utils.GPS_LATITUDE_CINDEX]);
        return gpsLocStr;
    }

    public static void displayToastMessage(String toastMsg, int msgDuration) {
        Toast toastDisplay = Toast.makeText(LiveLoggerActivity.getInstance(), toastMsg, msgDuration);
        toastDisplay.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
        int toastBackgroundColor = Utils.getColorFromTheme(R.attr.colorPrimaryDark);
        toastDisplay.getView().getBackground().setColorFilter(toastBackgroundColor, PorterDuff.Mode.SRC_IN);
        toastDisplay.getView().setAlpha(0.70f);
        toastDisplay.show();
        Log.i(TAG, "TOAST MSG DISPLAYED: " + toastMsg);
    }

    public static void displayToastMessageShort(String toastMsg) {
         Utils.displayToastMessage(toastMsg, Toast.LENGTH_SHORT);
    }

    public static void displayToastMessageLong(String toastMsg) {
        Utils.displayToastMessage(toastMsg, Toast.LENGTH_LONG);
    }

}