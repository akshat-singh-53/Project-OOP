package com.itsm;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.Employee;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.TechnicianLevel;

import java.lang.reflect.Field;
import java.util.Set;

/** Builders for domain objects in unit tests (no database involved). */
public final class TestData {

    private static long nextId = 1;

    private TestData() {
    }

    public static <T> T withId(T entity, long id) {
        Class<?> type = entity.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("id");
                field.setAccessible(true);
                field.set(entity, id);
                return entity;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        throw new IllegalArgumentException("No id field on " + entity.getClass());
    }

    public static Category category(String code) {
        return withId(new Category(code, code.charAt(0) + code.substring(1).toLowerCase(), null, null), nextId++);
    }

    public static Category category(String code, String keywords) {
        return withId(new Category(code, code, null, keywords), nextId++);
    }

    public static ItService service(String name, int criticality, Category defaultCategory) {
        return withId(new ItService(name, null, criticality, defaultCategory, "Team"), nextId++);
    }

    public static Employee employee() {
        return withId(new Employee("emp" + nextId, "hash", "Employee", "e@x.com", "9999999999", "Finance"), nextId++);
    }

    public static Technician technician(String name, TechnicianLevel level, int years, int max, Category... skills) {
        Technician t = withId(new Technician(name.toLowerCase(), "hash", name, name + "@x.com", "9999999999", level,
                years, max), nextId++);
        t.replaceSkills(Set.of(skills));
        return t;
    }

    public static Incident incident(String title, String description, ItService service, Impact impact,
                                    Urgency urgency) {
        return withId(new Incident(title, description, employee(), service, impact, urgency, 1), nextId++);
    }
}
