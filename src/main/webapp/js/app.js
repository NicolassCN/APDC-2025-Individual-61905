document.addEventListener("DOMContentLoaded", () => {
    const token = localStorage.getItem("token");
    if (token) {
        fetch("/api/user/info", {
            headers: { "Authorization": `Bearer ${token}` }
        })
        .then(response => response.json())
        .then(data => {
            document.getElementById("username").textContent = data.username;
            document.getElementById("role").textContent = data.role;
            const opsList = document.getElementById("operations");
            data.operations.forEach(op => {
                const li = document.createElement("li");
                li.textContent = op;
                opsList.appendChild(li);
            });
        });
    }
});