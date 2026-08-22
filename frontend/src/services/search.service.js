import apiClient from './api';

const searchService = {
    search: async (query, page = 1) => {
        if (!query || query.trim() === '') {
            throw new Error('Search query cannot be empty.');
        }

        const response = await apiClient.get('/search', {
            params: {
                q: query.trim(),
                page: Math.max(1, page),
            }
        });

        return response;
    }
};

export default searchService;