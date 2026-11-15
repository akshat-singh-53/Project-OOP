package com.itsm.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("ADMIN")
public class Administrator extends User {

    /** IT Managers sit at the top of the escalation chain. */
    private boolean itManager;

    protected Administrator() {
    }

    public Administrator(String username, String passwordHash, String fullName, String email, String phone,
                         boolean itManager) {
        super(username, passwordHash, fullName, email, phone);
        this.itManager = itManager;
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }

    public boolean isItManager() {
        return itManager;
    }

    public void setItManager(boolean itManager) {
        this.itManager = itManager;
    }
}
