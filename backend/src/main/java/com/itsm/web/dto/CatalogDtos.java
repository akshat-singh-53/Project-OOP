package com.itsm.web.dto;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.sla.SlaPolicy;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryRef(Long id, String code, String name) {
        public static CategoryRef from(Category c) {
            return c == null ? null : new CategoryRef(c.getId(), c.getCode(), c.getName());
        }
    }

    public record CategoryDto(Long id, String code, String name, String description, String keywords,
                              boolean active) {
        public static CategoryDto from(Category c) {
            return new CategoryDto(c.getId(), c.getCode(), c.getName(), c.getDescription(), c.getKeywords(),
                    c.isActive());
        }
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 40) @Pattern(regexp = "[A-Za-z_ ]+", message = "letters, spaces and _ only")
            String code,
            @NotBlank @Size(max = 80) String name,
            @Size(max = 255) String description,
            @Size(max = 1000) String keywords,
            Boolean active) {
    }

    public record ServiceDto(Long id, String name, String description, int businessCriticality,
                             CategoryRef defaultCategory, String ownerTeam, boolean active) {
        public static ServiceDto from(ItService s) {
            return new ServiceDto(s.getId(), s.getName(), s.getDescription(), s.getBusinessCriticality(),
                    CategoryRef.from(s.getDefaultCategory()), s.getOwnerTeam(), s.isActive());
        }
    }

    public record ServiceRequest(
            @NotBlank @Size(max = 80) String name,
            @Size(max = 255) String description,
            @Min(1) @Max(5) int businessCriticality,
            Long defaultCategoryId,
            @Size(max = 80) String ownerTeam,
            Boolean active) {
    }

    public record SlaPolicyDto(Long id, String name, Priority priority, Long serviceId, String serviceName,
                               int responseMinutes, int resolutionMinutes, int warningThresholdPercent,
                               boolean active) {
        public static SlaPolicyDto from(SlaPolicy p) {
            return new SlaPolicyDto(p.getId(), p.getName(), p.getPriority(),
                    p.getService() == null ? null : p.getService().getId(),
                    p.getService() == null ? "All services (default)" : p.getService().getName(),
                    p.getResponseMinutes(), p.getResolutionMinutes(), p.getWarningThresholdPercent(), p.isActive());
        }
    }

    public record SlaPolicyRequest(
            @NotBlank @Size(max = 80) String name,
            @NotNull Priority priority,
            Long serviceId,
            @Min(1) int responseMinutes,
            @Min(1) int resolutionMinutes,
            @Min(10) @Max(99) int warningThresholdPercent,
            Boolean active) {
    }
}
