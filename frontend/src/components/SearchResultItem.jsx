const SearchResultItem = ({ result }) => {
    return (
        <div className="mb-8 max-w-2xl">
            {/* URL */}
            <div className="text-sm text-gray-600 mb-1 flex items-center gap-2">
                <span className="truncate">{result.url}</span>
            </div>

            {/* clickable title */}
            <a
                href={result.url}
                target="_blank"
                rel="noopener noreferrer"
                className="text-xl text-blue-600 font-medium hover:underline hover:text-blue-800 visited:text-purple-700 decoration-1"
            >
                {result.title}
            </a>

            {/* dynamic highlighted snippet */}
            <p
                className="text-sm text-gray-800 mt-1 leading-relaxed"
                dangerouslySetInnerHTML={{ __html: result.snippet }}
            />

            {/* tf-idf relevance score */}
            <div className="mt-1">
                <span className="text-xs font-mono text-gray-400 bg-gray-50 px-2 py-0.5 rounded border border-gray-100">
                    Relevance Score: {result.relevanceScore}
                </span>
            </div>
        </div>
    );
};

export default SearchResultItem;