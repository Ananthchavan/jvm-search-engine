import apiClient from './api';

const crawlerService = {
    startCrawl: async (url) => {
        if (!url || url.trim() === '') {
            throw new Error('Please enter a valid URL to crawl.');
        }

        const formattedUrl = url.trim().toLowerCase();
        if (!formattedUrl.startsWith('http://') && !formattedUrl.startsWith('https://')) {
            throw new Error('URL must start with http:// or https://');
        }

        const response = await apiClient.post('/crawl', {
            seedUrl: formattedUrl
        });

        return response;
    },

    getQueueStats: async () => {
        const response = await apiClient.get('/crawl/stats');
        return response;
    },

    getCrawlerErrors: async () => {
        const response = await apiClient.get('/crawl/errors');
        return response;
    },

    flushIndex: async () => {
        // override the default 10s timeout with 60s to avoid premature failure
        const response = await apiClient.post('/crawl/flush-index', null, {
            timeout: 60000
        });
        return response;
    }
};

export default crawlerService;