import apiClient from './api';

const analyticsService = {
    getStats: async () => {
        const response = await apiClient.get('/analytics/stats');
        return response;
    },

    getSearchHistory: async (page = 0, size = 50) => {
        const response = await apiClient.get('/analytics/history', {
            params: {
                page: Math.max(0, page),
                size: Math.max(1, size)
            }
        });
        return response;
    }
};

export default analyticsService;