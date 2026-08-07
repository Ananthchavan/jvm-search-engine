package com.jvmservicengine.search.service;


import com.jvmservicengine.search.storage.entity.Page;
import com.jvmservicengine.search.storage.repository.PageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PageService {

    private final PageRepository pageRepository;

    public Page createPage(Page page){
        return pageRepository.save(page);
    }

    public List<Page> getAllPages() {
        return pageRepository.findAll();
    }

    public Optional<Page> getPageById(Long id) {
        return pageRepository.findById(id);
    }

    public Page updatePage(Long id, Page pageDetails){
        if(pageRepository.existsById(id)){
            pageDetails.setId(id);
            return pageRepository.save(pageDetails);
        } else{
            throw new RuntimeException("page not found with id: "+ id);
        }
    }

    public void deletePage(Long id){
        pageRepository.deleteById(id);
    }
}
