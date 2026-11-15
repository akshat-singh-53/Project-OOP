package com.itsm.domain.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_settings")
public class SystemSetting {

    @Id
    @Column(length = 60)
    private String settingKey;

    @Column(nullable = false, length = 200)
    private String settingValue;

    protected SystemSetting() {
    }

    public SystemSetting(String key, String value) {
        this.settingKey = key;
        this.settingValue = value;
    }

    public String getKey() {
        return settingKey;
    }

    public String getValue() {
        return settingValue;
    }

    public void setValue(String value) {
        this.settingValue = value;
    }
}
