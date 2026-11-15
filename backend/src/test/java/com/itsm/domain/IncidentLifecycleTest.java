package com.itsm.domain;

import com.itsm.TestData;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentAction;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.TechnicianLevel;
import com.itsm.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentLifecycleTest {

    private Category network;
    private Technician tech;
    private Incident incident;

    @BeforeEach
    void setUp() {
        network = TestData.category("NETWORK");
        ItService vpn = TestData.service("VPN", 3, network);
        tech = TestData.technician("Ravi", TechnicianLevel.L2, 4, 5, network);
        incident = TestData.incident("VPN down", "VPN disconnects for everybody", vpn, Impact.HIGH, Urgency.HIGH);
    }

    @Test
    @DisplayName("TC05 Full lifecycle NEW -> ... -> CLOSED succeeds and records timestamps")
    void happyPath() {
        incident.classify(network, false);
        incident.assignTo(tech, "manual");
        incident.acknowledge();
        incident.startWork();
        incident.resolve("Restarted VPN concentrator", "Memory leak");
        incident.verify();
        incident.close("Confirmed by user");

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.getAcknowledgedAt()).isNotNull();
        assertThat(incident.getResolvedAt()).isNotNull();
        assertThat(incident.getClosedAt()).isNotNull();
        assertThat(incident.getSlaStatus()).isEqualTo(SlaStatus.MET);
    }

    @Test
    @DisplayName("TC06 NEW -> CLOSED is rejected (invalid state transition)")
    void cannotCloseNewIncident() {
        assertThatThrownBy(() -> incident.close("skip everything"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("NEW");
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.NEW);
    }

    @Test
    @DisplayName("TC07 Technician cannot resolve before acknowledging and starting work")
    void cannotResolveFromAssigned() {
        incident.classify(network, false);
        incident.assignTo(tech, "manual");
        assertThatThrownBy(() -> incident.resolve("fixed", null)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TC08 Pending-info round trip and reopen after resolution")
    void pendingInfoAndReopen() {
        incident.classify(network, false);
        incident.assignTo(tech, "manual");
        incident.acknowledge();
        incident.startWork();
        incident.requestInfo();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.PENDING_INFO);
        assertThatThrownBy(() -> incident.resolve("x", null)).isInstanceOf(InvalidStateTransitionException.class);
        incident.provideInfo();
        incident.resolve("Updated client", null);
        incident.reopen();
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.IN_PROGRESS);
        assertThat(incident.getReopenCount()).isEqualTo(1);
        assertThat(incident.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("TC09 Closed incidents allow no further actions")
    void closedIsTerminal() {
        Incident original = TestData.incident("orig", "original problem", incident.getService(), Impact.LOW, Urgency.LOW);
        incident.markDuplicateOf(original);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(incident.allowedActions()).isEmpty();
        assertThatThrownBy(() -> incident.reopen()).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> incident.markDuplicateOf(original)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("TC10 Each state exposes only its legal actions")
    void allowedActionsPerState() {
        assertThat(incident.allowedActions()).containsExactlyInAnyOrder(IncidentAction.CLASSIFY, IncidentAction.MARK_DUPLICATE);
        incident.classify(network, true);
        assertThat(incident.allowedActions()).contains(IncidentAction.ASSIGN).doesNotContain(IncidentAction.RESOLVE);
    }

    @Test
    @DisplayName("TC11 Feedback rating outside 1..5 is rejected")
    void feedbackValidation() {
        assertThatThrownBy(() -> incident.recordFeedback(6, "too good")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> incident.recordFeedback(0, "bad")).isInstanceOf(IllegalArgumentException.class);
    }
}
