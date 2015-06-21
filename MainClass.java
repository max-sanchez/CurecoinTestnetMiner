import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.*;

public class MainClass
{
    //private static final String WORK_SERVER_HOST = "155.94.254.14";
    private static final int WORK_SERVER_PORT = 10000;
    private static final String WORK_SERVER_HOST = "127.0.0.1";

    private static final String CURECOIN_DAEMON_HOST = "127.0.0.1";
    private static final int CURECOIN_DAEMON_PORT = 8016;

    public static void main(String[] args)
    {
        //launch();
        Scanner scan = new Scanner(System.in);
        System.out.println("How many threads would you like to run?");
        String threadNumString = scan.nextLine();
        while (!isInteger(threadNumString))
        {
            System.out.println("Please enter a valid integer. How many threads would you like to run?");
            threadNumString = scan.nextLine();
        }
        String potentialError = ""; //Weird way of printing error messages, but it works.
        int blocksMined = 0;
        try
        {
            System.out.println("Connecting to 2.0.0a1 daemon on " + CURECOIN_DAEMON_HOST + ":" + CURECOIN_DAEMON_PORT + "...");
            potentialError = "Unable to connect to daemon! Make sure Curecoin 2.0.0a1 daemon is running on default port 8016.";
            Socket socketToDaemon = new Socket(CURECOIN_DAEMON_HOST, CURECOIN_DAEMON_PORT);
            System.out.println("Connected to daemon successfully!");
            BufferedReader inFromDaemon = new BufferedReader(new InputStreamReader(socketToDaemon.getInputStream()));
            PrintWriter outToDaemon = new PrintWriter(socketToDaemon.getOutputStream(), true);
            potentialError = "";
            inFromDaemon.readLine(); //Consume welcome message
            outToDaemon.println("getinfo");
            String defaultAddress = "";
            String inputFromDaemon = inFromDaemon.readLine();
            while (!inputFromDaemon.contains("Main address balance") && !inputFromDaemon.contains("<EOT>"))
            {
                if (inputFromDaemon.contains("Main address:"))
                {
                    defaultAddress = inputFromDaemon.split(" ")[2];
                    break;
                }
                inputFromDaemon = inFromDaemon.readLine();
            }
            System.out.println("Daemon gave address: " + defaultAddress);
            System.out.println("Connecting to work server on " + WORK_SERVER_HOST + ":" + WORK_SERVER_PORT + "...");
            potentialError = "Unable to connect to work server!";
            Socket socketToWorkServer = new Socket(WORK_SERVER_HOST, WORK_SERVER_PORT);
            BufferedReader inFromWorkServer = new BufferedReader(new InputStreamReader(socketToWorkServer.getInputStream()));
            PrintWriter outToWorkServer = new PrintWriter(socketToWorkServer.getOutputStream(), true);
            potentialError = "";
            System.out.println("Beginning to mine for address " + defaultAddress);
            MinerThread[] minerThreads = new MinerThread[Integer.parseInt(threadNumString)];
            while (true)
            {
                System.out.println("Reading new challenge...");
                String challenge = inFromWorkServer.readLine();
                System.out.println("New challenge: " + challenge);
                for (int i = 0; i < minerThreads.length; i++)
                {
                    minerThreads[i] = new MinerThread(challenge);
                    minerThreads[i].start();
                }
                String solution = "";
                String solutionHash = "";
                double hashRate;
                while (solution.equals(""))
                {
                    hashRate = 0.0D;
                    for (int i = 0; i < minerThreads.length; i++)
                    {
                        hashRate += minerThreads[i].hashesPerSecond;
                        if (!minerThreads[i].messageToMaster.equals(""))
                        {
                            solution = minerThreads[i].messageToMaster;
                            solutionHash = minerThreads[i].hashOfSolution;
                            for (int j = 0; j < minerThreads.length; j++)
                            {
                                minerThreads[j].shouldContinue = false;
                            }
                            break;
                        }
                    }
                    System.out.println("Current hash rate: " + (int)hashRate/1000 + " KH/s of " + challenge.split(",")[1] + " x SHA256");
                    System.out.println("     Mined " + blocksMined + " blocks.");
                    Thread.sleep(500);
                }
                System.out.println("SHA256(" + solution + ", " + challenge.split(",")[1] + ") = " + solutionHash);
                System.out.println("Submitting " + solution + " to work server...");
                outToWorkServer.println(solution + "," + defaultAddress);
                String certificate = inFromWorkServer.readLine();
                if (!certificate.contains("Incorrect solution. Sending new challenge..."))
                {
                    System.out.println("Certificate sent to daemon!");
                    System.out.println("Certificate: ");
                    System.out.println(certificate);
                    //Submit certificate to daemon
                    outToDaemon.println("submitcert " + certificate);
                }
                else
                {
                    System.out.println("Work was incorrect!");
                }
                //Consume response
                String result = inFromDaemon.readLine();
                if (result.contains("failed"))
                {
                    System.out.println(inFromDaemon.readLine()); //Target failure
                }
                else
                {
                    blocksMined++;
                    System.out.println(inFromDaemon.readLine()); //Earned target score x
                    System.out.println(inFromDaemon.readLine()); //Below target y
                }
            }
        } catch (Exception e)
        {
            if (potentialError.equals(""))
            {
                e.printStackTrace();
            }
            else
            {
                System.out.println(potentialError);
            }
        }
    }

    public static boolean isInteger(String toTest)
    {
        try
        {
            Integer.parseInt(toTest);
            return true;
        } catch (Exception e)
        {
            return false;
        }
    }

    public static void launch()
    {
        Console console = System.console(); //Get a system console object
        if (console != null) //If the application has a console
        {
            File f = new File("launch.bat");
            if (f.exists())
            {
                f.delete(); //delete bat file if it exists
            }
        } 
        else if (!GraphicsEnvironment.isHeadless()) //Application doesn't have a console, let's give it one!
        {
            String os = System.getProperty("os.name").toLowerCase(); //Get OS
            if (os.contains("indows")) //If OS is a windows OS
            { 
                try
                {
                    File JarFile = new File(MainClass.class.getProtectionDomain().getCodeSource().getLocation().toURI());//Get the absolute location of the .jar file
                    PrintWriter out = new PrintWriter(new File("launch.bat")); //Get a PrintWriter object to make a batch file
                    out.println("@echo off"); //turn echo off for batch file
                    out.println("title Curecoin 2.0.0a1 Testnet Miner"); 
                    out.println("java -jar \"" + JarFile.getPath() + "\"");
                    out.println("start /b \"\" cmd /c del \"%~f0\"&exit /b");
                    out.close(); //saves file
                    Runtime rt = Runtime.getRuntime(); //gets runtime
                    rt.exec("cmd /c start launch.bat"); //executes batch file
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
                System.exit(0); //Exit program, so only instance of program with command line runs!
            }
        }
    }
}
