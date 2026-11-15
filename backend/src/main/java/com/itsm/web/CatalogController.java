package com.itsm.web;

import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.security.CurrentUser;
import com.itsm.security.RequiresRole;
import com.itsm.service.CatalogService;
import com.itsm.web.dto.CatalogDtos.CategoryDto;
import com.itsm.web.dto.CatalogDtos.CategoryRequest;
import com.itsm.web.dto.CatalogDtos.ServiceDto;
import com.itsm.web.dto.CatalogDtos.ServiceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryDto> categories(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return catalog.categories(activeOnly).stream().map(CategoryDto::from).toList();
    }

    @PostMapping("/categories")
    @RequiresRole(Role.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody CategoryRequest r, @CurrentUser User user) {
        return CategoryDto.from(catalog.createCategory(r, user.getUsername()));
    }

    @PutMapping("/categories/{id}")
    @RequiresRole(Role.ADMIN)
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r,
                                      @CurrentUser User user) {
        return CategoryDto.from(catalog.updateCategory(id, r, user.getUsername()));
    }

    @GetMapping("/services")
    public List<ServiceDto> services(@RequestParam(defaultValue = "true") boolean activeOnly) {
        return catalog.services(activeOnly).stream().map(ServiceDto::from).toList();
    }

    @PostMapping("/services")
    @RequiresRole(Role.ADMIN)
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceDto createService(@Valid @RequestBody ServiceRequest r, @CurrentUser User user) {
        return ServiceDto.from(catalog.createService(r, user.getUsername()));
    }

    @PutMapping("/services/{id}")
    @RequiresRole(Role.ADMIN)
    public ServiceDto updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest r,
                                    @CurrentUser User user) {
        return ServiceDto.from(catalog.updateService(id, r, user.getUsername()));
    }
}
