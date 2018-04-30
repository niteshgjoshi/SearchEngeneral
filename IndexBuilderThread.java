package Search;

import org.tartarus.snowball.ext.PorterStemmer;

import java.util.*;


/**
 * Created by chimera on 9/11/17.
 */
class IndexBuilderThread implements Runnable
{

    private final Document doc;
    private final GlobalPosIndex globalPosIndex;
    private final GlobalBiWordIndex globalBiWordIndex;
    private final int docId;
    private PorterStemmer stemmer;

    public IndexBuilderThread(GlobalPosIndex globalPosIndex, GlobalBiWordIndex globalBiWordIndex, Document doc)             //Constructor
    {
        this.doc = doc;
        this.docId = doc.getId();
        this.globalPosIndex = globalPosIndex;
        this.stemmer = new PorterStemmer();
        this.globalBiWordIndex = globalBiWordIndex;
    }

    private String stem(String input)                                           //method utilizes Porter Stemmer class to stem the input tokens
    {
        stemmer.setCurrent(input);
        stemmer.stem();
        return stemmer.getCurrent();
    }

    @Override
    public void run()                                                           //method enables thread runs to add the stemmed token to the desired the Position-Indexes
    {

        Map<String, ArrayList<Integer>> posIndex = new HashMap<String, ArrayList<Integer>>();
        HashSet<String> biIndex = new HashSet<String>();
        LinkedList<String> biIndexFifo = new LinkedList<String>();
        LinkedList<String> biSubIndexFifo = new LinkedList<String>();
        String[] tokens = doc.getBody().split("\\s+");
        String[] token_arr;

        ArrayList<Integer> tempList;
        ArrayList<Integer> newList = new ArrayList<Integer>();
        String token;
        String trimmedToken;

        Integer i = 0;


        for (String tempToken : tokens)
        {
            trimmedToken = tempToken.toLowerCase().replaceAll("^\\W+|\\W+$|'", "");
            token_arr = trimmedToken.split("\\s*(-|\\.|\\s*,\\s*)\\s*");
            if (trimmedToken.isEmpty())
            {
                continue;
            }

            if (token_arr.length > 1)
            {
                int j = 0;
                for (String subToken : token_arr)
                {
                    trimmedToken = subToken.replaceAll("^\\W+|\\W+$|'", "");

                    token = stem(trimmedToken);
                    if (token.isEmpty())
                    {
                        continue;
                    }

                    biIndexFifo.add(token);
                    if (biSubIndexFifo.size() == 3)
                    {
                        biSubIndexFifo.removeFirst();
                        biIndex.add(String.join(" ", biSubIndexFifo));

                    }

                    if (posIndex.containsKey(token))
                    {
                        tempList = posIndex.get(token);
                        tempList.add(i + j);
                    }
                    else
                    {
                        newList.clear();
                        newList.add(docId);
                        newList.add(i + j);
                        posIndex.put(token, (ArrayList<Integer>) newList.clone());
                    }
                    j++;
                }
            }
            token = stem(String.join("", token_arr));
            if (token.isEmpty())
            {
                continue;
            }


            biIndexFifo.add(token);
            if (biIndexFifo.size() == 3)
            {
                biIndexFifo.removeFirst();
                biIndex.add(String.join(" ", biIndexFifo));
            }

            if (posIndex.containsKey(token))
            {
                tempList = posIndex.get(token);
                tempList.add(i);
            }
            else
            {
                newList.clear();
                newList.add(docId);
                newList.add(i);
                posIndex.put(token, (ArrayList<Integer>) newList.clone());
            }
            i++;
        }

        Map.Entry<String, ArrayList<Integer>> key_val;
        for (Object o : posIndex.entrySet())
        {
            key_val = (Map.Entry) o;
            tempList = key_val.getValue();

            globalPosIndex.add(key_val.getKey(), tempList.toArray(new Integer[tempList.size()]));
        }

        for (String key : biIndex)
        {
            globalBiWordIndex.add(key, docId);
        }
    }
}