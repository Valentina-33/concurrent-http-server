const resultEl = document.getElementById("result");

function showResult(text) {
    resultEl.hidden = false;
    resultEl.textContent = text;
}

function greet() {
    const name = document.getElementById("hello-name").value;
    fetch("/hello?name=" + encodeURIComponent(name))
        .then(response => response.text())
        .then(showResult)
        .catch(() => showResult("Network error: could not reach the server."));
}

function getPi() {
    fetch("/pi")
        .then(response => response.text())
        .then(showResult)
        .catch(() => showResult("Network error: could not reach the server."));
}

document.getElementById("hello-form").addEventListener("submit", (event) => {
    event.preventDefault();
    greet();
});

document.getElementById("pi-button").addEventListener("click", getPi);
