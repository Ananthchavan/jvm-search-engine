import { useState, useEffect } from 'react';
import {
    Search, Clock, List, Database, Globe, FileText, Hash,
    Activity, CheckCircle, XCircle, RefreshCw
} from 'lucide-react';
import analyticsService from '../services/analytics.service';
import crawlerService from '../services/crawler.service';

const AnalyticsDashboard = () => {
    const [searchMetrics, setSearchMetrics] = useState({ totalSearches: 0, averageLatency: 0, averageResults: 0 });
    const [indexStats, setIndexStats] = useState({ totalPages: 0, indexedPages: 0, totalTerms: 0, totalPostings: 0 });
    const [crawlerStats, setCrawlerStats] = useState({ pending: 0, processing: 0, completed: 0, failed: 0 });
    const [recentSearches, setRecentSearches] = useState([]);

    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchAllData = async () => {
        setIsLoading(true);
        const failures = [];

        const [searchResult, indexResult, crawlerResult, historyResult] = await Promise.allSettled([
            analyticsService.getSearchMetrics(),
            analyticsService.getStats(),
            crawlerService.getQueueStats(),
            analyticsService.getSearchHistory(0, 10)
        ]);

        if (searchResult.status === 'fulfilled') {
            setSearchMetrics(searchResult.value ?? { totalSearches: 0, averageLatency: 0, averageResults: 0 });
        } else {
            failures.push('Search Metrics');
        }

        if (indexResult.status === 'fulfilled') {
            setIndexStats(indexResult.value ?? { totalPages: 0, indexedPages: 0, totalTerms: 0, totalPostings: 0 });
        } else {
            failures.push('Index Stats');
        }

        if (crawlerResult.status === 'fulfilled') {
            setCrawlerStats(crawlerResult.value ?? { pending: 0, processing: 0, completed: 0, failed: 0 });
        } else {
            failures.push('Crawler Stats');
        }

        if (historyResult.status === 'fulfilled') {
            setRecentSearches(historyResult.value?.content ?? []);
        } else {
            failures.push('Search History');
        }

        setError(failures.length > 0
            ? `Could not load: ${failures.join(', ')}. Make sure the backend server is running.`
            : null
        );
        setIsLoading(false);
    };

    useEffect(() => {
        fetchAllData();
    }, []);

    const MetricCard = ({ title, value, icon: Icon, colorClass, bgColorClass, suffix = "" }) => (
        <div className="bg-white rounded-lg shadow-sm border border-gray-200 p-5 flex items-center">
            <div className={`p-3 rounded-full ${bgColorClass} ${colorClass} mr-4`}>
                <Icon size={22} />
            </div>
            <div>
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider">{title}</p>
                <h3 className="text-2xl font-bold text-gray-800 mt-1">
                    {value !== undefined && value !== null ? value.toLocaleString() : 0}
                    <span className="text-sm font-medium text-gray-500 ml-1">{suffix}</span>
                </h3>
            </div>
        </div>
    );

    return (
        <div className="p-8 max-w-7xl mx-auto min-h-[80vh] bg-gray-50">
            {/* Header */}
            <div className="flex justify-between items-center mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">System Analytics</h1>
                    <p className="text-gray-500 text-sm mt-1">Real-time health and performance metrics across the JVM Search Engine.</p>
                </div>
                <button
                    onClick={fetchAllData}
                    disabled={isLoading}
                    className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 transition shadow-sm disabled:opacity-50"
                >
                    <RefreshCw size={16} className={isLoading ? 'animate-spin' : ''} />
                    Refresh Data
                </button>
            </div>

            {error && (
                <div className="mb-6 p-4 bg-red-50 border border-red-200 text-red-700 rounded-md">
                    <strong>Failed to synchronize:</strong> {error}
                </div>
            )}

            {/* Search Engine Health */}
            <h2 className="text-lg font-bold text-gray-800 mb-4 border-b pb-2">Search Performance</h2>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <MetricCard title="Total Queries" value={searchMetrics.totalSearches} icon={Search} colorClass="text-indigo-600" bgColorClass="bg-indigo-100" />
                <MetricCard title="Avg Latency" value={searchMetrics.averageLatency} suffix="ms" icon={Clock} colorClass="text-emerald-600" bgColorClass="bg-emerald-100" />
                <MetricCard title="Avg Results" value={searchMetrics.averageResults} icon={List} colorClass="text-purple-600" bgColorClass="bg-purple-100" />
            </div>

            {/* Inverted Index Health */}
            <h2 className="text-lg font-bold text-gray-800 mb-4 border-b pb-2">Database & Index Size</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <MetricCard title="Total Pages" value={indexStats.totalPages} icon={Globe} colorClass="text-gray-600" bgColorClass="bg-gray-200" />
                <MetricCard title="Indexed Pages" value={indexStats.indexedPages} icon={FileText} colorClass="text-blue-600" bgColorClass="bg-blue-100" />
                <MetricCard title="Unique Terms" value={indexStats.totalTerms} icon={Hash} colorClass="text-orange-600" bgColorClass="bg-orange-100" />
                <MetricCard title="Total Postings" value={indexStats.totalPostings} icon={Database} colorClass="text-red-600" bgColorClass="bg-red-100" />
            </div>

            {/* Crawler Pipeline Health */}
            <h2 className="text-lg font-bold text-gray-800 mb-4 border-b pb-2">Ingestion Queue</h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
                <MetricCard title="Pending" value={crawlerStats.pending} icon={Clock} colorClass="text-yellow-600" bgColorClass="bg-yellow-100" />
                <MetricCard title="Processing" value={crawlerStats.processing} icon={Activity} colorClass="text-blue-600" bgColorClass="bg-blue-100" />
                <MetricCard title="Completed" value={crawlerStats.completed} icon={CheckCircle} colorClass="text-green-600" bgColorClass="bg-green-100" />
                <MetricCard title="Failed" value={crawlerStats.failed} icon={XCircle} colorClass="text-red-600" bgColorClass="bg-red-100" />
            </div>

            {/* Recent Search History Table */}
            <div className="bg-white rounded-lg shadow-sm border border-gray-200 overflow-hidden">
                <div className="p-5 border-b border-gray-200 bg-gray-50 flex justify-between items-center">
                    <h2 className="text-md font-bold text-gray-800 flex items-center gap-2">
                        <Search size={18} className="text-gray-500" />
                        Live Search Log
                    </h2>
                    <span className="text-xs font-medium text-gray-500 bg-white px-2 py-1 rounded border">Top 10 Recent</span>
                </div>

                <div className="overflow-x-auto">
                    <table className="w-full text-left text-sm text-gray-600">
                        <thead className="bg-white border-b border-gray-100 text-gray-500 text-xs uppercase">
                            <tr>
                                <th className="px-6 py-4 font-semibold">User Query</th>
                                <th className="px-6 py-4 font-semibold">Results Found</th>
                                <th className="px-6 py-4 font-semibold">Latency</th>
                                <th className="px-6 py-4 font-semibold">Timestamp</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-100">
                            {recentSearches.length === 0 ? (
                                <tr>
                                    <td colSpan="4" className="px-6 py-8 text-center text-gray-400 italic">
                                        No searches recorded yet. Go make a query!
                                    </td>
                                </tr>
                            ) : (
                                recentSearches.map((search) => (
                                    <tr key={search.id} className="hover:bg-gray-50 transition">
                                        <td className="px-6 py-4 font-medium text-gray-900">"{search.query}"</td>
                                        <td className="px-6 py-4">
                                            <span className="bg-blue-50 text-blue-700 px-2 py-1 rounded text-xs font-bold">
                                                {search.resultCount.toLocaleString()}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4">
                                            <span className={`px-2 py-1 rounded text-xs font-mono ${search.latencyMs > 300 ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-600'}`}>
                                                {search.latencyMs} ms
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 text-xs text-gray-500">
                                            {new Date(search.searchedAt).toLocaleString()}
                                        </td>
                                    </tr>
                                ))
                            )}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default AnalyticsDashboard;