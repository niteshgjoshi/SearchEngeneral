package Search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chimera on 9/25/17.
 */

public class GlobalBiWordIndex
{
    private final Map<String,ArrayList<Integer>> universalIndex;
    private ArrayList<Integer> tempList;
    private ArrayList<Integer> newList;

    public GlobalBiWordIndex()                                                  //Constructor
    {
        /*The structure used is a HashMap with keys as token of String type and values as ArrayList of Integer Array
        * where the ArrayList object depicts the Document and its Integer Array has the first entry as the docID and rest as the positions*/
        universalIndex = Collections.synchronizedMap(new HashMap<String,ArrayList<Integer>>());

        tempList =new ArrayList<Integer>();
        newList =new ArrayList<Integer>();

    }

    public void clear()                             //to refresh the Positional Index
    {
        universalIndex.clear();
    }

    public ArrayList<Integer[]> getBiwordPosting(String key)                    //Get the posting list from the BiWord index
    {
        ArrayList<Integer[]> returnVal=new ArrayList<>();

        ArrayList<Integer> docIds =universalIndex.get(key);

        for(Integer docId:docIds)
        {
            Integer[] tempPostingEntry=new Integer[1];
            tempPostingEntry[0]=docId;
            returnVal.add(tempPostingEntry);

        }
        return returnVal;
    }

    public void add(String key, Integer docID)                                  //token insertion into data structure as keys
    {
        synchronized (universalIndex)
        {
            if(universalIndex.containsKey(key))
            {
                tempList =  universalIndex.get(key);
                this.binInsert(tempList, docID);
            }
            else
            {
                newList.clear();
                newList.add(docID);
                universalIndex.put(key, (ArrayList<Integer>) newList.clone());
            }

        }
    }

    private ArrayList binInsert(ArrayList<Integer> tempListP, Integer docID)                //Insertion using Binary Search to sequentially arrange doc ids in ascending order
    {
        Integer insertPos;
        if(tempListP.size()==1)
        {
            Integer firstVal= tempListP.get(0);
            if(firstVal>docID)
            {
                insertPos=0;
            }
            else
            {
                insertPos=1;
            }
        }
        else
        {
            insertPos = searchSortedInsertPos(tempListP,0,tempListP.size()-1,docID);
        }


        tempListP.add(insertPos,docID);
        return tempListP;
    }

    private int searchSortedInsertPos(ArrayList<Integer> tempListP, int startP, int endP, Integer docId)            //insertion of token position in the data structure as in the document
    {
        if(endP==startP+1)
        {
            if(tempListP.get(endP)<docId)
            {
                return endP+1;
            }
            else
            {
                return endP;
            }
        }

        int midL=(startP+endP)/2;
        Integer midVal=tempListP.get(midL);
        if(midVal<docId)
        {
            return searchSortedInsertPos(tempListP,midL,endP,docId);
        }
        else
        {
            return searchSortedInsertPos(tempListP,startP,midL,docId);
        }
    }
}
