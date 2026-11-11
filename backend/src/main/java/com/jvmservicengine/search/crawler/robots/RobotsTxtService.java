package com.jvmservicengine.search.crawler.robots;

import com.jvmservicengine.search.crawler.jsoup.JsoupWebClient;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;


@Slf4j
@Service
public class RobotsTxtService {

    private static final String BOT_NAME = "jvmsearchenginebot";
    private static final int TIMEOUT_MS = 5000;

    // Cache TTLs
    private static final long TTL_SUCCESS_MS = 24L * 60 * 60 * 1000; // 24 hours
    private static final long TTL_4XX_MS = 24L * 60 * 60 * 1000;     // 24 hours (allow all)
    private static final long TTL_ERROR_MS = 15L * 60 * 1000;        // 15 minutes (temporary disallow all)

    private final ConcurrentHashMap<String, CachedRobots> cache = new ConcurrentHashMap<>();

    enum RuleType {
        ALLOW,
        DISALLOW
    }

    static class PathRule {
        final RuleType type;
        final String patternString;
        final int patternLength;
        final Pattern compiledPattern;

        PathRule(RuleType type, String patternString) {
            this.type = type;
            this.patternString = patternString;
            this.patternLength = patternString.length();
            this.compiledPattern = compilePattern(patternString);
        }

        boolean matches(String path) {
            return compiledPattern.matcher(path).find();
        }

