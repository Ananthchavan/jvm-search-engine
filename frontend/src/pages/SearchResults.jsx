import { useEffect } from 'react';
import { useSearchParams, useNavigate, Link } from 'react-router-dom';
import useSearch from '../hooks/useSearch';
import SearchBox from '../components/SearchBox';
import SearchInfo from '../components/SearchInfo';
import SearchResultItem from '../components/SearchResultItem';
import Pagination from '../components/Pagination';
import { Loader2 } from 'lucide-react';

const SearchResults = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { data, isLoading, error, performSearch } = useSearch();

    const query = searchParams.get('query') || searchParams.get('q') || '';
    const page = parseInt(searchParams.get('page') || '1', 10);

    useEffect(() => {
        if (query) {
            performSearch(query, page);
        }
    }, [query, page]);

    const handlePageChange = (newPage) => {
        navigate(`/search?query=${encodeURIComponent(query)}&page=${newPage}`);
        window.scrollTo({ top: 0, behavior: 'smooth' });
    };

    return (
        <div className="min-h-screen bg-gray-50">
            <div className="bg-white border-b border-gray-200 sticky top-0 z-40 py-4 px-4 sm:px-8 flex justify-center shadow-sm">
                <div className="w-full max-w-3xl">
                    <SearchBox initialQuery={query} placeholder="Search again..." />
                </div>
            </div>

            <div className="w-full max-w-3xl mx-auto px-4 sm:px-8 py-6">
                {isLoading ? (
                    <div className="flex items-center justify-center gap-2 text-blue-600 mt-8">
                        <Loader2 className="w-6 h-6 animate-spin" />
                        <span className="font-medium">Searching the inverted index...</span>
                    </div>
                ) : error ? (
                    <div className="bg-red-50 border border-red-200 text-red-700 p-4 rounded-md mt-4 text-center">
                        <strong>Engine Error:</strong> {error}
                    </div>
                ) : data && data.results ? (
                    <>
                        <SearchInfo
                            totalResults={data.totalResults}
                            executionTimeMs={data.executionTimeMs}
                        />

                        <div className="mt-2">
                            {data.results.length === 0 ? (
                                <p className="text-gray-600 text-lg mt-8 text-center">
                                    Your search - <strong className="text-gray-900">{query}</strong> - did not match any documents.
                                </p>
                            ) : (
                                data.results.map((result, index) => (
                                    <SearchResultItem key={index} result={result} />
                                ))
                            )}
                        </div>

                        <Pagination
                            currentPage={data.currentPage}
                            totalPages={data.totalPages}
                            onPageChange={handlePageChange}
                        />
                    </>
                ) : null}
            </div>
        </div>
    );
};

export default SearchResults;