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

        // using an absolute URL here because CrawlController is mapped 
        // to /api/crawl instead of /api/v1/crawl (will fix this later)
        const response = await apiClient.post('http://localhost:8080/api/crawl', {
            seedUrl: formattedUrl
        });

        return response;
    },

    getQueueStats: async () => {
        const response = await apiClient.get('http://localhost:8080/api/crawl/stats');
        return response;
    },

    getCrawlerErrors: async () => {
        const response = await apiClient.get('http://localhost:8080/api/crawl/errors');
        return response;
    },

    flushIndex: async () => {
        const response = await apiClient.post('http://localhost:8080/api/crawl/flush-index');
        return response;
    }
};

export default crawlerService;