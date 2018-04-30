package Search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by chimera on 9/11/17.
 */


class GlobalPosIndex
{
    private final Map<String,ArrayList<Integer[]>> universalIndex;
    private ArrayList<Integer[]> tempList;
    private ArrayList<Integer[]> newList;

    public GlobalPosIndex()                         //Constructor
    {
        universalIndex = Collections.synchronizedMap(new HashMap<String,ArrayList<Integer[]>>());
        tempList =new ArrayList<Integer[]>();
        newList =new ArrayList<Integer[]>();

    }

    public void clear()                             //to refresh the Postional Index
    {
        universalIndex.clear();
    }

    public void add(String key, Integer[] posArray)                         //token insertion into data structure as keys
    {
        synchronized (universalIndex)
        {
            if(universalIndex.containsKey(key))
            {
                tempList =  universalIndex.get(key);
                this.binInsert(tempList, posArray);
            }
            else
            {
                newList.clear();
                newList.add(posArray);
                universalIndex.put(key, (ArrayList<Integer[]>) newList.clone());
            }
        }
    }

    private ArrayList binInsert(ArrayList<Integer[]> tempListP, Integer[] postingArray)                     //Insertion using Binary Search to sequentially arrange doc ids in ascending order
    {
        Integer insertPos;
        if(tempListP.size()==1)
        {
            Integer[] firstPosting=  tempListP.get(0);
            Integer firstVal=firstPosting[0];
            if(firstVal>postingArray[0])
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
            insertPos = searchSortedInsertPos(tempListP,0,tempListP.size()-1,postingArray);
        }


        tempListP.add(insertPos,postingArray);
        return tempListP;
    }

    private int searchSortedInsertPos(ArrayList<Integer[]> tempListP, int startP, int endP, Integer[] elementP)             //insertion of token position in the data structure as in the document
    {

        if(endP==startP+1)
        {
            if((tempListP.get(endP))[0]<elementP[0])
            {
                return endP+1;
            }
            else
            {
                return endP;
            }
        }
        
        int midL=(startP+endP)/2;
        Integer[] midPosting= tempListP.get(midL);
        Integer midVal=midPosting[0];
        if(midVal<elementP[0])
        {
            return searchSortedInsertPos(tempListP,midL,endP,elementP);
        }
        else
        {
            return searchSortedInsertPos(tempListP,startP,midL,elementP);
        }
    }

    public ArrayList<Integer[]> get(String token)                               //getter for Universal Index
    {
        return universalIndex.get(token);
    }

    public void printLen()                                                      //method to print the size of the size
    {
        System.out.println(universalIndex.keySet().size());
    }


    public void showVocab(){                                                    //to list the vocabulary as in the postion index with token count
        System.out.println("\nWords in the Vocabulary:");
        for(String str : universalIndex.keySet())
        {
            System.out.println(str);
        }
        System.out.println("\nCount of Words in Vocabulary: "+universalIndex.keySet().size());
    }

}
