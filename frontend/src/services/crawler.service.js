import { adminApiClient } from './api';

const crawlerService = {
    startCrawl: async (url) => {
        if (!url || url.trim() === '') {
            throw new Error('Please enter a valid URL to crawl.');
        }

        const trimmedUrl = url.trim();
        if (!trimmedUrl.toLowerCase().startsWith('http://') && !trimmedUrl.toLowerCase().startsWith('https://')) {
            throw new Error('URL must start with http:// or https://');
        }

        const response = await adminApiClient.post('/crawl', {
            seedUrl: trimmedUrl
        });

        return response;
    },

    getQueueStats: async () => {
        const response = await adminApiClient.get('/crawl/stats');
        return response;
    },

    getCrawlerErrors: async () => {
        const response = await adminApiClient.get('/crawl/errors');
        return response;
    },

    flushIndex: async () => {
        // override the default 10s timeout with 60s to avoid premature failure
        const response = await adminApiClient.post('/crawl/flush-index', null, {
            timeout: 60000
        });
        return response;
    },

    clearQueue: async () => {
        const response = await adminApiClient.post('/crawl/clear-queue');
        return response;
    }
};

export default crawlerService;