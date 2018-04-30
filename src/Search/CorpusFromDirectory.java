package Search;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/* Class to Handle Position-Index from Directory Path entered by User*/


public class CorpusFromDirectory {

    private final GlobalPosIndex globalPosIndex;
    private final GlobalBiWordIndex globalBiWordIndex;
    private final File root;
    private final String filePath;

    private static int docCount=0;

    public CorpusFromDirectory(GlobalPosIndex globalPosIndex, GlobalBiWordIndex globalBiWordIndex, String filePath) throws IOException              //Constructor to Initialize Position-Indexes and FilePath
    {
        this.globalPosIndex = globalPosIndex;
        this.globalBiWordIndex = globalBiWordIndex;
        root = new File(filePath);
        this.filePath = filePath;
    }

    public void parseFilesNFolders(File folder)                             //To Access all folder sub-directory and files in th given file path
    {
        for (File fileEntry : folder.listFiles())
        {

            if (fileEntry.isDirectory()) {
                parseFilesNFolders(fileEntry);
            }
            else {
	            indexAndWriteFile(fileEntry.getAbsolutePath().toString());      // call to function to index and write to disk
            }
        }
    }

    private int generateDocId()                                         //method keeps track of document numbers
    {
        return docCount++;
    }

    private void indexAndWriteFile(String filePath)                     //Create Document object and populate the Document fields such as id, title and body
    {
        Document doc=new Document();
        doc.setUrl(filePath);
        doc.setTitle(filePath.substring(filePath.lastIndexOf('/') + 1));
        doc.setId(generateDocId());

            try {
                byte[] encoded = Files.readAllBytes(Paths.get(filePath));
                doc.setBody(new String(encoded, "UTF-8"));
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
            }
        submitJobs(doc);
    }


    private void submitJobs(Document doc)                               //to assign or submit job to the thread pool to write document to secondary storage without stemming
    {
        Runnable indexJob = new IndexBuilderThread(globalPosIndex,globalBiWordIndex,doc);
        Runnable writeJob = new DocumentWriterThread(doc);
        Main.submitJob(indexJob);
        Main.submitJob(writeJob);
    }

    public void start() throws IOException                              //start method to initiate corpus processing
    {
        parseFilesNFolders(root);
    }

}