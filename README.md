## Search engine with multi-threaded indexing and complex phrase query support!

### *Run main and enter one of the below two inputs:

### -Builds an index over a sample corpus of 37000 documents. 
    :corpusFromJSON

### -Builds an index over the files in the specified directory
    :index <dir_path>


### *You can now start querying..!
### At any point, use the below to print the contents of a file
### :open <file_name_from_search_result.txt>
    open 1.txt

### *Sample queries

### -Boolean AND query 
    Park National
### -Phrase query (Quotes are inclusive!)
    "Park National"
### -NOT  query
    "park national" -"national park"
### -Complex query
    ((park + volcano) eruption) + "wild fire" 