        private static Pattern compilePattern(String raw) {
            StringBuilder sb = new StringBuilder("^");
            for (int i = 0; i < raw.length(); i++) {
                char c = raw.charAt(i);
                if (c == '*') {
                    sb.append(".*");
                } else if (c == '$' && i == raw.length() - 1) {
                    sb.append("$");
                } else {
                    sb.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return Pattern.compile(sb.toString());
        }
    }

    static class RobotsDirectives {
        final List<PathRule> rules;
        final Long crawlDelayMs;
        final boolean allowAll;
        final boolean disallowAll;

        RobotsDirectives(List<PathRule> rules, Long crawlDelayMs, boolean allowAll, boolean disallowAll) {
            this.rules = rules;
            this.crawlDelayMs = crawlDelayMs;
            this.allowAll = allowAll;
            this.disallowAll = disallowAll;
        }

        static RobotsDirectives allowAll() {
            return new RobotsDirectives(Collections.emptyList(), null, true, false);
        }

        static RobotsDirectives disallowAll() {
            return new RobotsDirectives(Collections.emptyList(), null, false, true);
        }
    }

    private record CachedRobots(RobotsDirectives directives, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Checks if targetUrl is allowed according to robots.txt rules.
     */
    public boolean isAllowed(String targetUrl) {
        try {
            URL url = URI.create(targetUrl).toURL();
            String originKey = getOriginKey(url);
            String targetPath = getFullPathWithQuery(url);

            RobotsDirectives directives = getDirectives(url, originKey);

            if (directives.allowAll) {
                return true;
            }
            if (directives.disallowAll) {
                return false;
            }

            // Find matching rules. RFC 9309:
            // The most specific (longest pattern match) rule takes precedence.
            // If length is tied, Allow takes precedence over Disallow.
            PathRule bestRule = null;
            for (PathRule rule : directives.rules) {
                if (rule.matches(targetPath)) {
                    if (bestRule == null) {
                        bestRule = rule;
                    } else if (rule.patternLength > bestRule.patternLength) {
                        bestRule = rule;
                    } else if (rule.patternLength == bestRule.patternLength) {
                        if (rule.type == RuleType.ALLOW && bestRule.type == RuleType.DISALLOW) {
                            bestRule = rule;
                        }
                    }
                }
            }

            if (bestRule == null) {
                // Default is allow if no rules match
                return true;
            }

            return bestRule.type == RuleType.ALLOW;
        } catch (Exception e) {
            log.warn("Could not parse or check robots.txt for URL: {} - {}", targetUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Returns the Crawl-delay in milliseconds for the domain/origin, if specified.
     */
    public OptionalLong getCrawlDelayMs(String targetUrl) {
        try {
            URL url = URI.create(targetUrl).toURL();
            String originKey = getOriginKey(url);
            RobotsDirectives directives = getDirectives(url, originKey);
            if (directives.crawlDelayMs != null) {
                return OptionalLong.of(directives.crawlDelayMs);
            }
        } catch (Exception e) {
            log.warn("Could not retrieve crawl delay for URL: {}", targetUrl);
        }
        return OptionalLong.empty();
    }

    private String getOriginKey(URL url) {
        String scheme = url.getProtocol().toLowerCase();
        String host = url.getHost().toLowerCase();
        int port = url.getPort();
        return scheme + "://" + host + (port != -1 ? ":" + port : "");
    }

    private String getFullPathWithQuery(URL url) {
        String path = url.getPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (url.getQuery() != null && !url.getQuery().isEmpty()) {
            path = path + "?" + url.getQuery();
        }
        return path;
    }

    private RobotsDirectives getDirectives(URL url, String originKey) {
        CachedRobots cached = cache.get(originKey);
        if (cached != null && !cached.isExpired()) {
            return cached.directives();
        }

        // Network fetch outside ConcurrentHashMap computeIfAbsent
        CachedRobots fresh = fetchRobotsTxt(url, originKey);
        cache.put(originKey, fresh);
        return fresh.directives();
    }

    private CachedRobots fetchRobotsTxt(URL targetUrl, String originKey) {
        String robotsUrl = originKey + "/robots.txt";
        long now = System.currentTimeMillis();

        try {
            log.debug("Fetching robots.txt from: {}", robotsUrl);
            Connection.Response response = Jsoup.connect(robotsUrl)
                    .userAgent(JsoupWebClient.USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .ignoreContentType(true)
                    .followRedirects(true)
                    .execute();

            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                String body = response.body();
                RobotsDirectives parsed = parseRobotsTxt(body);
                log.info("Successfully fetched and parsed robots.txt for {}", originKey);
                return new CachedRobots(parsed, now + TTL_SUCCESS_MS);
            } else if (statusCode >= 400 && statusCode < 500) {
                log.info("robots.txt returned 4xx status ({}) for {}. Allowing all.", statusCode, originKey);
                return new CachedRobots(RobotsDirectives.allowAll(), now + TTL_4XX_MS);
            } else {
                // 5xx status codes
                log.warn("robots.txt returned server error ({}) for {}. Temporarily disallowing all.", statusCode, originKey);
                return new CachedRobots(RobotsDirectives.disallowAll(), now + TTL_ERROR_MS);
            }
        } catch (HttpStatusException e) {
            int code = e.getStatusCode();
            if (code >= 400 && code < 500) {
                log.info("robots.txt returned HTTP {} for {}. Allowing all.", code, originKey);
                return new CachedRobots(RobotsDirectives.allowAll(), now + TTL_4XX_MS);
            } else {
                log.warn("robots.txt returned HTTP {} for {}. Temporarily disallowing all.", code, originKey);
                return new CachedRobots(RobotsDirectives.disallowAll(), now + TTL_ERROR_MS);
            }
        } catch (Exception e) {
            log.warn("Network/timeout error fetching robots.txt for {}: {}. Temporarily disallowing all.",
                    originKey, e.getMessage());
            return new CachedRobots(RobotsDirectives.disallowAll(), now + TTL_ERROR_MS);
        }
    }

    private static class RawGroup {
        final List<String> userAgents = new ArrayList<>();
        final List<PathRule> rules = new ArrayList<>();
        Long crawlDelayMs = null;
    }

    protected RobotsDirectives parseRobotsTxt(String content) {
        if (content == null || content.isBlank()) {
            return RobotsDirectives.allowAll();
        }

        List<RawGroup> groups = new ArrayList<>();
        RawGroup currentGroup = null;
        boolean inRulesSection = false;

        String[] lines = content.split("\r?\n");
        for (String rawLine : lines) {
            // Strip comments
            int commentIndex = rawLine.indexOf('#');
            String line = commentIndex >= 0 ? rawLine.substring(0, commentIndex) : rawLine;
            line = line.trim();

            if (line.isEmpty()) {
                continue;
            }

            int colonIndex = line.indexOf(':');
            if (colonIndex < 0) {
                continue;
            }

            String directive = line.substring(0, colonIndex).trim().toLowerCase();
            String value = line.substring(colonIndex + 1).trim();

            if ("user-agent".equals(directive)) {
                String ua = value.toLowerCase();
                // If we were reading rules and now encounter a user-agent, start a new group
                if (inRulesSection || currentGroup == null) {
                    currentGroup = new RawGroup();
                    groups.add(currentGroup);
                    inRulesSection = false;
                }
                currentGroup.userAgents.add(ua);
            } else if (currentGroup != null) {
                inRulesSection = true;
                if ("disallow".equals(directive)) {
                    if (!value.isEmpty()) {
                        currentGroup.rules.add(new PathRule(RuleType.DISALLOW, value));
                    }
                    // Note: An empty "Disallow:" means allow all, so we intentionally don't add a disallow rule.
                } else if ("allow".equals(directive)) {
                    if (!value.isEmpty()) {
                        currentGroup.rules.add(new PathRule(RuleType.ALLOW, value));
                    }
                } else if ("crawl-delay".equals(directive)) {
                    try {
                        double seconds = Double.parseDouble(value);
                        currentGroup.crawlDelayMs = (long) (seconds * 1000);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        // Group selection according to RFC 9309:
        // If a group names our bot (jvmsearchenginebot), use ONLY that group.
        // Otherwise use the "*" group. Don't merge them.
        RawGroup specificGroup = null;
        RawGroup wildcardGroup = null;

        for (RawGroup g : groups) {
            for (String ua : g.userAgents) {
                if (BOT_NAME.equals(ua)) {
                    specificGroup = g;
                    break;
                }
                if ("*".equals(ua)) {
                    wildcardGroup = g;
                }
            }
            if (specificGroup != null) {
                break;
            }
        }

        RawGroup selectedGroup = specificGroup != null ? specificGroup : wildcardGroup;
        if (selectedGroup == null) {
            return RobotsDirectives.allowAll();
        }

        return new RobotsDirectives(
                selectedGroup.rules,
                selectedGroup.crawlDelayMs,
                false,
                false
        );
    }
}
