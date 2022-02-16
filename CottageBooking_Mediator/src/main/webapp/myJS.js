function doQuery()
{
//alert('doQuery...');	
	if((document.getElementById('tf01').value!='')&&(document.getElementById('tf02').value!='')&&(document.getElementById('tf03').value!='')
		&&(document.getElementById('tf04').value!='')&&(document.getElementById('tf05').value!='')&&(document.getElementById('tf06').value!='')
		&&(document.getElementById('tf07').value!='')&&(document.getElementById('tf08').value!='')&&(document.getElementById('tf09').value!='')
		&&(document.getElementById('rc').value!=''))
	{
		var q_str = 'reqType=doQuery';
		q_str = q_str+'&serviceURL='+document.getElementById('rc').value;
		q_str = q_str+'&name='+document.getElementById('tf01').value;
		q_str = q_str+'&places='+document.getElementById('tf02').value;
		q_str = q_str+'&bedrooms='+document.getElementById('tf03').value;
		q_str = q_str+'&lakeDist='+document.getElementById('tf04').value;
		q_str = q_str+'&city='+document.getElementById('tf05').value;
		q_str = q_str+'&cityDist='+document.getElementById('tf06').value;
		q_str = q_str+'&days='+document.getElementById('tf07').value;
		q_str = q_str+'&startDate='+document.getElementById('tf08').value;
		q_str = q_str+'&shift='+document.getElementById('tf09').value;
		doAjax('SSWAPServiceMed',q_str,'doQuery_back','post',0);
	}else
	{
		alert('Please fill all the fields');
	}
}

function doQuery_back(result)
{
	
	console.log(result);
	
	try {
		//console.log(JSON.parse(result))
		let alignment = JSON.parse(result)
		console.log(alignment)
		if ("prompt" in alignment){
			promptUser(alignment);
			return;
		}		
	} catch (e){
		console.log("Results fetched")
	}
	
	results = JSON.parse("[" + result + "]");
		
	results.sort((a,b) =>{
		return a.bookingNumber - b.bookingNumber;
	});

	var resultDiv = document.getElementById("results");

	resultDiv.textContent = "";

	results.forEach((o) =>{
		let cottageList = document.createElement("ul");
		let bookNumber = document.createElement("li");
		bookNumber.textContent = "Booking number: " + o.bookingNumber;
		cottageList.appendChild(bookNumber);
		resultDiv.appendChild(cottageList);

		let cottageNestedList = document.createElement("ul");
		cottageList.appendChild(cottageNestedList);

		let resultList = [];

		for (var key of Object.keys(o)){

			switch(key){
				case "bookerName":
					resultList.push({1: "Booker: " + o[key]})
					break;
				case "name":
					resultList.push({1: "Booker: " + o[key]})
					break;
				case "address":
					resultList.push({2: "Address: " + o[key]})
					break;
				case "actualNumberOfPlaces":
					resultList.push({3: "Number of places: " + o[key]})
					break;
				case "actualNumberOfBedrooms":
					resultList.push({4: "Bedrooms: " + o[key]})
					break;
				case "actualDistanceToLake":
					resultList.push({5: "Distance to nearest lake: " + o[key]})
					break;
				case "nearestCity":
					resultList.push({6: "Nearest city: " + o[key]})
					break;
				case "actualNearestCity":
					resultList.push({6: "Nearest city: " + o[key]})
					break;
				case "actualDistanceToNearestCity":
					resultList.push({7: "Distance to nearest city: " + o[key]})
					break;
				case "actualStartingDate":
					resultList.push({8: "Starting date: " + o[key]})
					break;
				case "actualEndDate":
					resultList.push({9: "End date: " + o[key]})
					break;
				case "imageURL":
					resultList.push({10: o[key]})
					break;
			}
		}
		
		let merged = resultList.reduce((result, current) => {
			return Object.assign(result, current);
		}, {});
				
		let ordered = Object.keys(merged).sort().reduce((obj, key) => {
			obj[key] = merged[key];
			return obj;
		}, {});


		for (let key of Object.keys(ordered)){

			let datarow = document.createElement("li");
			let value = ordered[key]
			if (value.includes("^^")){
				value = value.substring(0, value.indexOf("^^"))
			}

			if (key == 10){
				let img = document.createElement("img")
				img.setAttribute("src", value)
				datarow.appendChild(img);
			}
			else {				
				datarow.textContent = value;
			}
			cottageNestedList.appendChild(datarow);	

		}
		
	})

}

