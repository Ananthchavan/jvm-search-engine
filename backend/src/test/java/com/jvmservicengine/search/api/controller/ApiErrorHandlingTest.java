package com.jvmservicengine.search.api.controller;

import com.jvmservicengine.search.common.exception.GlobalExceptionHandler;
import com.jvmservicengine.search.crawler.service.CrawlerService;
import com.jvmservicengine.search.crawler.validation.SafeUrlConstraintValidator;
import com.jvmservicengine.search.crawler.validation.UrlSafetyValidator;
import com.jvmservicengine.search.indexing.service.IndexingService;
import com.jvmservicengine.search.searches.dto.SearchResponse;
import com.jvmservicengine.search.searches.service.SearchService;
import com.jvmservicengine.search.storage.repository.CrawlerQueueRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApiErrorHandlingTest {

    private MockMvc mockMvc;

    @Mock
    private SearchService searchService;

    @Mock
    private CrawlerService crawlerService;

    @Mock
    private IndexingService indexingService;

    @Mock
    private CrawlerQueueRepository crawlerQueueRepository;

    @BeforeEach
    void setUp() {
        UrlSafetyValidator safetyValidator = new UrlSafetyValidator(List.of(80, 443, -1));

        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        beanFactory.registerSingleton("urlSafetyValidator", safetyValidator);
        beanFactory.registerSingleton("safeUrlConstraintValidator", new SafeUrlConstraintValidator(safetyValidator));

        LocalValidatorFactoryBean validatorFactory = new LocalValidatorFactoryBean();
        validatorFactory.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beanFactory));
        validatorFactory.afterPropertiesSet();

        MethodValidationInterceptor interceptor = new MethodValidationInterceptor((jakarta.validation.Validator) validatorFactory.getValidator());
        ProxyFactory proxyFactory = new ProxyFactory(new SearchController(searchService));
        proxyFactory.addAdvice(interceptor);
        SearchController searchController = (SearchController) proxyFactory.getProxy();

        CrawlController crawlController = new CrawlController(crawlerService, indexingService, crawlerQueueRepository);

        mockMvc = MockMvcBuilders
                .standaloneSetup(searchController, crawlController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validatorFactory)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/search with missing q parameter should return 400")
    void searchMissingQReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("GET /api/v1/search with invalid page parameter (< 1) should return 400")
    void searchBadPageReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "spring")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/v1/crawl with blank seedUrl should return 400")
    void crawlBlankSeedUrlReturns400() throws Exception {
        String badJson = "{\"seedUrl\":\"   \"}";
        mockMvc.perform(post("/api/v1/crawl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Request to an unknown URL should return 404")
    void unknownUrlReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/this-path-does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Calling an endpoint with unsupported HTTP method should return 405")
    void wrongMethodReturns405() throws Exception {
        mockMvc.perform(post("/api/v1/search"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    @DisplayName("POST /api/v1/crawl with valid seedUrl returns 202 JSON accepted")
    void crawlValidSeedUrlReturns202() throws Exception {
        String validJson = "{\"seedUrl\":\"https://example.com\"}";
        mockMvc.perform(post("/api/v1/crawl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.seedUrl").value("https://example.com"));
    }
}
