package Search;

import org.tartarus.snowball.ext.PorterStemmer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chimera on 9/25/17.
 */


//Query Processing Class
public class QueryProcessor
{
    private final GlobalPosIndex globalPosIndex;
    private final GlobalBiWordIndex globalBiWordIndex;
    private final PorterStemmer stemmer;
    private HashMap<String, ArrayList<Integer[]>> queryMap;

    public QueryProcessor(GlobalPosIndex globalPosIndex, GlobalBiWordIndex globalBiWordIndex)               //Constructor
    {
        this.globalPosIndex = globalPosIndex;
        this.globalBiWordIndex = globalBiWordIndex;
        this.stemmer = new PorterStemmer();
    }

    /**
     * Method will parse query and extract all literals and populate positiveLiterals, negativeLiterals,
     * positivePhraseQueries, and negativePhraseQuries.
     *
     * @param query
     */


    public ArrayList<Integer[]> parseCompoundQuery(String query)                                    //Expanded Query implementation i.e. nested queries of AND and OR
    {
        Pattern pattern = Pattern.compile("^(.*?)\\(([^\\(\\)]*)\\)(.*?)$");
        queryMap=new HashMap<String, ArrayList<Integer[]>>();
        System.out.println(query);

        while(query.contains("("))
        {
            Matcher matcher = pattern.matcher(query);
            if(matcher.find()) {

                String extractedQuery = matcher.group(2);
                queryMap.put("^_^" + extractedQuery.hashCode(), parseSimpleQuery(extractedQuery));
                query = matcher.group(1) + " ^_^" + extractedQuery.hashCode() + " " + matcher.group(3);
                System.out.println(query);
            }
            else
            {
                System.err.println("Invalid Query!!");
                break;
            }
        }

        System.out.println(query);
        return parseSimpleQuery(query);
    }

    public ArrayList<Integer[]> parseSimpleQuery(String query)                                  //Implementation of user input queries
    {
        //split query on the basis of '+'
        //for each part then,
        //check if there is " present, find the closing quotes.
        //if there is closing quotes, ignore any symbol present in the phrase.
        //if there is no closing quotes, ignore opening quote.
        //populate all lists.
        //Keep ORing the results hence obtained.


        ArrayList<Integer[]> docList;
        String[] subQueries = query.split("\\s*\\+\\s*");
        ArrayList<ArrayList<Integer[]>> toBeOrEdList=new ArrayList<>();
        for(String subQuery:subQueries)
        {
            toBeOrEdList.add(processOrLessQuery(subQuery));
        }

        docList=orOperation(toBeOrEdList);

        return docList;
    }

    private ArrayList<Integer[]> processOrLessQuery(String subQuery)                            //Method to process queries that do not contain any OR querying
    {
        ArrayList<String> queryBuffer=new ArrayList<String>();
        ArrayList<String> AtomicQueries=new ArrayList<String>();
        boolean phraseBuffFlag=false;
        for(String subAtomicQuery:subQuery.trim().split("\\s+"))
        {
            subAtomicQuery=subAtomicQuery.trim();

            if (subAtomicQuery.matches("[^\"]*\"[^\"]*"))
            {
                if(phraseBuffFlag==false)
                {
                    phraseBuffFlag=true;
                }
                else
                {
                    phraseBuffFlag=false;
                    queryBuffer.add(subAtomicQuery);
                    AtomicQueries.add(0,String.join(" ",queryBuffer));
                    queryBuffer.clear();
                    continue;
                }
            }
            if (phraseBuffFlag==true)
            {
                queryBuffer.add(subAtomicQuery);
            }
            else
            {
                AtomicQueries.add(subAtomicQuery);
            }
        }

        ArrayList<ArrayList<Integer[]>> negQueriesPosList = new ArrayList();
        ArrayList<ArrayList<Integer[]>> posQueriesPosList = new ArrayList();

        for (String q:AtomicQueries)
        {
            if(q.startsWith("-"))
            {
                if(q.matches("^-?\\^_\\^.*"))
                {
                    negQueriesPosList.add(queryMap.get(q.substring(1)));
                }
                else
                {
                    negQueriesPosList.add(query(q.substring(1)));
                }

            }
            else
            {
                if(q.startsWith("^_^"))
                {
                    posQueriesPosList.add(queryMap.get(q));
                }
                else
                {
                    posQueriesPosList.add(query(q));
                }
            }
        }

        if(posQueriesPosList.size()<1)
        {
            System.err.println("!! Invalid Query !!");
            return new ArrayList<>();
        }
        ArrayList<Integer[]> posuniou=andOperation(posQueriesPosList);
        ArrayList<Integer[]> notunion =orOperation(negQueriesPosList);
        return notOperation(posuniou,notunion);
    }

