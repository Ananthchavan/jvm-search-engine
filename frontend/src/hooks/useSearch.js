import { useState } from 'react';
import searchService from '../services/search.service';

const useSearch = () => {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    const performSearch = async (query, page = 1) => {
        setIsLoading(true);
        setError(null);

        try {
            const response = await searchService.search(query, page);
            setData(response);
        } catch (err) {
            setError(err.message || 'An unexpected error occurred while searching.');
            setData(null);
        } finally {
            setIsLoading(false);
        }
    };

    return { data, isLoading, error, performSearch };
};

export default useSearch;