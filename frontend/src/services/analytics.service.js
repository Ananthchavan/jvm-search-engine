import apiClient from './api';

const analyticsService = {
    getStats: async () => {
        const response = await apiClient.get('/analytics/stats');
        return response;
    },

    getSearchHistory: async (page = 0, size = 10) => {
        const response = await apiClient.get('/analytics/history', {
            params: { page, size }
        });
        return response;
    },

    getSearchMetrics: async () => {
        const response = await apiClient.get('/analytics/history/metrics');
        return response;
    }
};

export default analyticsService;