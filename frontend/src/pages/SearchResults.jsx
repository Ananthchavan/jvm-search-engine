import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import useSearch from '../hooks/useSearch';
import SearchBox from '../components/SearchBox';
import SearchInfo from '../components/SearchInfo';
import SearchResultItem from '../components/SearchResultItem';
import Pagination from '../components/Pagination';
import SearchResultSkeleton from '../components/SearchResultSkeleton';
import SearchError from '../components/SearchError';
import SearchEmpty from '../components/SearchEmpty';
import { SearchX } from 'lucide-react';

const SearchResults = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const { data, isLoading, error, performSearch } = useSearch();

    const rawQuery = searchParams.get('query') || searchParams.get('q') || '';
    const query = rawQuery.trim();

    const parsedPage = parseInt(searchParams.get('page') || '1', 10);
    const page = isNaN(parsedPage) || parsedPage < 1 ? 1 : parsedPage;

    useEffect(() => {
        if (query) {
            performSearch(query, page);
        }
    }, [query, page]);

    const handlePageChange = (newPage) => {
        const targetPage = Math.max(1, newPage);
        navigate(`/search?query=${encodeURIComponent(query)}&page=${targetPage}`);
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
                {!query ? (
                    <div className="flex flex-col items-center justify-center text-center py-16 text-gray-500">
                        <SearchX className="w-12 h-12 text-gray-400 mb-3" />
                        <h3 className="text-lg font-semibold text-gray-700">No search query provided</h3>
                        <p className="text-sm text-gray-500 mt-1">
                            Please enter keywords into the search box above to explore documents.
                        </p>
                    </div>
                ) : isLoading ? (
                    <div className="mt-8">
                        {[...Array(5)].map((_, index) => (
                            <SearchResultSkeleton key={index} />
                        ))}
                    </div>
                ) : error ? (
                    /* render error Component */
                    <SearchError message={error} />
                ) : data && data.results ? (
                    <>
                        <SearchInfo
                            totalResults={data.totalResults}
                            executionTimeMs={data.executionTimeMs}
                        />

                        <div className="mt-2">
                            {data.results.length === 0 ? (
                                /* render empty state component */
                                <SearchEmpty query={query} />
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