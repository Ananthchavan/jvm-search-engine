package com.jvmservicengine.search.crawler.robots;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class RobotsTxtServiceTest {

    private RobotsTxtService robotsTxtService;

    @BeforeEach
    void setUp() {
        robotsTxtService = new RobotsTxtService(new com.jvmservicengine.search.crawler.validation.UrlSafetyValidator(java.util.List.of(80, 443, -1)));
    }

    @Test
    void testCaseSensitivePaths() {
        String content = """
                User-agent: *
                Disallow: /Admin/
                """;
        var directives = robotsTxtService.parseRobotsTxt(content);
        // /Admin/ is blocked, but /admin/ is allowed
        assertTrue(directives.rules.stream().anyMatch(r -> r.matches("/Admin/dashboard")));
        assertFalse(directives.rules.stream().anyMatch(r -> r.matches("/admin/dashboard")));
    }

    @Test
    void testConsecutiveUserAgentsSharingRules() {
        String content = """
                User-agent: googlebot
                User-agent: jvmsearchenginebot
                Disallow: /private/
                Crawl-delay: 5

                User-agent: *
                Disallow: /
                """;
        var directives = robotsTxtService.parseRobotsTxt(content);
        // Our bot should match the first group and get /private/ (not /)
        assertEquals(5000L, directives.crawlDelayMs);
        assertEquals(1, directives.rules.size());
        assertEquals("/private/", directives.rules.get(0).patternString);
    }

    @Test
    void testWildcardAndEndAnchorAndLongestRuleWins() {
        String content = """
                User-agent: *
                Disallow: /fish*
                Allow: /fish.html$
                """;
        var directives = robotsTxtService.parseRobotsTxt(content);
        // /fish/index.html -> matches /fish* (length 6) -> DISALLOW
        // /fish.html -> matches /fish* (len 6) and /fish.html$ (len 11) -> ALLOW wins by length
        var rule1 = directives.rules.get(0); // /fish*
        var rule2 = directives.rules.get(1); // /fish.html$

        assertTrue(rule1.matches("/fish/index.html"));
        assertFalse(rule2.matches("/fish/index.html"));

        assertTrue(rule1.matches("/fish.html"));
        assertTrue(rule2.matches("/fish.html"));
    }

    @Test
    void testEmptyDisallowMeansAllowAll() {
        String content = """
                User-agent: *
                Disallow:
                """;
        var directives = robotsTxtService.parseRobotsTxt(content);
        assertTrue(directives.rules.isEmpty());
        assertFalse(directives.disallowAll);
    }
}

