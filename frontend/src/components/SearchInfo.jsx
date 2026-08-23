const SearchInfo = ({ totalResults, executionTimeMs }) => {
    // Format numbers properly(12,345 from 12345)
    const formattedResults = new Intl.NumberFormat('en-US').format(totalResults);

    return (
        <div className="text-sm text-gray-500 mb-6 pb-2 border-b border-gray-100 max-w-2xl">
            Found {formattedResults} results in {executionTimeMs} milliseconds
        </div>
    );
};

export default SearchInfo;