    private String stem(String input)                                                   //Stemmer call to stem the token
    {
        stemmer.setCurrent(input);
        stemmer.stem();
        return stemmer.getCurrent();
    }



    public ArrayList<Integer[]> query(String token)                                     //method to recognize, fetch and forward phrased queries
    {
        if(token.startsWith("\""))
        {
            return queryPhrase(token);
        }
        else
        {
            token=token.replaceAll("^\\W+|\\W+$|'","");
            return globalPosIndex.get(stem(token));
        }
    }


    public ArrayList<Integer[]> queryPhrase(String phrase)                              //implements phrased query along with Near/k
    {

        int nearK=1;

        Pattern pattern = Pattern.compile("^(.*?)\\bnear/(\\d+)\\b(.*?)$");             //regex to find Near/k within the phrased query
        Matcher matcher = pattern.matcher(phrase);
        if(matcher.find())
        {
            nearK = Integer.parseInt(matcher.group(2));
            phrase =matcher.group(1)+" "+matcher.group(3);
        }

        String[] phraseTokens = phrase.replaceAll("^\"*|\"*$", "").split("\\s+");


        for(int i=0;i<phraseTokens.length;i++)
        {
            String temp=phraseTokens[i].toLowerCase().replaceAll("^\\W*|\\W*$","");
            phraseTokens[i]=this.stem(temp);
        }

        if(phraseTokens.length==2 && nearK==1)                                          //to decipher a BiWord Query as Two tokens with Near/K set to 1
        {
            String biWordKey=String.join(" ",phraseTokens);
            return globalBiWordIndex.getBiwordPosting(biWordKey);
        }

        HashSet<Integer> docsToBeScanned = new HashSet<>();

        ArrayList<Integer[]> andResult = query(phraseTokens[0]);
        for (int i = 1; i < phraseTokens.length; i++)
        {
            andResult = andOperation(andResult, phraseTokens[i]);
        }

        if (andResult.size() == 0)
        {
            return null;
        }

        for (Integer[] posting : andResult)
        {
            docsToBeScanned.add(posting[0]);
        }

        ArrayList<ArrayList<Integer[]>> queryTokenIndex = new ArrayList<>(phraseTokens.length);


        for (int i = 0; i < phraseTokens.length; i++)
        {
            ArrayList<Integer[]> andPosList=new ArrayList<>();
            for(Integer[] posting:query(phraseTokens[i]))
            {
                if(docsToBeScanned.contains(posting[0]))
                {
                    andPosList.add(posting);
                }
            }
            queryTokenIndex.add(andPosList);
        }


        int andDocsSize=queryTokenIndex.get(0).size();


        ArrayList<Integer[]> result=new ArrayList<>();
        for(int docNo=0;docNo<andDocsSize;docNo++)
        {
            ArrayList<Integer[]> allTermDocs=new ArrayList<>(queryTokenIndex.size());
            for(ArrayList<Integer[]> phraseToken :queryTokenIndex)
            {
                allTermDocs.add(phraseToken.get(docNo));
            }
            Integer[] basePositions=allTermDocs.get(0);
            for(int basePosIndex=1;basePosIndex<basePositions.length;basePosIndex++)
            {
                int basePos=basePositions[basePosIndex];

                int offset=0;
                boolean phraseFound=true;
                for (Integer[] termDoc:allTermDocs)
                {
                    boolean found=false;
                    for(int x=1;x<termDoc.length;x++)
                    {
                        for(int k=0;k<nearK;k++)
                        {

                            if(termDoc[x]==basePos+offset+k)
                            {
                                found=true;
                                break;
                            }
                        }
                        if(found)
                        {
                            break;
                        }
                    }
                    phraseFound=found && phraseFound;
                    offset++;
                }
                if(phraseFound)
                {
                    result.add(basePositions);
                    break;
                }
            }
        }
        return result;
    }

    /*All OR Operation returns ArrayList of Integer Array.
    *Where every ArrayList object depicts the Document and its Integer Array has the first entry as the docID and rest as the positions
    *In order to maintain uniformity between all postings we kept the postings in this structure
    **/
    public ArrayList<Integer[]> orOperation(String token1, String token2)                               //queries the left and right portion of the or operator and delegated then to query method
    {
        return orOperation(query(token1), query(token2));
    }

