package com.itsm.domain.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An IT service offered by the organisation, e.g. "Payment Gateway". */
@Entity
@Table(name = "it_services")
public class ItService {

    public static final int MIN_CRITICALITY = 1;
    public static final int MAX_CRITICALITY = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    @Column(length = 255)
    private String description;

    /** 1 = nice to have ... 5 = business critical. */
    private int businessCriticality;

    @ManyToOne(fetch = FetchType.EAGER)
    private Category defaultCategory;

    private String ownerTeam;

    private boolean active = true;

    protected ItService() {
    }

    public ItService(String name, String description, int businessCriticality, Category defaultCategory,
                     String ownerTeam) {
        this.name = name;
        this.description = description;
        setBusinessCriticality(businessCriticality);
        this.defaultCategory = defaultCategory;
        this.ownerTeam = ownerTeam;
    }

    public void update(String name, String description, int businessCriticality, Category defaultCategory,
                       String ownerTeam, boolean active) {
        this.name = name;
        this.description = description;
        setBusinessCriticality(businessCriticality);
        this.defaultCategory = defaultCategory;
        this.ownerTeam = ownerTeam;
        this.active = active;
    }

    private void setBusinessCriticality(int value) {
        if (value < MIN_CRITICALITY || value > MAX_CRITICALITY) {
            throw new IllegalArgumentException("Business criticality must be between 1 and 5");
        }
        this.businessCriticality = value;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getBusinessCriticality() {
        return businessCriticality;
    }

    public Category getDefaultCategory() {
        return defaultCategory;
    }

    public String getOwnerTeam() {
        return ownerTeam;
    }

    public boolean isActive() {
        return active;
    }
}
