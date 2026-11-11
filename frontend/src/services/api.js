import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// ---------------------------------------------------------------------------
// Shared response/error interceptor logic
// ---------------------------------------------------------------------------
function attachResponseInterceptor(client) {
    client.interceptors.response.use(
        (response) => response.data,
        (error) => {
            let customError = {
                status: 500,
                error: 'Network Error',
                message: 'Unable to connect to the search server. Please check your backend.',
            };

            if (error.response) {
                const backendError = error.response.data;
                customError = {
                    status: error.response.status,
                    error: backendError?.error || 'Server Error',
                    message: backendError?.message || error.message,
                    path: backendError?.path || '',
                    timestamp: backendError?.timestamp || new Date().toISOString(),
                };
            } else if (error.request) {
                customError.message = 'Server did not respond. Verify the Spring Boot application is running.';
            }

            return Promise.reject(customError);
        }
    );
}

// ---------------------------------------------------------------------------
// Public API client — used for search (no auth required)
// ---------------------------------------------------------------------------
const apiClient = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
});
attachResponseInterceptor(apiClient);

// ---------------------------------------------------------------------------
// Admin API client — attaches X-API-Key on every request.
// Used by admin-only pages (crawler, analytics).
// Set VITE_ADMIN_API_KEY in your .env.local file (never commit the actual key).
// ---------------------------------------------------------------------------
const adminApiClient = axios.create({
    baseURL: BASE_URL,
    timeout: 10000,
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
});

adminApiClient.interceptors.request.use((config) => {
    const apiKey = import.meta.env.VITE_ADMIN_API_KEY;
    if (apiKey) {
        config.headers['X-API-Key'] = apiKey;
    }
    return config;
});
attachResponseInterceptor(adminApiClient);

export default apiClient;
export { adminApiClient };
