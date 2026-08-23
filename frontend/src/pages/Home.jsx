import { useNavigate } from 'react-router-dom';
import SearchBox from '../components/SearchBox';

const Home = () => {
    const navigate = useNavigate();

    const suggestions = ['java', 'spring boot', 'inverted index', 'tf-idf', 'postgresql'];

    const handleSuggestionClick = (topic) => {
        navigate(`/search?query=${encodeURIComponent(topic)}`);
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-[calc(100vh-140px)] px-4 -mt-10">
            {/* Logo */}
            <div className="flex flex-col items-center mb-8 select-none">
                <h1 className="text-5xl sm:text-6xl font-extrabold tracking-tight mb-2">
                    <span className="text-blue-600">J</span>
                    <span className="text-red-500">V</span>
                    <span className="text-yellow-500">M</span>{' '}
                    <span className="text-gray-800">Search</span>
                </h1>
                <p className="text-sm font-medium text-gray-500 tracking-wide">
                    Information Retrieval Engine
                </p>
            </div>

            {/* SearchBox */}
            <SearchBox />

            {/* Search Suggestions */}
            <div className="mt-8 flex flex-col items-center">
                <span className="text-xs uppercase tracking-wider text-gray-400 font-semibold mb-3">
                    Suggested Topics
                </span>
                <div className="flex flex-wrap justify-center gap-2 max-w-lg">
                    {suggestions.map((topic) => (
                        <button
                            key={topic}
                            onClick={() => handleSuggestionClick(topic)}
                            className="px-3 py-1.5 bg-white border border-gray-200 rounded-full text-xs font-medium text-gray-600 hover:bg-gray-100 hover:text-blue-600 hover:border-blue-200 transition-all shadow-sm"
                        >
                            {topic}
                        </button>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default Home;