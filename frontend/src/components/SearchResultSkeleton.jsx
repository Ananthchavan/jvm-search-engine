const SearchResultSkeleton = () => {
    return (
        <div className="mb-8 max-w-2xl animate-pulse">
            {/* url display placeholder */}
            <div className="h-4 bg-gray-200 rounded w-1/3 mb-2"></div>

            {/* clickable title placeholder */}
            <div className="h-6 bg-gray-200 rounded w-3/4 mb-3"></div>

            {/* snippet placeholder */}
            <div className="h-4 bg-gray-200 rounded w-full mb-2"></div>
            <div className="h-4 bg-gray-200 rounded w-5/6 mb-3"></div>

            {/* relevance score placeholder */}
            <div className="h-5 bg-gray-100 rounded w-32 border border-gray-100"></div>
        </div>
    );
};

export default SearchResultSkeleton;