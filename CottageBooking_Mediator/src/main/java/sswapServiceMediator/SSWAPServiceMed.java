package sswapServiceMediator;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Servlet implementation class SSWAPServiceMed
 */
@WebServlet("/SSWAPServiceMed")
public class SSWAPServiceMed extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
	public SSWAPServiceMed() {
        super();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request,response);
        //response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        SSWAPMed mediator = new SSWAPMed(this.getServletContext().getRealPath("/alignments/"), this.getServletContext().getRealPath("/res/mySSWAPServiceOntology.owl"));
        
        String endPoint = request.getParameter("serviceURL").toString().trim();
        
        if(request.getParameter("reqType").toString().equals("deleteAlignment")){
            try {
                mediator.deleteAlignment(endPoint);
            } catch (Exception e) {
                
                PrintWriter out = response.getWriter();
                out.write("{\"deleted\" : false, \"message\" : \""+e.getMessage()+"\"}");  
                out.flush();
                out.close();
                return;
            }
            PrintWriter out = response.getWriter();
            out.write("{\"deleted\" : true}");  
            out.flush();
            out.close();
            return;
        }
        
        HashMap<String, String> params = new HashMap<>();
        
        params.put("name", request.getParameter("name").toString().trim());
        params.put("places", request.getParameter("places").toString().trim());
        params.put("bedrooms", request.getParameter("bedrooms").toString().trim());
        params.put("lakeDist", request.getParameter("lakeDist").toString().trim());
        params.put("city", request.getParameter("city").toString().trim());
        params.put("cityDist", request.getParameter("cityDist").toString().trim());
        params.put("days", request.getParameter("days").toString().trim());            
        params.put("startDate", request.getParameter("startDate").toString().trim());
        params.put("shift", request.getParameter("shift").toString().trim());
        
        if(request.getParameter("reqType").toString().equals("doQuery")){ // Initial query
            
            try {
                mediator.sendRequest(endPoint, params, null);
            } catch (JSONException e) {

                e.printStackTrace();
            }
        }
        else if (request.getParameter("reqType").toString().equals("aligned")){ // Second query with user-modified alignment 
            
            try {
                JSONObject alignment = new JSONObject(request.getParameter("alignment"));
                mediator.sendRequest(endPoint, params, alignment);
            } catch (JSONException e) {

                e.printStackTrace();
            }
        }
        
        PrintWriter out = response.getWriter();
        out.write(mediator.getResult());  
        out.flush();
        out.close();
        
        
        
        
        
        
    }

}
