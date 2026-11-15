package com.itsm.domain.user;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("EMPLOYEE")
public class Employee extends User {

    private String department;

    protected Employee() {
    }

    public Employee(String username, String passwordHash, String fullName, String email, String phone,
                    String department) {
        super(username, passwordHash, fullName, email, phone);
        this.department = department;
    }

    @Override
    public Role getRole() {
        return Role.EMPLOYEE;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
