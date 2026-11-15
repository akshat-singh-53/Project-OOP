package com.itsm.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Incident categories are data, not an enum, so administrators can introduce new
 * categories at runtime without touching unrelated code.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    /** Comma separated keywords used by the automatic categorisation feature. */
    @Column(length = 1000)
    private String keywords;

    private boolean active = true;

    protected Category() {
    }

    public Category(String code, String name, String description, String keywords) {
        this.code = normaliseCode(code);
        this.name = name;
        this.description = description;
        this.keywords = keywords;
    }

    private static String normaliseCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    public List<String> keywordList() {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(k -> !k.isEmpty())
                .map(k -> k.toLowerCase(Locale.ROOT))
                .toList();
    }

    public void update(String name, String description, String keywords, boolean active) {
        this.name = name;
        this.description = description;
        this.keywords = keywords;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getKeywords() {
        return keywords;
    }

    public boolean isActive() {
        return active;
    }
}
