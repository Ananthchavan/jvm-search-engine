import { AlertCircle } from 'lucide-react';

const SearchError = ({ message }) => {
    return (
        <div className="flex flex-col items-center justify-center py-16 px-4 bg-red-50 border border-red-100 rounded-lg max-w-2xl mt-8">
            <AlertCircle className="w-12 h-12 text-red-500 mb-4" />
            <h3 className="text-xl font-semibold text-red-800 mb-2">Engine Error</h3>
            <p className="text-red-600 text-center max-w-md">
                {message || "An unexpected error occurred while connecting to the index."}
            </p>
        </div>
    );
};

export default SearchError;