package sswapService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import info.sswap.api.servlet.SimpleSSWAPServlet;

@WebServlet("/SSWAPServlet")
public class SSWAPServlet extends SimpleSSWAPServlet {
    

private static final long serialVersionUID = 1L;

    
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        // always do this
        super.init(servletConfig);
        
        Initializer.setDB(this.getServletContext().getRealPath("/res/cottageDB.ttl"));
         
        // do anything else here that needs to be done once, on servlet load

        // if needed, to increase the timeout above the default 2 mins
        //int timeout = 10 * 60 * 1000; // 5 mins in milliseconds
        //setTimeout(timeout);
    }
    
    
    /*
     * We can override this method to connect this servlet with the
     * service class, or define the init-param 'ServiceClass' in
     * web.xml
     * 
     * @see info.sswap.api.servlet.SimpleSSWAPServlet#getServiceClass()
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Class<T> getServiceClass() {

        return (Class<T>) SSWAPServ.class;
    }


}
