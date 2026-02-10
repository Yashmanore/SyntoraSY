window.onload = function() {
   document.getElementById('popup').style.display = 'flex';
};

    // Close popup function
document.getElementById('closePopup').onclick = function() {
   document.getElementById('popup').style.display = 'none';
};
const fileInput = document.querySelector(".file-input"),
fileName = document.querySelector(".file-name"),
chooseFile = document.querySelector(".upload-excel");

console.log("File input: ",fileInput); // Ensure fileInput is not null
console.log("File name: ",fileName); // Ensure fileName is not null
console.log("Choose file: ",chooseFile); // Ensure chooseFile is not null

chooseFile.addEventListener("click", () => fileInput.click());

console.log(fileInput); // Ensure fileInput is not null
console.log(fileName); // Ensure fileName is not null
console.log(chooseFile); // Ensure chooseFile is not null

fileInput.addEventListener('change', () => {
    let file = fileInput.files[0];
    if (!file) return;
    fileName.textContent = `Selected file: ${file.name}`;

    console.log("Hello");
    let formData = new FormData();
    formData.append('file', file);
    console.log("Hello1");

    // Fetch CSRF token and header name from meta tags
//    const csrfToken = document.querySelector('meta[name="_csrf_token"]').getAttribute('content');
//    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');
//
//    if (!csrfHeader || !csrfToken) {
//        console.log('CSRF header or token is missing');
//        return;
//    } else {
//        console.log(`CSRF Header: ${csrfHeader}`);
//        console.log(`CSRF Token: ${csrfToken}`);
//    }

     // Log FormData contents
     for (let [key, value] of formData.entries()) {
        console.log(`hello ${key}: ${value}`);
     }

    fetch('/api/upload', {
        method: 'POST',
        body: formData,
//        headers: {
//            [csrfHeader]: csrfToken // Dynamically set the CSRF header
//        }
    })

//    for (let [key, value] of formData.entries()) {
//       console.log(`${key}: ${value}`);
//    }
    .then(response => {
        console.log(`Response status: ${response.status}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(result => {
        console.log("Fetch result:", result);
        alert(result);
    })
    .catch(error => console.error('Error: ', error));
});