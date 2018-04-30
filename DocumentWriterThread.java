package Search;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by chimera on 9/11/17.
 */



//Class to run Document writing threads
class DocumentWriterThread implements Runnable
{
    private final Document doc;
    DocumentWriterThread(Document doc)
    {
        this.doc=doc;
    }

    @Override
    public void run()
    {

        File file=new File("corpus/"+doc.getId()+".txt");
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();

        try
        {
            Files.write(Paths.get(file.toString()),doc.getBody().getBytes());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
