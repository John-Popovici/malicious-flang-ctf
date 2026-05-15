package org.mortimer;

public class Main {

    private static final String password = "thisIsthePassword_Clever_no?";
    private static final String secret_flag = "FLAG{hiding_data_in_a_encoded_dex_class_TM}";


    public static void main(String[] args) {
        char[] passwordChar = password.toCharArray();

        byte[] encryptedBytes = HiddenCompiledDexClass.xorCipher(secret_flag.getBytes(), passwordChar);

        System.out.print("Encrypted Byte Array: { ");
        for (byte b : encryptedBytes) {
            System.out.print(b + ", ");
        }
        System.out.println("}");
    }
}
