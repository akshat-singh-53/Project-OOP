package com.itsm.domain.user;

import com.itsm.domain.catalog.Category;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@DiscriminatorValue("TECHNICIAN")
public class Technician extends User {

    public static final int DEFAULT_MAX_WORKLOAD = 5;

    /** Aggregation: categories exist independently of the technician who is skilled in them. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "technician_skills",
            joinColumns = @JoinColumn(name = "technician_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private Set<Category> skills = new HashSet<>();

    @Enumerated(EnumType.STRING)
    private TechnicianLevel level = TechnicianLevel.L1;

    private int experienceYears;

    private int maxWorkload = DEFAULT_MAX_WORKLOAD;

    private boolean available = true;

    protected Technician() {
    }

    public Technician(String username, String passwordHash, String fullName, String email, String phone,
                      TechnicianLevel level, int experienceYears, int maxWorkload) {
        super(username, passwordHash, fullName, email, phone);
        this.level = level;
        this.experienceYears = Math.max(0, experienceYears);
        this.maxWorkload = maxWorkload > 0 ? maxWorkload : DEFAULT_MAX_WORKLOAD;
    }

    @Override
    public Role getRole() {
        return Role.TECHNICIAN;
    }

    public boolean hasSkill(Category category) {
        return category != null && skills.stream().anyMatch(s -> s.getId().equals(category.getId()));
    }

    public Set<Category> getSkills() {
        return Collections.unmodifiableSet(skills);
    }

    public void replaceSkills(Set<Category> newSkills) {
        this.skills.clear();
        this.skills.addAll(newSkills);
    }

    public TechnicianLevel getLevel() {
        return level;
    }

    public void setLevel(TechnicianLevel level) {
        this.level = level;
    }

    public boolean isTeamLead() {
        return level == TechnicianLevel.TEAM_LEAD;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public void setExperienceYears(int experienceYears) {
        this.experienceYears = Math.max(0, experienceYears);
    }

    public int getMaxWorkload() {
        return maxWorkload;
    }

    public void setMaxWorkload(int maxWorkload) {
        if (maxWorkload < 1) {
            throw new IllegalArgumentException("Max workload must be at least 1");
        }
        this.maxWorkload = maxWorkload;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
