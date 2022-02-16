function createREQ()
{
	try
	{
    		req = new XMLHttpRequest(); /* e.g. Firefox */
     	} catch(err1) {
      	try
		{
       		req = new ActiveXObject('Msxml2.XMLHTTP'); /* some versions IE */
       	} catch (err2) {
         		try
			{
         			req = new ActiveXObject("Microsoft.XMLHTTP"); /* some versions IE */
         		} catch (err3) {
          			req = false;
         		}
       	}
     }
     return req;
}


function requestGET(url, query, req)
{
	//myRand=parseInt(Math.random()*99999999);
	req.open("GET",url+'?'+query,true);
	req.send(null);
}


function requestPOST(url, query, req)
{
	req.open("POST",url,true);
	req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
	req.send(query);
}


function doCallback(callback,item)
{
	eval(callback+'(item)');
}


function doAjax(url,query,callback,reqtype,getxml)
{
// create the XMLHTTPRequest object instance
//alert('ajax-url: '+url);
//alert('ajax-query: '+query);
//alert('ajax-callback: '+callback);
	var myreq = createREQ();

	myreq.onreadystatechange = function() 
					{
						if(myreq.readyState == 4)
						{
							if(myreq.status == 200)
							{
      							var item = myreq.responseText;
      							if(getxml==1)
								{
        								item = myreq.responseXML;
      							}
    							doCallback(callback, item);
    						}else{
      							eval(alert(myreq.status));
    						}
 						}
					}
	if(reqtype=='post')
	{
		requestPOST(url,query,myreq);
	}else{
		requestGET(url,query,myreq);
	}
}

