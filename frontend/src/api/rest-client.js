export async function performRequest(request, callback) {
    try {
        const response = await fetch(request);
        let data;
        const text = await response.text();
        try {
            data = JSON.parse(text);
        } catch {
            data = text;
        }

        if (response.ok) {
            callback(data, response.status, null);
        } else {
            callback(null, response.status, data);
        }
    } catch (err) {
        callback(null, 1, err);
    }
}