    public ArrayList<Integer[]> orOperation(ArrayList<Integer[]> docIds1, ArrayList<Integer[]> docIds2)     //or operation to return the list of documents in order form after OR processing
    {
        ArrayList<Integer[]> output = new ArrayList<Integer[]>();
        int index1=0, index2 = 0;
        if(docIds1 == null && docIds2 == null)
        {
            return output;
        }
        else if(docIds1 == null)
        {
            return docIds2;
        }
        else if(docIds2 == null)
        {
            return docIds1;
        }
        while(index1 < docIds1.size() && index2 < docIds2.size())
        {
            if(docIds1.get(index1)[0].equals(docIds2.get(index2)[0]))
            {
                output.add(docIds1.get(index1));
                index1++;
                index2++;
            }
            else if(docIds1.get(index1)[0].intValue() < docIds2.get(index2)[0].intValue())
            {
                output.add(docIds1.get(index1));
                index1++;
            }
            else
            {
                output.add(docIds2.get(index2));
                index2++;
            }
        }

        while(index2< docIds2.size())
        {
            output.add(docIds2.get(index2));
            index2++;
        }
        while(index1 < docIds1.size())
        {
            output.add(docIds1.get(index1));
            index1++;
        }

        return output;
    }

    public ArrayList<Integer[]> orOperation(ArrayList<Integer[]> docIds, String token)                      //doc list with token OR operation
    {
        ArrayList<Integer[]> queryResults = query(token);
        return orOperation(docIds, queryResults);
    }

    public ArrayList<Integer[]> orOperation(ArrayList<ArrayList<Integer[]>> postingsList)                   //caller to OR operation
    {
        ArrayList<Integer[]> output = new ArrayList<Integer[]>();
        if(null == postingsList || postingsList.size() == 0) {
            return output;
        } else {
            for(ArrayList<Integer[]> thisPosting : postingsList) {
                output = orOperation(output, thisPosting);
            }
        }
        return output;
    }

    /*All Not Operation returns ArrayList of Integer Array.
    *Where every ArrayList object depicts the Document and its Integer Array has the first entry as the docID and rest as the positions
    *In order to maintain uniformity between all postings we kept the postings in this structure
    **/

    public ArrayList<Integer[]> notOperation(ArrayList<Integer[]> docIds1, ArrayList<Integer[]> docIds2)    //method to update the doc list by removing docs of not query from the already existing doc list
    {
        ArrayList<Integer[]> output = new ArrayList<Integer[]>();
        int index1 = 0, index2 = 0;
        if(docIds2==null || docIds2.size()<1)
        {
            return docIds1;
        }

        while(index1 < docIds1.size() && index2<docIds2.size())
        {
            if(docIds1.get(index1)[0].equals(docIds2.get(index2)[0]))
            {
                index1++;
                index2++;
            }
            else if (docIds1.get(index1)[0].intValue() < docIds2.get(index2)[0].intValue())
            {
                output.add(docIds1.get(index1));
                index1++;
            }
            else
            {
                index2++;
            }
        }

        while(index1 < docIds1.size())
        {
            output.add(docIds1.get(index1));
            index1++;
        }
        return output;
    }

    public ArrayList<Integer[]> notOperation(ArrayList<Integer[]> docIds, String token)             //method to shoot the Not operation with already accumalated Doc list with the not query
    {
        return notOperation(docIds, query(token));
    }

    public ArrayList<Integer[]> andOperation(String token1, String token2)
    {
        return andOperation(query(token1), query(token2));
    }

    /*All And Operation returns ArrayList of Integer Array.
    *Where every ArrayList object depicts the Document and its Integer Array has the first entry as the docID and rest as the positions
    *In order to maintain uniformity between all postings we kept the postings in this structure
    **/

    public ArrayList<Integer[]> andOperation(ArrayList<ArrayList<Integer[]>> postingsList)          //caller to AND operation
    {
        ArrayList<Integer[]> output = postingsList.get(0);
        //Do nothing for andOperation on single token.
        if(null == postingsList || postingsList.size() <= 1)
        {
            return output;
        }
        else
        {
            for(int i=1;i<postingsList.size();i++)
            {
                output = andOperation(output, postingsList.get(i));
            }
        }
        return output;
    }

    public ArrayList<Integer[]> andOperation(ArrayList<Integer[]> docIds1, ArrayList<Integer[]> docIds2)        //method to update the doc list by finding union of docs from the already existing doc list
    {
        ArrayList<Integer[]> output = new ArrayList<Integer[]>();
        int index1=0, index2 = 0;


        if (docIds1==null || docIds2==null)
        {
            return output;
        }


        while(index1 < docIds1.size() && index2 < docIds2.size())
        {
            if(docIds1.get(index1)[0].equals(docIds2.get(index2)[0]))
            {
                output.add(docIds1.get(index1));
                index1++;
                index2++;
            }
            else if(docIds1.get(index1)[0].intValue() < docIds2.get(index2)[0].intValue())
            {
                index1++;
            }
            else
            {
                index2++;
            }
        }
        return output;
    }

    public ArrayList<Integer[]> andOperation(ArrayList<Integer[]> docIds, String token)                     //method to shoot the AND operation with already accumulated Doc list
    {
        ArrayList<Integer[]> queryResults = query(token);
        return andOperation(docIds, queryResults);
    }
}
