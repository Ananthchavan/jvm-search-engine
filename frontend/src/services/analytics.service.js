import { adminApiClient } from './api';

const analyticsService = {
    getStats: async () => {
        const response = await adminApiClient.get('/analytics/stats');
        return response;
    },

    getSearchHistory: async (page = 0, size = 10) => {
        const response = await adminApiClient.get('/analytics/history', {
            params: { page, size }
        });
        return response;
    },

    getSearchMetrics: async () => {
        const response = await adminApiClient.get('/analytics/history/metrics');
        return response;
    }
};

export default analyticsService;