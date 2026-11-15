package com.itsm.service;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.ServiceNotFoundException;
import com.itsm.exception.ValidationException;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.ItServiceRepository;
import com.itsm.web.dto.CatalogDtos.CategoryRequest;
import com.itsm.web.dto.CatalogDtos.ServiceRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CatalogService {

    private final CategoryRepository categories;
    private final ItServiceRepository services;
    private final AuditService audit;

    public CatalogService(CategoryRepository categories, ItServiceRepository services, AuditService audit) {
        this.categories = categories;
        this.services = services;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Category> categories(boolean activeOnly) {
        return activeOnly ? categories.findByActiveTrueOrderByNameAsc() : categories.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }

    @Transactional
    public Category createCategory(CategoryRequest r, String actor) {
        if (categories.existsByCodeIgnoreCase(r.code().trim().replace(' ', '_'))) {
            throw new ValidationException("Category code '" + r.code() + "' already exists");
        }
        Category c = categories.save(new Category(r.code(), r.name(), r.description(), r.keywords()));
        audit.recordAdminAction(actor, "CATEGORY_CREATED", "Created category " + c.getCode());
        return c;
    }

    @Transactional
    public Category updateCategory(Long id, CategoryRequest r, String actor) {
        Category c = category(id);
        c.update(r.name(), r.description(), r.keywords(), r.active() == null || r.active());
        audit.recordAdminAction(actor, "CATEGORY_UPDATED", "Updated category " + c.getCode());
        return c;
    }

    @Transactional(readOnly = true)
    public List<ItService> services(boolean activeOnly) {
        return activeOnly ? services.findByActiveTrueOrderByNameAsc() : services.findAll(Sort.by("name"));
    }

    @Transactional(readOnly = true)
    public ItService service(Long id) {
        return services.findById(id).orElseThrow(() -> new ServiceNotFoundException(id));
    }

    @Transactional
    public ItService createService(ServiceRequest r, String actor) {
        if (services.findByNameIgnoreCase(r.name().trim()).isPresent()) {
            throw new ValidationException("A service named '" + r.name() + "' already exists");
        }
        Category category = r.defaultCategoryId() == null ? null : category(r.defaultCategoryId());
        ItService s = services.save(new ItService(r.name().trim(), r.description(), r.businessCriticality(),
                category, r.ownerTeam()));
        audit.recordAdminAction(actor, "SERVICE_CREATED", "Created IT service " + s.getName());
        return s;
    }

    @Transactional
    public ItService updateService(Long id, ServiceRequest r, String actor) {
        ItService s = service(id);
        Category category = r.defaultCategoryId() == null ? null : category(r.defaultCategoryId());
        s.update(r.name().trim(), r.description(), r.businessCriticality(), category, r.ownerTeam(),
                r.active() == null || r.active());
        audit.recordAdminAction(actor, "SERVICE_UPDATED", "Updated IT service " + s.getName());
        return s;
    }
}
