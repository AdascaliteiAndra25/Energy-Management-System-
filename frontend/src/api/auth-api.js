import { performRequest } from "./rest-client";
import { HOST } from "./hosts";

export function registerUser(username, password, role, age, callback) {
    const body = { username, password, role, age };
    const request = new Request(`${HOST.auth_api}/register`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    performRequest(request, callback);
}

export function loginUser(username, password, callback) {
    const body = { username, password };
    const request = new Request(`${HOST.auth_api}/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    performRequest(request, callback);
}
