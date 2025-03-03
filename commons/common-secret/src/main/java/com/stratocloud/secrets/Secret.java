package com.stratocloud.secrets;

import com.stratocloud.jpa.entities.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Secret extends Auditable {
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    public Secret(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }
}
