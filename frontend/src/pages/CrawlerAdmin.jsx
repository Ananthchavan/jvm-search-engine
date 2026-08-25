import { useState, useEffect } from 'react';
import { Activity, Clock, CheckCircle, XCircle, RefreshCw } from 'lucide-react';
import crawlerService from '../services/crawler.service';

const CrawlerAdmin = () => {
    const [stats, setStats] = useState({
        pending: 0,
        processing: 0,
        completed: 0,
        failed: 0
    });
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchStats = async () => {
        try {
            const data = await crawlerService.getQueueStats();
            setStats(data);
            setError(null);
        } catch (err) {
            setError(err.message || 'Failed to load crawler statistics.');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchStats();
        const interval = setInterval(fetchStats, 5000);
        return () => clearInterval(interval);
    }, []);

    // stat card component
    const StatCard = ({ title, count, icon: Icon, colorClass, bgColorClass }) => (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 flex items-center">
            <div className={`p-4 rounded-full ${bgColorClass} ${colorClass} mr-4`}>
                <Icon size={24} />
            </div>
            <div>
                <p className="text-sm font-medium text-gray-500">{title}</p>
                <h3 className="text-2xl font-bold text-gray-800">
                    {count.toLocaleString()}
                </h3>
            </div>
        </div>
    );

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-[80vh]">
            {/* Header Section */}
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Crawler Management</h1>
                    <p className="text-gray-500 text-sm mt-1">Monitor and control your backend ingestion pipeline.</p>
                </div>
                <button
                    onClick={() => { setIsLoading(true); fetchStats(); }}
                    className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm"
                >
                    <RefreshCw size={16} className={isLoading ? 'animate-spin' : ''} />
                    Refresh
                </button>
            </div>

            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-md">
                    {error}
                </div>
            )}

            {/* grid */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <StatCard
                    title="Pending URLs"
                    count={stats.pending}
                    icon={Clock}
                    colorClass="text-yellow-600"
                    bgColorClass="bg-yellow-100"
                />
                <StatCard
                    title="Processing"
                    count={stats.processing}
                    icon={Activity}
                    colorClass="text-blue-600"
                    bgColorClass="bg-blue-100"
                />
                <StatCard
                    title="Completed"
                    count={stats.completed}
                    icon={CheckCircle}
                    colorClass="text-green-600"
                    bgColorClass="bg-green-100"
                />
                <StatCard
                    title="Failed"
                    count={stats.failed}
                    icon={XCircle}
                    colorClass="text-red-600"
                    bgColorClass="bg-red-100"
                />
            </div>

            {/* Bottom Section */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                <div className="lg:col-span-1">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 border-dashed p-6 h-64 flex items-center justify-center text-gray-400">
                        Crawler Controls (Stage 3) will go here
                    </div>
                </div>
                <div className="lg:col-span-2">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 border-dashed p-6 h-64 flex items-center justify-center text-gray-400">
                        Error Logs Table (Stage 4) will go here
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CrawlerAdmin;