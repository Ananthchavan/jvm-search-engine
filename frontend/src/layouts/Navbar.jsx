import { Link } from 'react-router-dom';
import { Settings, BarChart2 } from 'lucide-react';

const Navbar = () => {
    return (
        <header className="flex items-center justify-between px-6 py-4 bg-white">
            {/* Left side: Logo routing to Home */}
            <Link to="/" className="text-xl font-bold tracking-tight">
                <span className="text-blue-500">J</span>
                <span className="text-red-500">V</span>
                <span className="text-yellow-500">M</span> Search
            </Link>

            <nav className="flex items-center space-x-6 text-sm text-gray-700">
                <Link to="/admin/crawler" className="flex items-center gap-1 hover:underline">
                    <Settings size={16} />
                    Crawler Admin
                </Link>
                <Link to="/admin/analytics" className="flex items-center gap-1 hover:underline">
                    <BarChart2 size={16} />
                    Analytics
                </Link>
                <div className="w-8 h-8 bg-purple-600 text-white rounded-full flex items-center justify-center font-bold ml-2">
                    J
                </div>
            </nav>
        </header>
    );
};

export default Navbar;