function promptUser(alignment){
	// Get the modal
	var modal = document.getElementById("myModal");
	
	// Get the <span> element that closes the modal
	var span = document.getElementsByClassName("close")[0];
	
	modal.style.display = "block";
		
	// When the user clicks on <span> (x), close the modal
	span.onclick = function() {
	  modal.style.display = "none";
	}
		
	
	let table = document.getElementById("alignment");
	table.textContent = "";
	//Header row
	let headers = document.createElement("tr");
	let th1 = document.createElement("th");
	th1.textContent = "Foreign term";
	headers.appendChild(th1);
	let th2 = document.createElement("th");
	th2.textContent = "Your term";
	headers.appendChild(th2);
	table.appendChild(headers)
	//Content
	let i = 0;
	const map = new Map();
	for (var key of Object.keys(alignment)){
		
		if (key === "prompt") continue;
		
		let tr = document.createElement("tr");
		let td1 = document.createElement("td");
		td1.textContent = key;
		tr.appendChild(td1);
		let td2 = document.createElement("td");
		//td2.textContent = alignment[key];
		let input = document.createElement("input");
		let id = "term" + ++i;
		input.setAttribute("id", id);
		input.setAttribute("value", alignment[key]);
		td2.appendChild(input);
		tr.appendChild(td2);
		table.appendChild(tr)
		
		map.set(key, id)
	}
	
	let okBtn = document.getElementById("confirm")
	let cancelBtn = document.getElementById("cancel")
	
	okBtn.onclick = () => {
		
		let newAlignment = {};
		
		for (let key of Object.keys(alignment)){
			
			if (key === "prompt") continue;
			
			let userValue = document.getElementById(map.get(key)).value
			if (userValue === "") continue;
			console.log("key: " + key + " value :" + map.get(key))
			//console.log(document.getElementById(map.get(key)).value)
			newAlignment[key] = document.getElementById(map.get(key)).value;
		}
		newAlignment["modified"] = true;
		console.log(newAlignment)
		
		console.log(encodeURIComponent(JSON.stringify(newAlignment)))
		var q_str = 'reqType=aligned';
		q_str = q_str+'&serviceURL='+document.getElementById('rc').value;
		q_str = q_str+'&name='+document.getElementById('tf01').value;
		q_str = q_str+'&places='+document.getElementById('tf02').value;
		q_str = q_str+'&bedrooms='+document.getElementById('tf03').value;
		q_str = q_str+'&lakeDist='+document.getElementById('tf04').value;
		q_str = q_str+'&city='+document.getElementById('tf05').value;
		q_str = q_str+'&cityDist='+document.getElementById('tf06').value;
		q_str = q_str+'&days='+document.getElementById('tf07').value;
		q_str = q_str+'&startDate='+document.getElementById('tf08').value;
		q_str = q_str+'&shift='+document.getElementById('tf09').value;
		q_str = q_str+'&alignment='+encodeURIComponent(JSON.stringify(newAlignment))
		doAjax('SSWAPServiceMed',q_str,'doQuery_back','post',0);
		modal.style.display = "none";
	}
	
	cancelBtn.onclick = () => {
	  modal.style.display = "none";
	}
	
}

deleteAlignment = () => {
	
	var q_str = 'reqType=deleteAlignment';
	q_str = q_str+'&serviceURL='+document.getElementById('rc').value;
	doAjax('SSWAPServiceMed',q_str,'delete_back','post',0);
	
}

delete_back = (result) => {
	let status = JSON.parse(result)
	console.log(status)
	if (status.deleted){
		alert("Alignment deleted")
	}
	else{
		alert(status.message)
	}
}





