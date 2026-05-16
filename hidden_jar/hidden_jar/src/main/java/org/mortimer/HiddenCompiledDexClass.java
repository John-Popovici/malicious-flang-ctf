package org.mortimer;

import java.util.Arrays;

public class HiddenCompiledDexClass {

    private static final char separator = '_';
    private static final char[] jumpPlus = {'I', 'r', 'k', 'b', 'k', 'x'};
    private static final char[] arrayOfCharsAgainWithATwist = {'s', 'I'};
    private static final String keepGoing = "ThePassword";
    private static final char[] arrayOfChars = {'t', 'h', 'i', 's'};
    private static final char[] slightChangeAgain = {'?', 'o', 'n'};
    private static final byte[] whatAreThese = {50, 36, 40, 52, 50, 27, 29, 12, 12, 62, 6, 44, 23, 22, 27, 19, 59, 54, 45, 51, 4, 41, 0, 28, 60, 1, 11, 90, 16, 55, 13, 22, 49, 44, 23, 4, 4, 35, 18, 44, 39, 58, 18};

    //Expected to be unused, as will be called from outside the class
    @SuppressWarnings("unused")
    public static String checkParameter(String parameter) {
        if (parameter.equals(Arrays.toString(arrayOfChars)
                + arrayOfCharsAgainWithATwist[1]
                + arrayOfCharsAgainWithATwist[0]
                + keepGoing
                + separator
                + letsJumpAgain(Arrays.toString(jumpPlus), -6)
                + separator
                + slightChangeAgain[2]
                + slightChangeAgain[1]
                + slightChangeAgain[0])) {
            return new String(mergeThemTogether(whatAreThese, parameter.toCharArray()));
        }
        return "\"Game over, man! Game over!\" - Hicks, Aliens";
    }

    private static String letsJumpAgain(String text, @SuppressWarnings("SameParameterValue") int shift) {
        StringBuilder result = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (Character.isLetter(character)) {
                char base = Character.isUpperCase(character) ? 'A' : 'a';
                character = (char) (base + (character - base + shift) % 26);
            }
            result.append(character);
        }
        return result.toString();
    }

    protected static byte[] mergeThemTogether(byte[] inputData, char[] password) {
        byte[] output = new byte[inputData.length];
        for (int i = 0; i < inputData.length; i++) {
            char keyChar = password[i % password.length];
            output[i] = (byte) (inputData[i] ^ keyChar);
        }

        return output;
    }
}
