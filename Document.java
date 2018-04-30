package Search;

/**
 * Created by chimera on 9/9/17.
 */


// Class to maintain the data and functionality of a Document

class Document
{
    private String body;
    private String title;
    private int id;
    private String url;


    @Override
    public String toString()
    {
        return "\"title\" = \"" + title+"\" ::: \"body\" = \""+ body+"\"}";
    }

    public  String getTitle()
    {
        return title;
    }

    public  void setTitle(String title)
    {
        this.title=title;
    }

    public  String getBody()
    {
        return body;
    }

    public  void setBody(String body)
    {
        this.body=body;
    }

    public void setId(int id)
    {
        this.id=id;
    }
    public int getId()
    {
        return id;
    }
    public void setUrl(String url)
    {
        this.url=url;
    }

}
