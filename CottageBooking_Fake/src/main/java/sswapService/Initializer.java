package sswapService;

public class Initializer {
    
    static String pathToDB;

    public static void setDB(String realPath) {
        pathToDB = realPath;
        
    }
    
    public static String getDB() {
        return pathToDB;
    }

}
