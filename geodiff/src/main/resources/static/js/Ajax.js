var Ajax = Ajax || {};

Ajax.request = function(method, url, json, callback) {
  var xhr = new XMLHttpRequest();
  xhr.open(method, url, true);
  if (method == "POST") {
    xhr.setRequestHeader('Content-type', 'application/json');
  }
  xhr.onreadystatechange = function(){
    if (xhr.readyState === 4 && xhr.status === 200){
        callback(xhr.response);

    }
  }
  if (json == {}) xhr.send();
  else xhr.send(JSON.stringify(json));
}
