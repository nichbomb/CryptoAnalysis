package example;


import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

public final class DoNotUseWeakSeed1 {

  // The misuse to avoid here is use low-entropy seeds
  
  public static void main(String[] args) {
    try {
      SecureRandom r1 = SecureRandom.getInstance("SHA1PRNG", "SUN");
      SecureRandom r2 = SecureRandom.getInstanceStrong();
      r1.setSeed(r2.nextLong()); // 64 bits can be a lower bound 
      
      // this is for exercising the algorthms 
      for (int i = 0; i < 10; i++) {
        if (i == 0) {   System.out.println("i , r1 , r2"); }
        System.out.println(i+","+r1. nextInt(10000)+","+r2.nextInt(10000));
      }
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) 
    { System.out.println(e);
    }
  }

}
