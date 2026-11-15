package com.itsm.intelligence;

import com.itsm.TestData;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ItService;
import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.Urgency;
import com.itsm.domain.user.TechnicianLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntelligenceTest {

    private final Category auth = TestData.category("AUTHENTICATION", "login, password, sso, otp, locked, account");
    private final Category software = TestData.category("SOFTWARE", "software, outlook, crash, teams, audio, install");
    private final Category network = TestData.category("NETWORK", "network, wifi, vpn, drop, disconnect, slow");
    private final Category application = TestData.category("APPLICATION", "payment, portal, transaction, checkout");
    private final List<Category> all = List.of(auth, software, network, application);

    @Test
    @DisplayName("TC17 Text analyzer folds synonyms and stems words")
    void textNormalisation() {
        assertThat(TextAnalyzer.tokens("Payments are failing, service down"))
                .contains("payment", "fail", "unavailable");
        assertThat(TextAnalyzer.tokens("crashes dropping logged")).contains("fail", "drop", "log");
    }

    @Test
    @DisplayName("TC18 Keyword classifier: locked account -> Authentication")
    void classifyAuthentication() {
        var result = new KeywordCategoryClassifier().classify("I forgot my password and my account is locked", all);
        assertThat(result.get(0).code()).isEqualTo("AUTHENTICATION");
    }

    @Test
    @DisplayName("TC19 Keyword classifier: Outlook crash -> Software; Teams audio drop is not Network-only")
    void classifySoftware() {
        var c = new KeywordCategoryClassifier();
        assertThat(c.classify("Outlook crashes when opening attachments", all).get(0).code()).isEqualTo("SOFTWARE");
        assertThat(c.classify("Teams calls dropping audio", all).get(0).code()).isEqualTo("SOFTWARE");
    }

    @Test
    @DisplayName("TC20 Duplicate detector links the three payment reports and ignores unrelated ones")
    void duplicates() {
        ItService payments = TestData.service("Payment Gateway", 5, application);
        ItService hardware = TestData.service("Office Hardware", 2, software);
        Incident outage = TestData.incident("Payment application unavailable",
                "The payment application is down and users are unable to complete transactions.", payments,
                Impact.CRITICAL, Urgency.CRITICAL);
        Incident printer = TestData.incident("Printer jam", "Printer on floor 2 is jammed", hardware,
                Impact.LOW, Urgency.LOW);

        var detector = new DuplicateDetector();
        var matches = detector.findCandidates("Payment transaction failing. Payment transactions are failing at checkout.",
                payments.getId(), null, List.of(outage, printer));
        assertThat(matches).extracting(DuplicateCandidate::incidentCode).containsExactly(outage.getCode());
        assertThat(matches.get(0).sameService()).isTrue();

        assertThat(detector.findCandidates("Laptop keyboard not working", hardware.getId(), null, List.of(outage)))
                .isEmpty();
    }

    @Test
    @DisplayName("TC21 Suggested resolution comes from the most similar solved incident")
    void resolutionSuggestion() {
        ItService vpnService = TestData.service("VPN", 3, network);
        Incident solved = TestData.incident("VPN keeps disconnecting", "VPN disconnects every few minutes from home",
                vpnService, Impact.MEDIUM, Urgency.HIGH);
        solved.classify(network, false);
        solved.assignTo(TestData.technician("T", TechnicianLevel.L1, 1, 5, network), "manual");
        solved.acknowledge();
        solved.startWork();
        solved.resolve("Updated VPN client to v5.2", "Old client");

        Incident current = TestData.incident("VPN connection drops", "My VPN disconnects frequently when working from home",
                vpnService, Impact.MEDIUM, Urgency.HIGH);
        current.classify(network, false);

        var suggestions = new ResolutionSuggester().suggest(current, List.of(solved));
        assertThat(suggestions).hasSize(1);
        assertThat(suggestions.get(0).resolution()).contains("v5.2");
    }

    @Test
    @DisplayName("TC22 SLA risk: overdue incident scores 100, fresh low-priority incident scores low")
    void slaRisk() {
        ItService svc = TestData.service("Svc", 3, network);
        Incident fresh = TestData.incident("Minor", "minor problem", svc, Impact.LOW, Urgency.LOW);
        fresh.applyPriority(Priority.P4, 2, "");
        fresh.applySlaTargets(240, 1440, 80);

        var predictor = new SlaRiskPredictor();
        LocalDateTime now = fresh.getCreatedAt().plusMinutes(5);
        assertThat(predictor.assess(fresh, 120, 1, now).riskLevel()).isEqualTo("LOW");
        assertThat(predictor.assess(fresh, 120, 1, fresh.getResolutionDueAt().plusMinutes(1)).riskScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("TC23 Correlation flags three open incidents on one service as a suspected major incident")
    void correlation() {
        ItService payments = TestData.service("Payment Gateway", 5, application);
        List<Incident> burst = List.of(
                TestData.incident("Payment down", "a", payments, Impact.HIGH, Urgency.HIGH),
                TestData.incident("Payment failing", "b", payments, Impact.HIGH, Urgency.HIGH),
                TestData.incident("Cannot pay", "c", payments, Impact.HIGH, Urgency.HIGH));
        var clusters = new CorrelationAnalyzer().analyse(burst);
        assertThat(clusters).anyMatch(c -> c.groupName().equals("Payment Gateway") && c.suspectedMajorIncident()
                && c.incidentCount() == 3);
    }
}
