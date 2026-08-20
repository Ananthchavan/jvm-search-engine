import { Routes, Route } from 'react-router-dom';

import MainLayout from './layouts/MainLayout';

import Home from './pages/Home';
import SearchResults from './pages/SearchResults';
import CrawlerAdmin from './pages/CrawlerAdmin';
import AnalyticsAdmin from './pages/AnalyticsAdmin';

function App() {
  return (
    <Routes>
      <Route path="/" element={<MainLayout />}>
        <Route index element={<Home />} />
        <Route path="search" element={<SearchResults />} />
        <Route path="admin/crawler" element={<CrawlerAdmin />} />
        <Route path="admin/analytics" element={<AnalyticsAdmin />} />

      </Route>
    </Routes>
  );
}

export default App;