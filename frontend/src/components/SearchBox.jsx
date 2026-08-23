import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Loader2, X } from 'lucide-react';

const SearchBox = ({ initialQuery = '', placeholder = 'Search indexed pages or topics...' }) => {
    const [query, setQuery] = useState(initialQuery);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const inputRef = useRef(null);
    const navigate = useNavigate();

    useEffect(() => {
        inputRef.current?.focus();
    }, []);

    const handleSubmit = (e) => {
        e.preventDefault();
        const trimmedQuery = query.trim();
        if (!trimmedQuery || isSubmitting) return;
        setIsSubmitting(true);
        navigate(`/search?query=${encodeURIComponent(trimmedQuery)}`);
    };

    const handleClear = () => {
        setQuery('');
        inputRef.current?.focus();
    };

    return (
        <form onSubmit={handleSubmit} className="w-full max-w-2xl">
            <div className="relative flex items-center shadow-md hover:shadow-lg focus-within:shadow-lg rounded-full transition-shadow duration-200 bg-white border border-gray-200">
                {/* Left Search Icon / Loading Spinner */}
                <div className="pl-4 pr-2 text-gray-400 flex items-center">
                    {isSubmitting ? (
                        <Loader2 className="w-5 h-5 animate-spin text-blue-600" />
                    ) : (
                        <Search className="w-5 h-5" />
                    )}
                </div>

                {/* Input Field */}
                <input
                    ref={inputRef}
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    placeholder={placeholder}
                    disabled={isSubmitting}
                    className="w-full py-3.5 px-2 text-gray-800 placeholder-gray-400 bg-transparent focus:outline-none text-base disabled:opacity-60"
                />

                {/* Clear Button */}
                {query && !isSubmitting && (
                    <button
                        type="button"
                        onClick={handleClear}
                        className="p-1 mr-2 text-gray-400 hover:text-gray-600 rounded-full focus:outline-none"
                        title="Clear search"
                    >
                        <X className="w-4 h-4" />
                    </button>
                )}

                {/* Submit Button */}
                <div className="pr-2">
                    <button
                        type="submit"
                        disabled={!query.trim() || isSubmitting}
                        className="px-5 py-2 bg-blue-600 text-white text-sm font-medium rounded-full hover:bg-blue-700 disabled:bg-gray-200 disabled:text-gray-400 transition-colors duration-150 flex items-center gap-1.5"
                    >
                        {isSubmitting ? 'Searching...' : 'Search'}
                    </button>
                </div>
            </div>
        </form>
    );
};

export default SearchBox;