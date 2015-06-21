import java.util.*;
import java.security.*;
import javax.xml.bind.DatatypeConverter;

public class MinerThread extends Thread
{
    private String challenge = "";
    public String messageToMaster = "";
    public String hashOfSolution = "";
    public boolean shouldContinue = true;
    public double hashesPerSecond = 0;

    public MinerThread(String challenge)
    {
        this.challenge = challenge;
    }

    public void run()
    {
        try
        {
            String prefix = challenge.split(",")[0];
            int numLoops = Integer.parseInt(challenge.split(",")[1]);
            int incrementMe = new Random().nextInt(Integer.MAX_VALUE);
            //shouldContinue set to false by master when work is old/stale.
            long lastTimestamp = System.currentTimeMillis();
            int lastIncrementMe = incrementMe;
            while (shouldContinue)
            {
                if(SHA256(prefix + incrementMe, numLoops).substring(0, 6).equals("000000"))
                {
                    hashOfSolution = SHA256(prefix + incrementMe, numLoops);
                    messageToMaster = prefix + incrementMe;
                    shouldContinue = false;
                }
                incrementMe++;
                if (incrementMe - 10000 == lastIncrementMe)
                {
                    lastIncrementMe = incrementMe;
                    double secondsPassed = ((double)System.currentTimeMillis() - (double)lastTimestamp)/1000;
                    hashesPerSecond = 10000/secondsPassed;
                    lastTimestamp = System.currentTimeMillis();
                }
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public String SHA256(String toHash, int rounds)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String hash = DatatypeConverter.printHexBinary(md.digest(toHash.getBytes("UTF-8")));
            for (int i = 1; i < rounds; i++)
            {
                hash = DatatypeConverter.printHexBinary(md.digest(hash.getBytes("UTF-8")));
            }
            return hash;
        } catch (Exception e)
        {
            return null;
        }
    }

}
