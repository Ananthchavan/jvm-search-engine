import axios from 'axios';

const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
    },
});

apiClient.interceptors.request.use(
    (config) => {
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

apiClient.interceptors.response.use(
    (response) => {
        return response.data;
    },
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

export default apiClient;