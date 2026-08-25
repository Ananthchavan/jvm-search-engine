import { useState, useEffect } from 'react';
import { Activity, Clock, CheckCircle, XCircle, RefreshCw, Play, Square, Database, AlertTriangle } from 'lucide-react';
import crawlerService from '../services/crawler.service';

const CrawlerAdmin = () => {
    const [stats, setStats] = useState({ pending: 0, processing: 0, completed: 0, failed: 0 });
    const [errors, setErrors] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const [seedUrl, setSeedUrl] = useState('');
    const [actionStatus, setActionStatus] = useState({ message: '', isError: false });
    const [isSubmitting, setIsSubmitting] = useState(false);

    const fetchData = async () => {
        try {
            const [statsData, errorsData] = await Promise.all([
                crawlerService.getQueueStats(),
                crawlerService.getCrawlerErrors()
            ]);
            setStats(statsData);
            setErrors(errorsData);
            setError(null);
        } catch (err) {
            setError(err.message || 'Failed to load crawler dashboard data.');
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
        const interval = setInterval(fetchData, 5000);
        return () => clearInterval(interval);
    }, []);

    const handleStartCrawl = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setActionStatus({ message: '', isError: false });

        try {
            await crawlerService.startCrawl(seedUrl);
            setActionStatus({ message: 'Crawler successfully started!', isError: false });
            setSeedUrl('');
            fetchData();
        } catch (err) {
            setActionStatus({ message: err.message || 'Failed to start crawler.', isError: true });
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleFlushIndex = async () => {
        setActionStatus({ message: 'Flushing index to database...', isError: false });
        try {
            await crawlerService.flushIndex();
            setActionStatus({ message: 'Index successfully flushed to PostgreSQL.', isError: false });
        } catch (err) {
            setActionStatus({ message: 'Failed to flush index.', isError: true });
        }
    };

    const StatCard = ({ title, count, icon: Icon, colorClass, bgColorClass }) => (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6 flex items-center">
            <div className={`p-4 rounded-full ${bgColorClass} ${colorClass} mr-4`}>
                <Icon size={24} />
            </div>
            <div>
                <p className="text-sm font-medium text-gray-500">{title}</p>
                <h3 className="text-2xl font-bold text-gray-800">{count.toLocaleString()}</h3>
            </div>
        </div>
    );

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-[80vh]">
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Crawler Management</h1>
                    <p className="text-gray-500 text-sm mt-1">Monitor and control your backend ingestion pipeline.</p>
                </div>
                <button
                    onClick={() => { setIsLoading(true); fetchData(); }}
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

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <StatCard title="Pending URLs" count={stats.pending} icon={Clock} colorClass="text-yellow-600" bgColorClass="bg-yellow-100" />
                <StatCard title="Processing" count={stats.processing} icon={Activity} colorClass="text-blue-600" bgColorClass="bg-blue-100" />
                <StatCard title="Completed" count={stats.completed} icon={CheckCircle} colorClass="text-green-600" bgColorClass="bg-green-100" />
                <StatCard title="Failed" count={stats.failed} icon={XCircle} colorClass="text-red-600" bgColorClass="bg-red-100" />
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Command Center */}
                <div className="lg:col-span-1">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-6">
                        <h2 className="text-lg font-bold text-gray-800 mb-4">Command Center</h2>

                        <form onSubmit={handleStartCrawl} className="mb-6">
                            <label htmlFor="seedUrl" className="block text-sm font-medium text-gray-700 mb-2">
                                Add Seed URL
                            </label>
                            <input
                                type="url"
                                id="seedUrl"
                                value={seedUrl}
                                onChange={(e) => setSeedUrl(e.target.value)}
                                placeholder="https://example.com"
                                className="w-full px-4 py-2 border border-gray-300 rounded-md focus:ring-blue-500 focus:border-blue-500 mb-3 outline-none"
                                required
                            />
                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="w-full flex justify-center items-center gap-2 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition disabled:opacity-50"
                            >
                                <Play size={18} />
                                {isSubmitting ? 'Starting...' : 'Start Crawler'}
                            </button>
                        </form>

                        <hr className="my-6 border-gray-100" />

                        <div className="space-y-3">
                            <button
                                onClick={handleFlushIndex}
                                className="w-full flex justify-center items-center gap-2 bg-gray-100 text-gray-700 px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-200 transition"
                            >
                                <Database size={18} />
                                Force Index Flush
                            </button>

                            <button
                                disabled
                                className="w-full flex justify-center items-center gap-2 bg-red-50 text-red-400 px-4 py-2 border border-red-100 rounded-md cursor-not-allowed"
                                title="Stop endpoint not yet implemented in backend"
                            >
                                <Square size={18} />
                                Stop Crawler
                            </button>
                        </div>

                        {actionStatus.message && (
                            <div className={`mt-4 p-3 text-sm rounded-md ${actionStatus.isError ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-green-50 text-green-700 border border-green-200'}`}>
                                {actionStatus.message}
                            </div>
                        )}
                    </div>
                </div>

                <div className="lg:col-span-2">
                    <div className="bg-white rounded-lg shadow-sm border border-gray-200 flex flex-col h-full">
                        <div className="p-6 border-b border-gray-100 flex justify-between items-center">
                            <h2 className="text-lg font-bold text-gray-800 flex items-center gap-2">
                                <AlertTriangle className="text-red-500" size={20} />
                                Recent Crawler Errors
                            </h2>
                            <span className="text-xs font-medium bg-red-100 text-red-700 px-2 py-1 rounded-full">
                                {errors.length} Latest
                            </span>
                        </div>

                        <div className="p-0 overflow-auto max-h-[400px]">
                            {errors.length === 0 ? (
                                <div className="p-8 text-center text-gray-500">
                                    No crawler errors found. The queue is healthy!
                                </div>
                            ) : (
                                <table className="w-full text-left text-sm text-gray-600">
                                    <thead className="bg-gray-50 text-gray-700 sticky top-0">
                                        <tr>
                                            <th className="px-6 py-3 font-medium">Target URL</th>
                                            <th className="px-6 py-3 font-medium">Error Message</th>
                                            <th className="px-6 py-3 font-medium w-40">Time</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100">
                                        {errors.map((err) => (
                                            <tr key={err.id} className="hover:bg-gray-50">
                                                <td className="px-6 py-4 max-w-[200px] truncate text-blue-600 hover:text-blue-800" title={err.url}>
                                                    <a href={err.url} target="_blank" rel="noopener noreferrer">
                                                        {err.url}
                                                    </a>
                                                </td>
                                                <td className="px-6 py-4 max-w-[300px] truncate text-red-600 font-mono text-xs" title={err.errorMessage}>
                                                    {err.errorMessage || 'Unknown Error'}
                                                </td>
                                                <td className="px-6 py-4 whitespace-nowrap text-xs text-gray-500">
                                                    {new Date(err.lastCrawledAt).toLocaleString()}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default CrawlerAdmin;