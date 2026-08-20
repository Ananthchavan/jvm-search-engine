const Footer = () => {
    return (
        <footer className="bg-[#f2f2f2] text-[#70757a] text-sm font-sans">
            {/* Top Location Bar */}
            <div className="px-8 py-3 border-b border-[#dadce0]">
                India
            </div>

            {/* Bottom Links Bar */}
            <div className="flex flex-col sm:flex-row justify-between px-8 py-3">
                <div className="flex flex-wrap gap-x-6 gap-y-2 mb-2 sm:mb-0">
                    <span className="hover:underline cursor-pointer">About</span>
                    <span className="hover:underline cursor-pointer">Advertising</span>
                    <span className="hover:underline cursor-pointer">Business</span>
                    <span className="hover:underline cursor-pointer">How Search works</span>
                </div>
                <div className="flex flex-wrap gap-x-6 gap-y-2">
                    <span className="hover:underline cursor-pointer">Privacy</span>
                    <span className="hover:underline cursor-pointer">Terms</span>
                    <span className="hover:underline cursor-pointer">Settings</span>
                </div>
            </div>
        </footer>
    );
};

export default Footer;