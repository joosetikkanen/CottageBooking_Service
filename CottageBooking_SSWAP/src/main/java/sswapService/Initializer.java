package sswapService;

import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebListener;

public class Initializer {
    
    static String pathToDB;

    public static void setDB(String realPath) {
        pathToDB = realPath;
        
    }
    
    public static String getDB() {
        return pathToDB;
    }

}
