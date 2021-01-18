function submit() {
    const Http = new XMLHttpRequest();
    const var1 = document.getElementById('nodeid')
    const var2 = document.getElementById('datetime')
    const url = 'http://localhost/print.php?nodeid=' + var1 + '?datetime=' + var2;
    Http.open("GET", url);
    Http.send();

    Http.onreadystatechange = (e) => {
        console.log(Http.responseText)
    }
}