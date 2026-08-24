import { FileQuestion } from 'lucide-react';

const SearchEmpty = ({ query }) => {
    return (
        <div className="flex flex-col items-center justify-center py-16 px-4 text-center mt-8">
            <FileQuestion className="w-16 h-16 text-gray-300 mb-4" />
            <h3 className="text-xl font-medium text-gray-800 mb-2">No results found</h3>
            <p className="text-gray-600">
                Your search - <strong className="text-gray-900">{query}</strong> - did not match any documents in our index.
            </p>

            <ul className="mt-6 text-sm text-gray-500 text-left list-disc list-inside">
                <li>Make sure all words are spelled correctly.</li>
                <li>Try different, more general keywords.</li>
                <li>Check if your crawler has ingested relevant pages.</li>
            </ul>
        </div>
    );
};

export default SearchEmpty;