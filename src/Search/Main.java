package Search;

import org.tartarus.snowball.ext.PorterStemmer;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.*;

class Main
{

    private static ExecutorService executor=null;
    private static PorterStemmer stemmer=new PorterStemmer();
    public static CountDownLatch latch;
    private static boolean noCorpus = true;



    public static void submitJob(Runnable job)                                      //submitting job to the thread pool
    {
        executor.execute(job);
    }

    public static String stem(String input)                                         //call to Porter Stemmer
    {
        stemmer.setCurrent(input);
        stemmer.stem();
        return stemmer.getCurrent();
    }

    public static void showDocument(String docId) throws IOException                //To view the Document the user requested
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(new File("corpus/"+docId)), "UTF-8"));
        while(true)
        {
            String line = reader.readLine();
            if(line==null)
                break;
            System.out.println(line);
        }
    }

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException
    {
        GlobalPosIndex globalPosIndex=new GlobalPosIndex();
        GlobalBiWordIndex globalBiWordIndex=new GlobalBiWordIndex();
        QueryProcessor qp=new QueryProcessor(globalPosIndex,globalBiWordIndex);
        Scanner reader = new Scanner(System.in);
        boolean moreInput=true;
        String in;

        System.out.println("...MY SEARCH ENGINE...");
        System.out.println(":q");
        System.out.println(":stem <token>");
        System.out.println(":index <dir_path>");
        System.out.println(":vocab");
        System.out.println(":open <docName>");
        System.out.println("<query>");
        System.out.println(":corpusFromJSON <json filepath>");
        do
        {

            System.out.println("Enter an input :");
            System.out.println();

            in = reader.nextLine().trim();

            String[] command = in.split("\\s+",2);

            switch(command[0].toLowerCase())                        //Switch Menu to Search Engine
            {
                case ":q": moreInput=false;
                    break;
                case ":stem":
                    if(command.length<2)
                    {
                        System.err.println("No word found to Stem..!!");
                        break;
                    }
                    System.out.println("Stem Value:\n"+stem(command[1]));
                    break;

                case ":open" :
                    if(command.length>1)
                    {
                        showDocument(command[1]);
                    }
                    else
                        System.err.println("Missing Document name parameter!!");
                    break;

                case ":index":
                    globalBiWordIndex.clear();
                    globalPosIndex.clear();
                    if(command.length>1)
                    {
                        executor = Executors.newFixedThreadPool(200);
                        CorpusFromDirectory corpusFromDirectory = new CorpusFromDirectory(globalPosIndex, globalBiWordIndex, command[1]);
                        corpusFromDirectory.start();
                        executor.shutdown();
                        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
                        noCorpus=false;
                    }
                    else
                        System.err.println("Missing filepath parameter!!");
                    break;

                case ":corpusfromjson":
                    globalBiWordIndex.clear();
                    globalPosIndex.clear();
                    if(command.length<2)
                    {
                        System.err.println("Missing filepath parameter!!");
                        break;
                    }
                    executor = Executors.newFixedThreadPool(200);
                    JsonStreamParser parser = new JsonStreamParser(globalPosIndex,globalBiWordIndex,command[1]);
                    parser.start();
                    executor.shutdown();
                    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
                    noCorpus=false;
                    break;

                case ":vocab":
                    globalPosIndex.showVocab();
                    break;

                default:
                    if(noCorpus) {
                        System.out.println("Corpus Undefined..!\ntry \n:corpusFromJSON\nOR\n:index <directory_path>\n");
                        break;
                    }
                    ArrayList<Integer[]> result = qp.parseCompoundQuery(in.toLowerCase());
                    System.out.println();
                    System.out.println("Search Results : ");
                    for(Integer[] x : result)
                    {
                        System.out.println(x[0].intValue()+".txt");
                    }
                    System.out.println("Documents retrieved : "+ result.size());
                    System.out.println();
                    break;
            }
        }while(moreInput);
    }
}